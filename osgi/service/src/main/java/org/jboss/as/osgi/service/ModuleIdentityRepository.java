/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.osgi.service;

import static org.jboss.as.osgi.OSGiLogger.LOGGER;
import static org.jboss.as.osgi.OSGiMessages.MESSAGES;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import org.jboss.as.server.ServerEnvironment;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.jboss.osgi.repository.RepositoryResolutionException;
import org.jboss.osgi.repository.URLResourceBuilderFactory;
import org.jboss.osgi.repository.spi.AbstractRepository;
import org.jboss.osgi.resolver.XCapability;
import org.jboss.osgi.resolver.XResourceBuilder;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;

/**
 * An {@link org.jboss.osgi.repository.XRepository} that resolves artifacts from the local modules/bundles location
 *
 * @author thomas.diesler@jboss.com
 * @since 20-Jan-2012
 */
final class ModuleIdentityRepository extends AbstractRepository {

    private final File modulesDir;
    private final List<File> bundlesPath;

    ModuleIdentityRepository(ServerEnvironment serverEnvironment) {
        File bundlesDir = serverEnvironment.getBundlesDir();
        if (bundlesDir.isDirectory() == false)
            throw MESSAGES.illegalStateArtifactBaseLocation(bundlesDir);
        modulesDir = new File(bundlesDir.getParent() + File.separator + "modules");
        if (modulesDir.isDirectory() == false)
            throw MESSAGES.illegalStateArtifactBaseLocation(modulesDir);
        bundlesPath = LayeredBundlePathFactory.resolveLayeredBundlePath(serverEnvironment);
    }

    @Override
    public Collection<Capability> findProviders(Requirement req) {
        String namespace = req.getNamespace();
        List<Capability> result = new ArrayList<Capability>();
        if (MODULE_IDENTITY_NAMESPACE.equals(namespace)) {
            String moduleId = (String) req.getAttributes().get(MODULE_IDENTITY_NAMESPACE);
            ModuleIdentifier moduleIdentifier = ModuleIdentifier.fromString(moduleId);
            try {
                File contentFile = getRepositoryEntry(bundlesPath, moduleIdentifier);
                if (contentFile == null) {
                    // See if we can use the boot module loader and work out a path from it
                    contentFile = getRepositoryEntryFromModuleLoader(moduleIdentifier);
                }
                if (contentFile == null) {
                    // As a last gasp, try $JBOSS_HOME/modules. Following AS7-6344 this will no longer
                    // find any standard module shipped with the AS or by a layered distribution or
                    // add-on based upon it. It may, however, find user provided modules, since $JBOSS_HOME/modules
                    // is a valid root for user modules.
                    contentFile = getRepositoryEntry(modulesDir, moduleIdentifier);
                }
                if (contentFile != null) {
                    URL contentURL = contentFile.toURI().toURL();
                    XResourceBuilder builder = URLResourceBuilderFactory.create(contentURL, null, true);
                    XCapability cap = builder.addCapability(MODULE_IDENTITY_NAMESPACE, moduleId);
                    builder.getResource();
                    result.add(cap);
                }
            } catch (RepositoryResolutionException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new RepositoryResolutionException(ex);
            }
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Get file for the singe jar that corresponds to the given identifier
     */
    static File getRepositoryEntry(List<File> bundlesPath, ModuleIdentifier identifier) throws IOException {
        String identifierPath = getModuleIdAsPath(identifier);
        for (File bundlesDir : bundlesPath) {
            File contentFile = getRepositoryEntry(bundlesDir, identifierPath);
            if (contentFile != null) {
                return contentFile;
            }
        }
        return null;
    }

    /**
     * Get file for the singe jar that corresponds to the given identifier
     */
    private static File getRepositoryEntry(File rootDir, ModuleIdentifier identifier) throws IOException {

        String identifierPath = getModuleIdAsPath(identifier);
        return getRepositoryEntry(rootDir, identifierPath);
    }

    private static File getRepositoryEntry(File rootDir, String identifierPath) throws IOException {

        File entryDir = new File(rootDir + "/" + identifierPath);
        if (entryDir.isDirectory() == false) {
            LOGGER.tracef("Cannot obtain directory: %s", entryDir);
            return null;
        }

        String[] files = entryDir.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".jar");
            }
        });
        if (files.length == 0) {
            LOGGER.tracef("Cannot find jar in: %s", entryDir);
            return null;
        }
        if (files.length > 1) {
            LOGGER.tracef("Multiple jars in: %s", entryDir);
            return null;
        }

        File entryFile = new File(entryDir + "/" + files[0]);
        if (entryFile.exists() == false) {
            LOGGER.tracef("File does not exist: %s", entryFile);
            return null;
        }

        return entryFile;
    }

    private static File getRepositoryEntryFromModuleLoader(ModuleIdentifier moduleIdentifier) {

        // Attempt to load the module from the modules hierarchy
        String identifierPath = getModuleIdAsPath(moduleIdentifier);
        try {
            final ModuleLoader moduleLoader = Module.getBootModuleLoader();
            final Module module = moduleLoader.loadModule(moduleIdentifier);

            // Extract URLs from the module and look for one that includes 'identifierPath'.
            // To do this we have to provide a path to getResources that we expect any usable
            // module will have, so we use META-INF
            Enumeration<URL> manifestURLs = module.getClassLoader().getResources("META-INF");
            while (manifestURLs.hasMoreElements()) {
                URL manifestURL = manifestURLs.nextElement();

                String manifestString = manifestURL.toExternalForm();
                int idx = manifestString.indexOf(identifierPath);
                if (idx > -1) {
                    // A URL related to our desired module. See if we can get a File URI pointing
                    // to the module root from which this module was loaded
                    String parent = manifestString.substring(0, idx);
                    if (parent.startsWith("jar:")) {
                        parent = parent.substring(4);
                    }
                    try {
                        File file = new File(new URI(parent));
                        return getRepositoryEntry(file, moduleIdentifier);
                    } catch (Exception e) {
                        // probably URISyntaxException on the URI or IAE from new File(URI)
                        // In any case, resolution via this mechanism is not available for this module
                        break;
                    }

                } // else it's a META-INF in a jar in some other module our module depends on
            }
        } catch (ModuleLoadException ex) {
            // not available
        } catch (IOException ex) {
            // not available
        }

        return null;
    }

    private static String getModuleIdAsPath(ModuleIdentifier moduleIdentifier) {
        return moduleIdentifier.getName().replace('.', '/') + "/" + moduleIdentifier.getSlot();
    }
}

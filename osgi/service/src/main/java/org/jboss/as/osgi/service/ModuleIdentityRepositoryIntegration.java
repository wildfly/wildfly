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
import static org.jboss.osgi.resolver.XResource.MODULE_IDENTITY_NAMESPACE;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.osgi.metadata.OSGiMetaData;
import org.jboss.osgi.metadata.OSGiMetaDataBuilder;
import org.jboss.osgi.repository.RepositoryLogger;
import org.jboss.osgi.repository.RepositoryMessages;
import org.jboss.osgi.repository.RepositoryResolutionException;
import org.jboss.osgi.repository.URLResourceBuilderFactory;
import org.jboss.osgi.repository.spi.ModuleIdentityRepository;
import org.jboss.osgi.resolver.XResource;
import org.jboss.osgi.resolver.XResourceBuilder;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;

/**
 * An {@link org.jboss.osgi.repository.XRepository} that resolves artifacts from the local modules/bundles location
 *
 * @author thomas.diesler@jboss.com
 * @since 20-Jan-2012
 */
public final class ModuleIdentityRepositoryIntegration extends ModuleIdentityRepository {

    private final List<File> bundlesPath;
    private final File modulesDir;

    public ModuleIdentityRepositoryIntegration(ServerEnvironment serverEnvironment) {
        super(Module.getCallerModuleLoader());
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
        if (!MODULE_IDENTITY_NAMESPACE.equals(namespace)) {
            return Collections.emptyList();
        }

        String idspec = (String) req.getAttributes().get(MODULE_IDENTITY_NAMESPACE);
        if (idspec == null)
            throw RepositoryMessages.MESSAGES.cannotObtainRequiredAttribute(MODULE_IDENTITY_NAMESPACE);

        List<Capability> result = new ArrayList<Capability>();
        try {
            // Try to resolve the moduleId in the bundles hierarchy
            ModuleIdentifier moduleId = ModuleIdentifier.fromString(idspec);
            File contentFile = getRepositoryEntry(bundlesPath, moduleId);
            if (contentFile != null) {
                XResourceBuilder<XResource> builder;
                URL contentURL = contentFile.toURI().toURL();
                builder = URLResourceBuilderFactory.create(contentURL, null);
                builder.addCapability(MODULE_IDENTITY_NAMESPACE, idspec);
                XResource resource = builder.getResource();
                try {
                    resource = getTargetResource(resource);
                    result.add(resource.getIdentityCapability());
                } catch (Exception ex) {
                    RepositoryLogger.LOGGER.errorCannotCreateResource(ex, idspec);
                }
            } else {
                Collection<Capability> providers = super.findProviders(req);
                result.addAll(providers);
            }
        } catch (RepositoryResolutionException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RepositoryResolutionException(ex);
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Get file for the singe jar that corresponds to the given identifier
     */
    public File getRepositoryEntry(List<File> basePaths, ModuleIdentifier moduleId) throws IOException {
        String identifierPath = getModuleIdAsPath(moduleId);
        for (File basePath : basePaths) {
            File contentFile = getSingleJarEntry(basePath, identifierPath);
            if (contentFile != null) {
                return contentFile;
            }
        }
        return null;
    }

    @Override
    public OSGiMetaData getOSGiMetaData(XResource resource) throws IOException {

        OSGiMetaData result = null;

        // Check for a jbosgi-xservice.properties in the root of a module directory under $JBOSS_HOME/modules/system/layers/base
        ModuleIdentifier moduleId = getModuleIdentifier(resource);
        Path basePath = FileSystems.getDefault().getPath(modulesDir.getAbsolutePath(), "system", "layers", "base");
        String identifierPath = moduleId.getName().replace('.', File.separatorChar) + File.separator + moduleId.getSlot();
        File entryFile = FileSystems.getDefault().getPath(basePath.toString(), identifierPath, "jbosgi-xservice.properties").toFile();
        if (entryFile.exists()) {
            try {
                FileInputStream input = new FileInputStream(entryFile);
                try {
                    Properties props = new Properties();
                    props.load(input);
                    result = OSGiMetaDataBuilder.load(props);
                } finally {
                    input.close();
                }
            } catch (IOException ex) {
                LOGGER.debugf("Cannot load OSGi metadata from: %s", entryFile);
            }
        }

        // Use the original metadata from the manifest
        if (result == null) {
            result = getOSGiMetaDataFromManifest(resource);
        }

        // Generate the OSGiMetaData by scanning the module
        if (result == null) {
            Module module = loadModule(moduleId);
            result = getOSGiMetaDataFromModule(module);
        }

        return result;
    }

    private File getSingleJarEntry(File baseDir, String identifierPath) throws IOException {

        File entryDir = FileSystems.getDefault().getPath(baseDir.getAbsolutePath(), identifierPath).toFile();
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

    private String getModuleIdAsPath(ModuleIdentifier moduleIdentifier) {
        return moduleIdentifier.getName().replace('.', '/') + "/" + moduleIdentifier.getSlot();
    }
}

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
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jboss.as.server.ServerEnvironment;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.osgi.repository.RepositoryResolutionException;
import org.jboss.osgi.repository.URLResourceBuilderFactory;
import org.jboss.osgi.repository.spi.AbstractRepository;
import org.jboss.osgi.resolver.XResourceBuilder;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;

/**
 * An {@link ArtifactProviderPlugin} that resolves artifacts from the local modules/bundles location
 *
 * @author thomas.diesler@jboss.com
 * @since 20-Jan-2012
 */
final class ModuleIdentityRepository extends AbstractRepository {

    private final File modulesDir;
    private final File bundlesDir;

    ModuleIdentityRepository(ServerEnvironment serverEnvironment) {
        bundlesDir = serverEnvironment.getBundlesDir();
        if (bundlesDir.isDirectory() == false)
            throw MESSAGES.illegalStateArtifactBaseLocation(bundlesDir);
        modulesDir = new File(bundlesDir.getParent() + File.separator + "modules");
        if (modulesDir.isDirectory() == false)
            throw MESSAGES.illegalStateArtifactBaseLocation(modulesDir);
    }

    @Override
    public Collection<Capability> findProviders(Requirement req) {
        String namespace = req.getNamespace();
        List<Capability> result = new ArrayList<Capability>();
        if (MODULE_IDENTITY_NAMESPACE.equals(namespace)) {
            String moduleId = (String) req.getAttributes().get(MODULE_IDENTITY_NAMESPACE);
            ModuleIdentifier moduleIdentifier = ModuleIdentifier.fromString(moduleId);
            try {
                File contentFile = getRepositoryEntry(bundlesDir, moduleIdentifier);
                if (contentFile != null) {
                    URL contentURL = contentFile.toURI().toURL();
                    XResourceBuilder builder = URLResourceBuilderFactory.create(contentURL, null, true);
                    result.add(builder.addGenericCapability(MODULE_IDENTITY_NAMESPACE, moduleId));
                } else {
                    contentFile = getRepositoryEntry(modulesDir, moduleIdentifier);
                    if (contentFile != null) {
                        URL contentURL = contentFile.toURI().toURL();
                        XResourceBuilder builder = URLResourceBuilderFactory.create(contentURL, null, true);
                        result.add(builder.addGenericCapability(MODULE_IDENTITY_NAMESPACE, moduleId));
                    }
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
    static File getRepositoryEntry(File rootDir, ModuleIdentifier identifier) throws IOException {

        String identifierPath = identifier.getName().replace('.', '/') + "/" + identifier.getSlot();
        File entryDir = new File(rootDir + "/" + identifierPath);
        if (entryDir.isDirectory() == false) {
            LOGGER.tracef("Cannot obtain directory: %s", entryDir);
            return null;
        }

        String[] files = entryDir.list(new FilenameFilter() {
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
}
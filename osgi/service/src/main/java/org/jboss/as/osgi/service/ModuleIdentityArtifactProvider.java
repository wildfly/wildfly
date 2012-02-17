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

import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.ServerEnvironmentService;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.osgi.framework.Constants;
import org.jboss.osgi.framework.Services;
import org.jboss.osgi.repository.ArtifactProviderPlugin;
import org.jboss.osgi.repository.RepositoryResolutionException;
import org.jboss.osgi.resolver.v2.XResource;
import org.jboss.osgi.resolver.v2.XResourceBuilder;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.resource.Capability;
import org.osgi.framework.resource.Requirement;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import static org.jboss.as.osgi.OSGiLogger.ROOT_LOGGER;
import static org.jboss.as.osgi.service.FrameworkBootstrapService.SERVICE_BASE_NAME;
import static org.jboss.osgi.resolver.v2.XResourceConstants.MODULE_IDENTITY_NAMESPACE;

/**
 * An {@link ArtifactProviderPlugin} that resolves artifacts from the local modules/bundles location
 *
 * @author thomas.diesler@jboss.com
 * @since 20-Jan-2012
 */
final class ModuleIdentityArtifactProvider extends AbstractService<Void> implements ArtifactProviderPlugin {

    public static final ServiceName SERVICE_NAME = SERVICE_BASE_NAME.append("artifact.provider");

    private final InjectedValue<BundleContext> injectedSystemContext = new InjectedValue<BundleContext>();
    private final InjectedValue<ServerEnvironment> injectedEnvironment = new InjectedValue<ServerEnvironment>();
    private ServiceRegistration registration;
    private File modulesDir;
    private File bundlesDir;

    static ServiceController<?> addService(final ServiceTarget target) {
        ModuleIdentityArtifactProvider service = new ModuleIdentityArtifactProvider();
        ServiceBuilder<?> builder = target.addService(SERVICE_NAME, service);
        builder.addDependency(ServerEnvironmentService.SERVICE_NAME, ServerEnvironment.class, service.injectedEnvironment);
        builder.addDependency(Services.SYSTEM_CONTEXT, BundleContext.class, service.injectedSystemContext);
        builder.addDependency(Services.FRAMEWORK_CREATE);
        builder.setInitialMode(Mode.PASSIVE);
        return builder.install();
    }

    private ModuleIdentityArtifactProvider() {
    }

    @Override
    public void start(StartContext context) throws StartException {
        BundleContext syscontext = injectedSystemContext.getValue();
        Dictionary props = new Hashtable();
        props.put(Constants.SERVICE_RANKING, Integer.MAX_VALUE);
        registration = syscontext.registerService(ArtifactProviderPlugin.class.getName(), this, props);
        ServerEnvironment serverEnvironment = injectedEnvironment.getValue();
        modulesDir = serverEnvironment.getModulesDir();
        bundlesDir = serverEnvironment.getBundlesDir();
    }

    @Override
    public void stop(StopContext context) {
        if (registration != null) {
            registration.unregister();
            registration = null;
        }
    }

    @Override
    public Collection<Capability> findProviders(Requirement req) {
        String namespace = req.getNamespace();
        List<Capability> result = new ArrayList<Capability>();
        if (MODULE_IDENTITY_NAMESPACE.equals(namespace)) {
            String strval = (String) req.getAttributes().get(MODULE_IDENTITY_NAMESPACE);
            ModuleIdentifier moduleIdentifier = ModuleIdentifier.fromString(strval);
            try {
                File contentFile = getRepositoryEntry(bundlesDir, moduleIdentifier);
                if (contentFile != null) {
                    URL baseURL = bundlesDir.toURI().toURL();
                    String contentPath = contentFile.toURI().toURL().toExternalForm();
                    contentPath = contentPath.substring(baseURL.toExternalForm().length());
                    XResource resource = XResourceBuilder.create(baseURL, contentPath).getResource();
                    result.add(resource.getIdentityCapability());
                } else {
                    contentFile = getRepositoryEntry(modulesDir, moduleIdentifier);
                    if (contentFile != null) {
                        URL baseURL = modulesDir.toURI().toURL();
                        String contentPath = contentFile.toURI().toURL().toExternalForm();
                        contentPath = contentPath.substring(baseURL.toExternalForm().length());
                        XResource resource = XResourceBuilder.create(baseURL, contentPath).getResource();
                        result.add(resource.getIdentityCapability());
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
            ROOT_LOGGER.debugf("Cannot obtain directory: %s", entryDir);
            return null;
        }

        String[] files = entryDir.list(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".jar");
            }
        });
        if (files.length == 0) {
            ROOT_LOGGER.debugf("Cannot find jar in: %s", entryDir);
            return null;
        }
        if (files.length > 1) {
            ROOT_LOGGER.debugf("Multiple jars in: %s", entryDir);
            return null;
        }

        File entryFile = new File(entryDir + "/" + files[0]);
        if (entryFile.exists() == false) {
            ROOT_LOGGER.debugf("File does not exist: %s", entryFile);
            return null;
        }

        return entryFile;
    }

    @Override
    public String toString() {
        return ModuleIdentityArtifactProvider.class.getSimpleName();
    }
}

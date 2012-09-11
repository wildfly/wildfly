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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.osgi.OSGiConstants;
import org.jboss.as.osgi.parser.SubsystemState;
import org.jboss.as.osgi.parser.SubsystemState.OSGiCapability;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.ServerEnvironmentService;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.ServiceListener.Inheritance;
import org.jboss.msc.value.InjectedValue;
import org.jboss.osgi.deployment.deployer.Deployment;
import org.jboss.osgi.deployment.deployer.DeploymentFactory;
import org.jboss.osgi.framework.AbstractBundleRevisionAdaptor;
import org.jboss.osgi.framework.BootstrapBundlesInstall;
import org.jboss.osgi.framework.BundleManager;
import org.jboss.osgi.framework.IntegrationService;
import org.jboss.osgi.framework.Services;
import org.jboss.osgi.framework.StorageState;
import org.jboss.osgi.framework.StorageStatePlugin;
import org.jboss.osgi.metadata.OSGiMetaData;
import org.jboss.osgi.metadata.OSGiMetaDataBuilder;
import org.jboss.osgi.repository.XRepository;
import org.jboss.osgi.repository.XRequirementBuilder;
import org.jboss.osgi.resolver.MavenCoordinates;
import org.jboss.osgi.resolver.XBundleRevision;
import org.jboss.osgi.resolver.XBundleRevisionBuilderFactory;
import org.jboss.osgi.resolver.XCapability;
import org.jboss.osgi.resolver.XEnvironment;
import org.jboss.osgi.resolver.XResource;
import org.jboss.osgi.resolver.XResourceBuilder;
import org.jboss.osgi.spi.BundleInfo;
import org.jboss.osgi.spi.OSGiManifestBuilder;
import org.jboss.osgi.vfs.AbstractVFS;
import org.osgi.framework.BundleContext;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.repository.ContentNamespace;
import org.osgi.service.startlevel.StartLevel;

/**
 * An {@link IntegrationService} to install the configured capability bundles.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 11-Sep-2010
 */
class BootstrapBundlesIntegration extends BootstrapBundlesInstall<Void> implements IntegrationService<Void> {

    private final InjectedValue<BundleManager> injectedBundleManager = new InjectedValue<BundleManager>();
    private final InjectedValue<StorageStatePlugin> injectedStorageProvider = new InjectedValue<StorageStatePlugin>();
    private final InjectedValue<ServerEnvironment> injectedServerEnvironment = new InjectedValue<ServerEnvironment>();
    private final InjectedValue<BundleContext> injectedSystemContext = new InjectedValue<BundleContext>();
    private final InjectedValue<PackageAdmin> injectedPackageAdmin = new InjectedValue<PackageAdmin>();
    private final InjectedValue<StartLevel> injectedStartLevel = new InjectedValue<StartLevel>();
    private final InjectedValue<SubsystemState> injectedSubsystemState = new InjectedValue<SubsystemState>();
    private final InjectedValue<XEnvironment> injectedEnvironment = new InjectedValue<XEnvironment>();
    private final InjectedValue<XRepository> injectedRepository = new InjectedValue<XRepository>();
    private File bundlesDir;

    BootstrapBundlesIntegration() {
        super(BOOTSTRAP_BUNDLES);
    }

    ServiceController<Void> install(ServiceTarget serviceTarget, ServiceVerificationHandler verificationHandler) {
        ServiceController<Void> controller = super.install(serviceTarget);
        if (verificationHandler != null)
            controller.addListener(Inheritance.ALL, verificationHandler);
        return controller;
    }

    @Override
    protected void addServiceDependencies(ServiceBuilder<Void> builder) {
        builder.addDependency(ServerEnvironmentService.SERVICE_NAME, ServerEnvironment.class, injectedServerEnvironment);
        builder.addDependency(OSGiConstants.SUBSYSTEM_STATE_SERVICE_NAME, SubsystemState.class, injectedSubsystemState);
        builder.addDependency(OSGiConstants.REPOSITORY_SERVICE_NAME, XRepository.class, injectedRepository);
        builder.addDependency(Services.BUNDLE_MANAGER, BundleManager.class, injectedBundleManager);
        builder.addDependency(Services.PACKAGE_ADMIN, PackageAdmin.class, injectedPackageAdmin);
        builder.addDependency(IntegrationService.STORAGE_STATE_PLUGIN, StorageStatePlugin.class, injectedStorageProvider);
        builder.addDependency(Services.FRAMEWORK_CREATE, BundleContext.class, injectedSystemContext);
        builder.addDependency(Services.START_LEVEL, StartLevel.class, injectedStartLevel);
        builder.addDependency(Services.ENVIRONMENT, XEnvironment.class, injectedEnvironment);
    }

    @Override
    public synchronized void start(StartContext context) throws StartException {
        super.start(context);

        final BundleContext syscontext = injectedSystemContext.getValue();
        final List<Deployment> deployments = new ArrayList<Deployment>();
        try {
            ServerEnvironment serverEnvironment = injectedServerEnvironment.getValue();
            bundlesDir = serverEnvironment.getBundlesDir();
            if (bundlesDir.isDirectory() == false)
                throw MESSAGES.illegalStateCannotFindBundleDir(bundlesDir);

            final List<OSGiCapability> configcaps = new ArrayList<OSGiCapability>();
            for (String capspec : SystemPackagesIntegration.DEFAULT_CAPABILITIES) {
                configcaps.add(new OSGiCapability(capspec, null));
            }
            configcaps.addAll(injectedSubsystemState.getValue().getCapabilities());
            Iterator<OSGiCapability> iterator = configcaps.iterator();
            while (iterator.hasNext()) {
                OSGiCapability configcap = iterator.next();
                if (installInitialModuleCapability(configcap))
                    iterator.remove();
            }

            for (OSGiCapability configcap : configcaps) {
                Deployment dep = getInitialDeployment(syscontext, configcap);
                deployments.add(dep);
            }
        } catch (Exception ex) {
            throw MESSAGES.startFailedToProcessInitialCapabilites(ex);
        }

        // Install the bundles from the given locations
        installBootstrapBundles(context.getChildTarget(), deployments);
    }

    private boolean installInitialModuleCapability(OSGiCapability osgicap) throws Exception {
        String identifier = osgicap.getIdentifier();
        if (isValidModuleIdentifier(identifier)) {
            ModuleIdentifier moduleId = ModuleIdentifier.fromString(identifier);

            // Find the module in the bundles hierarchy
            File bundleFile = ModuleIdentityRepository.getRepositoryEntry(bundlesDir, moduleId);
            if (bundleFile == null) {
                LOGGER.tracef("Installing initial module capability: %s", identifier);

                // Attempt to load the module from the modules hierarchy
                final Module module;
                try {
                    ModuleLoader moduleLoader = Module.getBootModuleLoader();
                    module = moduleLoader.loadModule(moduleId);
                } catch (ModuleLoadException ex) {
                    throw MESSAGES.startFailedCannotResolveInitialCapability(ex, identifier);
                }
                if (module != null) {
                    final OSGiMetaData metadata = getModuleMetadata(module);
                    final BundleContext syscontext = injectedSystemContext.getValue();
                    XBundleRevisionBuilderFactory factory = new XBundleRevisionBuilderFactory() {
                        @Override
                        public XBundleRevision createResource() {
                            return new AbstractBundleRevisionAdaptor(syscontext, module);
                        }
                    };
                    XResourceBuilder builder = XBundleRevisionBuilderFactory.create(factory);
                    if (metadata != null) {
                        builder.loadFrom(metadata);
                    } else {
                        builder.loadFrom(module);
                    }
                    XResource resource = builder.getResource();
                    injectedEnvironment.getValue().installResources(resource);
                    return true;
                }
            }
        }
        return false;
    }

    private Deployment getInitialDeployment(BundleContext context, OSGiCapability osgicap) throws Exception {
        String identifier = osgicap.getIdentifier();
        Integer level = osgicap.getStartLevel();

        Deployment result = null;

        // Try the identifier as ModuleIdentifier
        if (isValidModuleIdentifier(identifier)) {
            ModuleIdentifier moduleId = ModuleIdentifier.fromString(identifier);

            // Attempt to install the bundle from the bundles hierarchy
            File bundleFile = ModuleIdentityRepository.getRepositoryEntry(bundlesDir, moduleId);
            if (bundleFile != null) {
                LOGGER.tracef("Installing initial bundle capability: %s", identifier);
                URL bundleURL = bundleFile.toURI().toURL();
                result = getDeploymentFromURL(bundleURL, identifier, level);
            }
        }

        // Try the identifier as MavenCoordinates
        else if (isValidMavenIdentifier(identifier)) {
            LOGGER.tracef("Installing initial maven capability: %s", identifier);
            XRepository repository = injectedRepository.getValue();
            MavenCoordinates mavenId = MavenCoordinates.parse(identifier);
            Requirement req = XRequirementBuilder.create(mavenId).getRequirement();
            Collection<Capability> caps = repository.findProviders(req);
            if (caps.isEmpty() == false) {
                XResource resource = (XResource) caps.iterator().next().getResource();
                XCapability ccap = (XCapability) resource.getCapabilities(ContentNamespace.CONTENT_NAMESPACE).get(0);
                URL bundleURL = new URL((String) ccap.getAttribute(ContentNamespace.CAPABILITY_URL_ATTRIBUTE));
                result = getDeploymentFromURL(bundleURL, identifier, level);
            }
        }

        if (result == null)
            throw MESSAGES.startFailedCannotResolveInitialCapability(null, identifier);

        return result;
    }

    private boolean isValidModuleIdentifier(String identifier) {
        try {
            ModuleIdentifier.fromString(identifier);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private boolean isValidMavenIdentifier(String identifier) {
        try {
            MavenCoordinates.parse(identifier);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private Deployment getDeploymentFromURL(URL bundleURL, String location, Integer level) throws Exception {
        BundleInfo info = BundleInfo.createBundleInfo(AbstractVFS.toVirtualFile(bundleURL), location);
        Deployment dep = DeploymentFactory.createDeployment(info);
        if (level != null) {
            dep.setStartLevel(level.intValue());
            dep.setAutoStart(true);
        }
        StorageStatePlugin storageProvider = injectedStorageProvider.getValue();
        StorageState storageState = storageProvider.getByLocation(dep.getLocation());
        if (storageState != null) {
            dep.addAttachment(StorageState.class, storageState);
        }
        return dep;
    }

    private OSGiMetaData getModuleMetadata(Module module) throws IOException {
        URL manifestURL = module.getClassLoader().getResource(JarFile.MANIFEST_NAME);
        if (manifestURL != null) {
            InputStream input = manifestURL.openStream();
            try {
                Manifest manifest = new Manifest(input);
                if (OSGiManifestBuilder.isValidBundleManifest(manifest)) {
                    return OSGiMetaDataBuilder.load(manifest);
                }
            } finally {
                input.close();
            }
        }
        File homeDir = injectedServerEnvironment.getValue().getHomeDir();
        final File modulesDir = new File(homeDir + File.separator + "modules");
        final ModuleIdentifier identifier = module.getIdentifier();

        String identifierPath = identifier.getName().replace('.', File.separatorChar) + File.separator + identifier.getSlot();
        File entryFile = new File(modulesDir + File.separator + identifierPath + File.separator + "jbosgi-xservice.properties");
        if (entryFile.exists()) {
            FileInputStream input = new FileInputStream(entryFile);
            try {
                Properties props = new Properties();
                props.load(input);
                return OSGiMetaDataBuilder.load(props);
            } finally {
                input.close();
            }
        }
        return null;
    }
}

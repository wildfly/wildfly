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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.jboss.as.osgi.parser.SubsystemState;
import org.jboss.as.osgi.parser.SubsystemState.OSGiCapability;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.ServerEnvironmentService;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.value.InjectedValue;
import org.jboss.osgi.deployment.deployer.Deployment;
import org.jboss.osgi.deployment.deployer.DeploymentFactory;
import org.jboss.osgi.framework.AutoInstallHandler;
import org.jboss.osgi.framework.AutoInstallHandlerComplete;
import org.jboss.osgi.framework.BundleManager;
import org.jboss.osgi.framework.Constants;
import org.jboss.osgi.framework.IntegrationServices;
import org.jboss.osgi.framework.Services;
import org.jboss.osgi.framework.StorageStateProvider;
import org.jboss.osgi.metadata.OSGiMetaData;
import org.jboss.osgi.metadata.OSGiMetaDataBuilder;
import org.jboss.osgi.resolver.MavenCoordinates;
import org.jboss.osgi.resolver.XEnvironment;
import org.jboss.osgi.resolver.XIdentityCapability;
import org.jboss.osgi.resolver.XRequirementBuilder;
import org.jboss.osgi.resolver.XResource;
import org.jboss.osgi.resolver.XResourceBuilder;
import org.jboss.osgi.resolver.XResourceBuilderFactory;
import org.jboss.osgi.resolver.XResourceConstants;
import org.jboss.osgi.spi.BundleInfo;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.repository.Repository;
import org.osgi.service.startlevel.StartLevel;

/**
 * Integration point to auto install bundles at framework startup.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 11-Sep-2010
 */
class AutoInstallIntegration extends AbstractService<AutoInstallHandler> implements AutoInstallHandler {

    private final InjectedValue<BundleManager> injectedBundleManager = new InjectedValue<BundleManager>();
    private final InjectedValue<StorageStateProvider> injectedStorageProvider = new InjectedValue<StorageStateProvider>();
    private final InjectedValue<ServerEnvironment> injectedServerEnvironment = new InjectedValue<ServerEnvironment>();
    private final InjectedValue<Repository> injectedRepository = new InjectedValue<Repository>();
    private final InjectedValue<Bundle> injectedSystemBundle = new InjectedValue<Bundle>();
    private final InjectedValue<PackageAdmin> injectedPackageAdmin = new InjectedValue<PackageAdmin>();
    private final InjectedValue<StartLevel> injectedStartLevel = new InjectedValue<StartLevel>();
    private final InjectedValue<SubsystemState> injectedSubsystemState = new InjectedValue<SubsystemState>();
    private final InjectedValue<XEnvironment> injectedEnvironment = new InjectedValue<XEnvironment>();
    private File bundlesDir;

    static ServiceController<?> addService(final ServiceTarget target) {
        AutoInstallIntegration service = new AutoInstallIntegration();
        ServiceBuilder<?> builder = target.addService(IntegrationServices.AUTOINSTALL_HANDLER, service);
        builder.addDependency(ServerEnvironmentService.SERVICE_NAME, ServerEnvironment.class, service.injectedServerEnvironment);
        builder.addDependency(SubsystemState.SERVICE_NAME, SubsystemState.class, service.injectedSubsystemState);
        builder.addDependency(RepositoryProvider.SERVICE_NAME, Repository.class, service.injectedRepository);
        builder.addDependency(Services.BUNDLE_MANAGER, BundleManager.class, service.injectedBundleManager);
        builder.addDependency(Services.PACKAGE_ADMIN, PackageAdmin.class, service.injectedPackageAdmin);
        builder.addDependency(Services.STORAGE_STATE_PROVIDER, StorageStateProvider.class, service.injectedStorageProvider);
        builder.addDependency(Services.SYSTEM_BUNDLE, Bundle.class, service.injectedSystemBundle);
        builder.addDependency(Services.START_LEVEL, StartLevel.class, service.injectedStartLevel);
        builder.addDependency(Services.ENVIRONMENT, XEnvironment.class, service.injectedEnvironment);
        builder.addDependency(Services.FRAMEWORK_CREATE);
        builder.setInitialMode(Mode.ON_DEMAND);
        return builder.install();
    }

    AutoInstallIntegration() {
    }

    @Override
    public synchronized void start(StartContext context) throws StartException {
        ServiceController<?> serviceController = context.getController();
        LOGGER.debugf("Starting: %s in mode %s", serviceController.getName(), serviceController.getMode());

        final Map<ServiceName, Deployment> installedBundles = new LinkedHashMap<ServiceName, Deployment>();
        final Set<ServiceName> resolvableServices = new LinkedHashSet<ServiceName>();

        final BundleContext syscontext = injectedSystemBundle.getValue().getBundleContext();
        final String startLevelProp = syscontext.getProperty(Constants.FRAMEWORK_BEGINNING_STARTLEVEL);
        final int beginningStartLevel = startLevelProp != null ? Integer.parseInt(startLevelProp) : 1;

        try {
            ServiceTarget serviceTarget = context.getChildTarget();

            ServerEnvironment serverEnvironment = injectedServerEnvironment.getValue();
            bundlesDir = serverEnvironment.getBundlesDir();
            if (bundlesDir.isDirectory() == false)
                throw MESSAGES.illegalStateCannotFindBundleDir(bundlesDir);

            List<OSGiCapability> configcaps = new ArrayList<OSGiCapability>();
            configcaps.add(new OSGiCapability("org.osgi.enterprise", null));
            configcaps.addAll(injectedSubsystemState.getValue().getCapabilities());
            for (OSGiCapability moduleMetaData : configcaps) {
                ServiceName serviceName = installCapability(serviceTarget, moduleMetaData, installedBundles);
                int startLevel = moduleMetaData.getStartLevel() != null ? moduleMetaData.getStartLevel() : 1;
                if (serviceName != null && startLevel <= beginningStartLevel) {
                    resolvableServices.add(serviceName);
                }
            }

            AutoInstallHandlerComplete installComplete = new AutoInstallHandlerComplete(installedBundles) {
                @Override
                public void start(StartContext context) throws StartException {
                    // Resolve all bundles up until and including the Framework beginning start level
                    Set<Bundle> resolvableBundles = new LinkedHashSet<Bundle>();
                    ServiceContainer serviceContainer = context.getController().getServiceContainer();
                    for (ServiceName serviceName : resolvableServices) {
                        ServiceController<?> requiredService = serviceContainer.getRequiredService(serviceName);
                        resolvableBundles.add((Bundle) requiredService.getValue());
                    }
                    Bundle[] bundleArr = resolvableBundles.toArray(new Bundle[resolvableBundles.size()]);
                    PackageAdmin packageAdmin = injectedPackageAdmin.getValue();
                    packageAdmin.resolveBundles(bundleArr);
                    super.start(context);
                }
            };
            installComplete.install(serviceTarget);
        } catch (Exception ex) {
            throw MESSAGES.startFailedToProcessInitialCapabilites(ex);
        }
    }

    private ServiceName  installCapability(ServiceTarget serviceTarget, OSGiCapability osgicap, Map<ServiceName, Deployment> installedBundles) throws Exception {
        String identifier = osgicap.getIdentifier();
        Integer startLevel = osgicap.getStartLevel();

        // Try the identifier as ModuleIdentifier
        if (isValidModuleIdentifier(identifier)) {
            ModuleIdentifier moduleId = ModuleIdentifier.fromString(identifier);

            // Attempt to install the bundle from the bundles hierarchy
            File bundleFile = ModuleIdentityArtifactProvider.getRepositoryEntry(bundlesDir, moduleId);
            if (bundleFile != null) {
                URL bundleURL = bundleFile.toURI().toURL();
                return installBundleFromURL(serviceTarget, bundleURL, startLevel, installedBundles);
            }

            // Attempt to load the module from the modules hierarchy
            Module module = null;
            try {
                ModuleLoader moduleLoader = Module.getBootModuleLoader();
                module = moduleLoader.loadModule(moduleId);
            } catch (ModuleLoadException e) {
                LOGGER.debugf("Cannot load module: %s", moduleId);
            }
            if (module != null) {
                OSGiMetaData metadata = getModuleMetadata(module);
                XResourceBuilder builder = XResourceBuilderFactory.create();
                if (metadata != null) {
                    builder.loadFrom(metadata);
                } else {
                    builder.loadFrom(module);
                }
                XResource res = builder.getResource();
                res.addAttachment(Module.class, module);
                injectedEnvironment.getValue().installResources(res);
                return null;
            }
        }

        // Try the identifier as MavenCoordinates
        else if (isValidMavenIdentifier(identifier)) {
            Repository repository = injectedRepository.getValue();
            MavenCoordinates mavenId = MavenCoordinates.parse(identifier);
            Requirement req = XRequirementBuilder.createArtifactRequirement(mavenId);
            Collection<Capability> caps = repository.findProviders(Collections.singleton(req)).get(req);
            if (caps.isEmpty() == false) {
                XIdentityCapability icap = (XIdentityCapability) caps.iterator().next();
                URL bundleURL = (URL) icap.getAttribute(XResourceConstants.CONTENT_URL);
                return installBundleFromURL(serviceTarget, bundleURL, startLevel, installedBundles);
            }
        }
        LOGGER.warnCannotResolveCapability(identifier);
        return null;
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

    private ServiceName installBundleFromURL(ServiceTarget serviceTarget, URL bundleURL, Integer startLevel, Map<ServiceName, Deployment> installedBundles) throws Exception {
        BundleManager bundleManager = injectedBundleManager.getValue();
        BundleInfo info = BundleInfo.createBundleInfo(bundleURL);
        Deployment dep = DeploymentFactory.createDeployment(info);
        if (startLevel != null) {
            dep.setStartLevel(startLevel.intValue());
            dep.setAutoStart(true);
        }
        ServiceName serviceName = bundleManager.installBundle(serviceTarget, dep);
        installedBundles.put(serviceName, dep);
        return serviceName;
    }

    @Override
    public synchronized AutoInstallIntegration getValue() throws IllegalStateException {
        return this;
    }

    private OSGiMetaData getModuleMetadata(Module module) throws IOException {

        URL manifestURL = module.getClassLoader().getResource(JarFile.MANIFEST_NAME);
        if (manifestURL != null) {
            InputStream input = manifestURL.openStream();
            try {
                Manifest manifest = new Manifest(input);
                if (BundleInfo.isValidBundleManifest(manifest)) {
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
                return OSGiMetaDataBuilder.load(input);
            } finally {
                input.close();
            }
        }
        return null;
    }
}

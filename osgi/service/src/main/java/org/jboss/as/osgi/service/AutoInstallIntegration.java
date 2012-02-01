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

import org.jboss.as.osgi.parser.SubsystemState;
import org.jboss.as.osgi.parser.SubsystemState.ChangeEvent;
import org.jboss.as.osgi.parser.SubsystemState.ChangeType;
import org.jboss.as.osgi.parser.SubsystemState.OSGiCapability;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.ServerEnvironmentService;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
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
import org.jboss.osgi.framework.AutoInstallProvider;
import org.jboss.osgi.framework.BundleManagerService;
import org.jboss.osgi.framework.Services;
import org.jboss.osgi.metadata.OSGiMetaData;
import org.jboss.osgi.metadata.OSGiMetaDataBuilder;
import org.jboss.osgi.spi.util.BundleInfo;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.service.startlevel.StartLevel;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.atomic.AtomicLong;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import static org.jboss.as.osgi.OSGiLogger.ROOT_LOGGER;
import static org.jboss.as.osgi.OSGiMessages.MESSAGES;

/**
 * Integration point to auto install bundles at framework startup.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 11-Sep-2010
 */
class AutoInstallIntegration extends AbstractService<AutoInstallProvider> implements AutoInstallProvider, Observer {

    final InjectedValue<BundleManagerService> injectedBundleManager = new InjectedValue<BundleManagerService>();
    final InjectedValue<ServerEnvironment> injectedEnvironment = new InjectedValue<ServerEnvironment>();
    final InjectedValue<Bundle> injectedSystemBundle = new InjectedValue<Bundle>();
    final InjectedValue<StartLevel> injectedStartLevel = new InjectedValue<StartLevel>();
    final InjectedValue<SubsystemState> injectedSubsystemState = new InjectedValue<SubsystemState>();
    ServiceController<?> serviceController;

    private File bundlesDir;
    private ServiceTarget serviceTarget;
    private final AtomicLong updateServiceIdCounter = new AtomicLong();

    static ServiceController<?> addService(final ServiceTarget target) {
        AutoInstallIntegration service = new AutoInstallIntegration();
        ServiceBuilder<?> builder = target.addService(Services.AUTOINSTALL_PROVIDER, service);
        builder.addDependency(ServerEnvironmentService.SERVICE_NAME, ServerEnvironment.class, service.injectedEnvironment);
        builder.addDependency(SubsystemState.SERVICE_NAME, SubsystemState.class, service.injectedSubsystemState);
        builder.addDependency(Services.BUNDLE_MANAGER, BundleManagerService.class, service.injectedBundleManager);
        builder.addDependency(Services.SYSTEM_BUNDLE, Bundle.class, service.injectedSystemBundle);
        builder.addDependency(Services.START_LEVEL, StartLevel.class, service.injectedStartLevel);
        builder.addDependency(Services.FRAMEWORK_INIT);
        builder.setInitialMode(Mode.ON_DEMAND);
        return builder.install();
    }

    AutoInstallIntegration() {
    }

    @Override
    public synchronized void start(StartContext context) throws StartException {
        serviceController = context.getController();
        ROOT_LOGGER.debugf("Starting: %s in mode %s", serviceController.getName(), serviceController.getMode());

        final Map<ServiceName, OSGiCapability> pendingServices = new LinkedHashMap<ServiceName, OSGiCapability>();
        try {
            final BundleManagerService bundleManager = injectedBundleManager.getValue();
            final ServiceContainer serviceContainer = serviceController.getServiceContainer();
            serviceTarget = context.getChildTarget();

            ServerEnvironment serverEnvironment = injectedEnvironment.getValue();
            bundlesDir = serverEnvironment.getBundlesDir();

            if (bundlesDir.isDirectory() == false)
                throw MESSAGES.cannotFindBundleDir(bundlesDir);

            injectedSubsystemState.getValue().addObserver(this);

            List<OSGiCapability> configcaps = new ArrayList<OSGiCapability>();
            configcaps.add(new OSGiCapability("javax.api", null));
            configcaps.add(new OSGiCapability("org.osgi.enterprise", null));
            configcaps.add(new OSGiCapability("org.jboss.osgi.repository.api", null));
            configcaps.addAll(injectedSubsystemState.getValue().getCapabilities());
            for (OSGiCapability moduleMetaData : configcaps) {
                ServiceName serviceName = installModule(bundleManager, moduleMetaData);
                if (serviceName != null) {
                    pendingServices.put(serviceName, moduleMetaData);
                }
            }

            // Install a service that has a dependency on all pending bundle INSTALLED services
            ServiceName servicesInstalled = Services.AUTOINSTALL_PROVIDER.append("INSTALLED");
            ServiceBuilder<Void> builder = serviceTarget.addService(servicesInstalled, new AbstractService<Void>() {
                public void start(StartContext context) throws StartException {
                    ROOT_LOGGER.debugf("Auto bundles installed");
                }
            });
            builder.addDependencies(pendingServices.keySet());
            builder.install();

            // Install a service that starts the bundles
            builder = serviceTarget.addService(Services.AUTOINSTALL_PROVIDER_COMPLETE, new AbstractService<Void>() {
                public void start(StartContext context) throws StartException {
                    for (ServiceName serviceName : pendingServices.keySet()) {
                        OSGiCapability moduleMetaData = pendingServices.get(serviceName);
                        startBundle(serviceContainer, serviceName, moduleMetaData);
                    }
                    ROOT_LOGGER.debugf("Auto bundles bundles started");
                }
            });
            builder.addDependencies(servicesInstalled);
            builder.install();

        } catch (Exception ex) {
            throw new StartException(MESSAGES.failedToCreateAutoInstallList(), ex);
        }
    }

    ServiceName installModule(BundleManagerService bundleManager, OSGiCapability moduleMetaData) throws Exception {
        String identifier = moduleMetaData.getIdentifier();
        Integer startLevel = moduleMetaData.getStartLevel();

        // Try the identifier as ModuleIdentifier
        if (isValidModuleIdentifier(identifier)) {
            ModuleIdentifier moduleId = ModuleIdentifier.fromString(identifier);

            // Attempt to install bundle from the bundles hirarchy
            File modulesFile = ModuleIdentityArtifactProvider.getRepositoryEntry(bundlesDir, moduleId);
            if (modulesFile != null) {
                URL url = modulesFile.toURI().toURL();
                return installBundleFromURL(bundleManager, url, startLevel);
            }

            // Register module with the OSGi layer
            ModuleLoader moduleLoader = Module.getBootModuleLoader();
            Module module = moduleLoader.loadModule(moduleId);
            OSGiMetaData metadata = getModuleMetadata(module);
            return bundleManager.registerModule(serviceTarget, module, metadata);
        }

        ROOT_LOGGER.cannotResolveCapability(identifier);
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

    private ServiceName installBundleFromURL(BundleManagerService bundleManager, URL moduleURL, Integer startLevel) throws Exception {
        BundleInfo info = BundleInfo.createBundleInfo(moduleURL);
        Deployment dep = DeploymentFactory.createDeployment(info);
        if (startLevel != null) {
            dep.setStartLevel(startLevel.intValue());
        }
        return bundleManager.installBundle(serviceTarget, dep);
    }

    void startBundle(final ServiceContainer serviceContainer, ServiceName serviceName, OSGiCapability moduleMetaData) {
        if (moduleMetaData.getStartLevel() != null) {
            ServiceController<Bundle> controller = (ServiceController<Bundle>) serviceContainer.getRequiredService(serviceName);
            Bundle bundle = controller.getValue();
            StartLevel startLevel = injectedStartLevel.getValue();
            startLevel.setBundleStartLevel(bundle, moduleMetaData.getStartLevel());
            try {
                bundle.start();
            } catch (BundleException ex) {
                ROOT_LOGGER.cannotStart(ex, bundle);
            }
        }
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
        final File modulesDir = injectedEnvironment.getValue().getModulesDir();
        final ModuleIdentifier identifier = module.getIdentifier();

        String identifierPath = identifier.getName().replace('.', File.separatorChar) + File.separator + identifier.getSlot();
        File entryFile = new File(modulesDir + File.separator + identifierPath + File.separator + "jbosgi-xservice.properties");
        if (entryFile.exists() == false) {
            ROOT_LOGGER.debugf("Cannot obtain OSGi metadata file: %s", entryFile);
            return null;
        }

        FileInputStream input = new FileInputStream(entryFile);
        try {
            return OSGiMetaDataBuilder.load(input);
        } finally {
            input.close();
        }
    }

    // Called when the SubsystemState changes.
    @Override
    public void update(Observable o, Object arg) {
        if (arg instanceof SubsystemState.ChangeEvent == false)
            return;

        SubsystemState.ChangeEvent event = (ChangeEvent) arg;
        if (event.getType() != ChangeType.CAPABILITY)
            return;

        if (event.isRemoved() == false) {
            try {
                for (final OSGiCapability module : injectedSubsystemState.getValue().getCapabilities()) {
                    if (module.getIdentifier().toString().equals(event.getId())) {
                        final ServiceName serviceName = installModule(injectedBundleManager.getValue(), module);
                        if (serviceName != null) {
                            ServiceBuilder<Void> builder = serviceController.getServiceContainer().addService(
                                    ServiceName.of(Services.AUTOINSTALL_PROVIDER, "ModuleUpdater", "" + updateServiceIdCounter.incrementAndGet()),
                                    new AbstractService<Void>() {
                                        @Override
                                        public void start(StartContext context) throws StartException {
                                            try {
                                                startBundle(serviceController.getServiceContainer(), serviceName, module);
                                            } finally {
                                                // Remove this temporary service
                                                context.getController().setMode(Mode.REMOVE);
                                            }
                                        }
                                    });
                            builder.addDependency(serviceName);
                            builder.install();
                        }
                        return;
                    }
                }
            } catch (Exception e) {
                ROOT_LOGGER.errorAddingModule(e, event.getId());
                return;
            }
            ROOT_LOGGER.moduleNotFound(event.getId());
        }
    }
}

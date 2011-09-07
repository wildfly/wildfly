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

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.atomic.AtomicLong;

import org.jboss.as.osgi.parser.SubsystemState;
import org.jboss.as.osgi.parser.SubsystemState.ChangeEvent;
import org.jboss.as.osgi.parser.SubsystemState.ChangeType;
import org.jboss.as.osgi.parser.SubsystemState.OSGiModule;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.ServerEnvironmentService;
import org.jboss.logging.Logger;
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
import org.jboss.osgi.vfs.AbstractVFS;
import org.jboss.osgi.vfs.VirtualFile;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.service.startlevel.StartLevel;

/**
 * Integration point to auto install bundles at framework startup.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 11-Sep-2010
 */
class AutoInstallIntegration extends AbstractService<AutoInstallProvider> implements AutoInstallProvider, Observer {

    private static final Logger log = Logger.getLogger("org.jboss.as.osgi");

    final InjectedValue<BundleManagerService> injectedBundleManager = new InjectedValue<BundleManagerService>();
    final InjectedValue<ServerEnvironment> injectedEnvironment = new InjectedValue<ServerEnvironment>();
    final InjectedValue<Bundle> injectedSystemBundle = new InjectedValue<Bundle>();
    final InjectedValue<StartLevel> injectedStartLevel = new InjectedValue<StartLevel>();
    final InjectedValue<SubsystemState> injectedSubsystemState = new InjectedValue<SubsystemState>();
    ServiceController<?> serviceController;

    private File modulesDir;
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
        log.debugf("Starting: %s in mode %s", serviceController.getName(), serviceController.getMode());

        final Map<ServiceName, OSGiModule> pendingServices = new LinkedHashMap<ServiceName, OSGiModule>();
        try {
            final BundleManagerService bundleManager = injectedBundleManager.getValue();
            final ServiceContainer serviceContainer = serviceController.getServiceContainer();
            serviceTarget = context.getChildTarget();

            modulesDir = injectedEnvironment.getValue().getModulesDir();
            bundlesDir = new File(modulesDir.getPath() + "/../bundles").getCanonicalFile();

            if (bundlesDir.isDirectory() == false)
                throw new IllegalStateException("Cannot find bundles directory: " + bundlesDir);

            injectedSubsystemState.getValue().addObserver(this);

            for (OSGiModule moduleMetaData : injectedSubsystemState.getValue().getModules()) {
                ServiceName serviceName = installModule(bundleManager, moduleMetaData);
                pendingServices.put(serviceName, moduleMetaData);
            }

            // Install a service that has a dependency on all pending bundle INSTALLED services
            ServiceName servicesInstalled = Services.AUTOINSTALL_PROVIDER.append("INSTALLED");
            ServiceBuilder<Void> builder = serviceTarget.addService(servicesInstalled, new AbstractService<Void>() {
                public void start(StartContext context) throws StartException {
                    log.debugf("Auto bundles installed");
                }
            });
            builder.addDependencies(pendingServices.keySet());
            builder.install();

            // Install a service that starts the bundles
            builder = serviceTarget.addService(Services.AUTOINSTALL_PROVIDER_COMPLETE, new AbstractService<Void>() {
                public void start(StartContext context) throws StartException {
                    for (ServiceName serviceName : pendingServices.keySet()) {
                        OSGiModule moduleMetaData = pendingServices.get(serviceName);
                        startBundle(serviceContainer, serviceName, moduleMetaData);
                    }
                    log.debugf("Auto bundles bundles started");
                }
            });
            builder.addDependencies(servicesInstalled);
            builder.install();

        } catch (Exception ex) {
            throw new StartException("Failed to create auto install list", ex);
        }
    }

    ServiceName installModule(BundleManagerService bundleManager, OSGiModule moduleMetaData) throws Exception {
        ModuleIdentifier identifier = moduleMetaData.getIdentifier();
        Integer startLevel = moduleMetaData.getStartLevel();

        // Attempt to install bundle from the bundles hirarchy
        File modulesFile = getRepositoryEntry(bundlesDir, identifier);
        if (modulesFile != null) {
            URL url = modulesFile.toURI().toURL();
            return installBundleFromURL(bundleManager, url, startLevel);
        }

        // Attempt to install bundle from the modules hirarchy
        modulesFile = getRepositoryEntry(modulesDir, identifier);
        if (modulesFile != null) {
            URL url = modulesFile.toURI().toURL();
            VirtualFile virtualFile = AbstractVFS.toVirtualFile(url);
            if (BundleInfo.isValidBundle(virtualFile)) {
                log.warnf("Found OSGi bundle in modules hirachy: %s", modulesFile);
                return installBundleFromURL(bundleManager, url, startLevel);
            }
        }

        // Register module with the OSGi layer
        ModuleLoader moduleLoader = Module.getBootModuleLoader();
        Module module = moduleLoader.loadModule(identifier);
        OSGiMetaData metadata = getModuleMetadata(module);
        return bundleManager.registerModule(serviceTarget, module, metadata);
    }

    private ServiceName installBundleFromURL(BundleManagerService bundleManager, URL moduleURL, Integer startLevel) throws Exception {
        BundleInfo info = BundleInfo.createBundleInfo(moduleURL);
        Deployment dep = DeploymentFactory.createDeployment(info);
        if (startLevel != null) {
            dep.setStartLevel(startLevel.intValue());
        }
        return bundleManager.installBundle(serviceTarget, dep);
    }

    void startBundle(final ServiceContainer serviceContainer, ServiceName serviceName, OSGiModule moduleMetaData) {
        if (moduleMetaData.getStartLevel() != null) {
            @SuppressWarnings("unchecked")
            ServiceController<Bundle> controller = (ServiceController<Bundle>) serviceContainer.getRequiredService(serviceName);
            Bundle bundle = controller.getValue();
            StartLevel startLevel = injectedStartLevel.getValue();
            startLevel.setBundleStartLevel(bundle, moduleMetaData.getStartLevel());
            try {
                bundle.start();
            } catch (BundleException ex) {
                log.errorf(ex, "Cannot start bundle: %s", bundle);
            }
        }
    }

    @Override
    public synchronized AutoInstallIntegration getValue() throws IllegalStateException {
        return this;
    }

    /**
     * Get file for the singe jar that corresponds to the given identifier
     */
    private File getRepositoryEntry(File rootDir, ModuleIdentifier identifier) throws IOException {

        String identifierPath = identifier.getName().replace('.', '/') + "/" + identifier.getSlot();
        File entryDir = new File(rootDir + "/" + identifierPath);
        if (entryDir.isDirectory() == false) {
            log.debugf("Cannot obtain directory: %s", entryDir);
            return null;
        }

        String[] files = entryDir.list(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".jar");
            }
        });
        if (files.length == 0) {
            log.debugf("Cannot find jar in: %s", entryDir);
            return null;
        }
        if (files.length > 1) {
            log.debugf("Multiple jars in: %s", entryDir);
            return null;
        }

        File entryFile = new File(entryDir + "/" + files[0]);
        if (entryFile.exists() == false) {
            log.debugf("File does not exist: %s", entryFile);
            return null;
        }

        return entryFile;
    }

    private OSGiMetaData getModuleMetadata(Module module) throws IOException {
        final File modulesDir = injectedEnvironment.getValue().getModulesDir();
        final ModuleIdentifier identifier = module.getIdentifier();

        String identifierPath = identifier.getName().replace('.', '/') + "/" + identifier.getSlot();
        File entryFile = new File(modulesDir + "/" + identifierPath + "/jbosgi-xservice.properties");
        if (entryFile.exists() == false) {
            log.debugf("Cannot obtain OSGi metadata file: %s", entryFile);
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
        if (event.getType() != ChangeType.MODULE)
            return;

        if (!event.isRemoved()) {
            try {
                for (final OSGiModule module : injectedSubsystemState.getValue().getModules()) {
                    if (module.getIdentifier().toString().equals(event.getId())) {
                        final ServiceName serviceName = installModule(injectedBundleManager.getValue(), module);

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
                        return;
                    }
                }
            } catch (Exception e) {
                log.errorf(e, "Problem adding module: %s", event.getId());
                return;
            }
            log.errorf("Cannot add module as it was not found: %s", event.getId());
        }
    }
}

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

import org.jboss.as.osgi.parser.SubsystemState;
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
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.service.startlevel.StartLevel;

/**
 * Integration point to auto install bundles at framework startup.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 11-Sep-2010
 */
final class AutoInstallIntegration extends AbstractService<AutoInstallProvider> implements AutoInstallProvider {

    private static final Logger log = Logger.getLogger("org.jboss.as.osgi");

    private InjectedValue<BundleManagerService> injectedBundleManager = new InjectedValue<BundleManagerService>();
    private final InjectedValue<ServerEnvironment> injectedEnvironment = new InjectedValue<ServerEnvironment>();
    private InjectedValue<Bundle> injectedSystemBundle = new InjectedValue<Bundle>();
    private InjectedValue<StartLevel> injectedStartLevel = new InjectedValue<StartLevel>();
    private SubsystemState subsystemState;

    static ServiceController<?> addService(final ServiceTarget target, final SubsystemState subsystemState) {
        AutoInstallIntegration service = new AutoInstallIntegration(subsystemState);
        ServiceBuilder<?> builder = target.addService(Services.AUTOINSTALL_PROVIDER, service);
        builder.addDependency(ServerEnvironmentService.SERVICE_NAME, ServerEnvironment.class, service.injectedEnvironment);
        builder.addDependency(Services.BUNDLE_MANAGER, BundleManagerService.class, service.injectedBundleManager);
        builder.addDependency(Services.SYSTEM_BUNDLE, Bundle.class, service.injectedSystemBundle);
        builder.addDependency(Services.START_LEVEL, StartLevel.class, service.injectedStartLevel);
        builder.addDependency(Services.FRAMEWORK_INIT);
        builder.setInitialMode(Mode.ON_DEMAND);
        return builder.install();
    }

    private AutoInstallIntegration(SubsystemState subsystemState) {
        this.subsystemState = subsystemState;
    }

    @Override
    public void start(StartContext context) throws StartException {
        ServiceController<?> controller = context.getController();
        log.debugf("Starting: %s in mode %s", controller.getName(), controller.getMode());
        final Map<ServiceName, OSGiModule> pendingServices = new LinkedHashMap<ServiceName, OSGiModule>();
        try {
            final BundleManagerService bundleManager = injectedBundleManager.getValue();
            final ServiceContainer serviceContainer = context.getController().getServiceContainer();
            final ServiceTarget serviceTarget = context.getChildTarget();
            final File modulesDir = injectedEnvironment.getValue().getModulesDir();
            final File bundlesDir = new File(modulesDir.getPath() + "/../bundles").getCanonicalFile();
            for (OSGiModule moduleMetaData : subsystemState.getModules()) {
                ServiceName serviceName;
                ModuleIdentifier identifier = moduleMetaData.getIdentifier();
                File bundleFile = getRepositoryEntry(bundlesDir, identifier);
                if (bundleFile != null) {
                    URL url = bundleFile.toURI().toURL();
                    BundleInfo info = BundleInfo.createBundleInfo(url);
                    Deployment dep = DeploymentFactory.createDeployment(info);
                    Integer startLevel = moduleMetaData.getStartLevel();
                    if (startLevel != null)
                        dep.setStartLevel(startLevel.intValue());
                    serviceName = bundleManager.installBundle(serviceTarget, dep);
                } else {
                    ModuleLoader moduleLoader = Module.getBootModuleLoader();
                    Module module = moduleLoader.loadModule(identifier);
                    OSGiMetaData metadata = getModuleMetadata(module);
                    serviceName = bundleManager.registerModule(serviceTarget, module, metadata);
                }
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
                    log.debugf("Auto bundles bundles started");
                }
            });
            builder.addDependencies(servicesInstalled);
            builder.install();

        } catch (Exception ex) {
            throw new StartException("Failed to create auto install list", ex);
        }
    }

    @Override
    public AutoInstallIntegration getValue() throws IllegalStateException {
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
}

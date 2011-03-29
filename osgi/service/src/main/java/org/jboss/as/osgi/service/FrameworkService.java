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

import static org.jboss.as.osgi.parser.SubsystemState.PROP_JBOSS_OSGI_SYSTEM_MODULES;
import static org.jboss.osgi.framework.Constants.JBOSGI_PREFIX;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.MBeanServer;

import org.jboss.as.jmx.MBeanServerService;
import org.jboss.as.osgi.parser.SubsystemState;
import org.jboss.as.osgi.parser.SubsystemState.OSGiModule;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.ServerEnvironmentService;
import org.jboss.as.server.Services;
import org.jboss.as.server.moduleservice.ServiceModuleLoader;
import org.jboss.as.server.services.net.SocketBinding;
import org.jboss.logging.Logger;
import org.jboss.modules.DependencySpec;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.jboss.modules.ModuleSpec;
import org.jboss.modules.filter.PathFilters;
import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.osgi.deployment.deployer.DeployerService;
import org.jboss.osgi.deployment.deployer.Deployment;
import org.jboss.osgi.deployment.deployer.DeploymentFactory;
import org.jboss.osgi.deployment.deployer.SystemDeployerService;
import org.jboss.osgi.framework.BundleReferenceClassLoader;
import org.jboss.osgi.framework.DeployerServiceProvider;
import org.jboss.osgi.framework.FrameworkExt;
import org.jboss.osgi.framework.FrameworkModuleProvider;
import org.jboss.osgi.framework.ModuleLoaderProvider;
import org.jboss.osgi.framework.SystemBundleProvider;
import org.jboss.osgi.framework.SystemModuleProvider;
import org.jboss.osgi.framework.internal.FrameworkBuilder;
import org.jboss.osgi.spi.util.BundleInfo;
import org.jboss.osgi.vfs.AbstractVFS;
import org.jboss.osgi.vfs.VirtualFile;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.launch.Framework;

/**
 * Service responsible for creating and managing the life-cycle of the OSGi Framework.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 11-Sep-2010
 */
public class FrameworkService implements Service<Framework> {

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("osgi", "framework");

    private static final Logger log = Logger.getLogger("org.jboss.as.osgi");

    private final InjectedValue<ServerEnvironment> injectedEnvironment = new InjectedValue<ServerEnvironment>();
    private final InjectedValue<MBeanServer> injectedMBeanServer = new InjectedValue<MBeanServer>();
    private final InjectedValue<SocketBinding> httpServerPortBinding = new InjectedValue<SocketBinding>();
    private final SubsystemState subsystemState;
    private Framework framework;

    public static void addService(final ServiceTarget target, final SubsystemState subsystemState) {
        FrameworkService service = new FrameworkService(subsystemState);
        ServiceBuilder<?> serviceBuilder = target.addService(FrameworkService.SERVICE_NAME, service);
        serviceBuilder.addDependency(ServerEnvironmentService.SERVICE_NAME, ServerEnvironment.class, service.injectedEnvironment);
        serviceBuilder.addDependency(MBeanServerService.SERVICE_NAME, MBeanServer.class, service.injectedMBeanServer);
        serviceBuilder.addDependency(SocketBinding.JBOSS_BINDING_NAME.append("osgi-http"), SocketBinding.class, service.httpServerPortBinding);
        serviceBuilder.setInitialMode(Mode.ON_DEMAND);
        serviceBuilder.install();

        AutoInstallIntegration.addService(target, subsystemState);
        FrameworkModuleIntegration.addService(target, subsystemState);
        ModuleLoaderIntegration.addService(target);
    }

    private FrameworkService(SubsystemState subsystemState) {
        this.subsystemState = subsystemState;
    }

    public synchronized void start(StartContext context) throws StartException {
        log.infof("Starting OSGi Framework");
        try {
            ServiceContainer serviceContainer = context.getController().getServiceContainer();

            // Setup the OSGi {@link Framework} properties
            Map<String, Object> props = new HashMap<String, Object>(subsystemState.getProperties());
            setupIntegrationProperties(context, props);

            // Start the OSGi {@link Framework}
            FrameworkBuilder builder = new FrameworkBuilder(props);
            builder.setServiceContainer(serviceContainer);
            builder.addProvidedService(DeployerServiceProvider.SERVICE_NAME);
            builder.addProvidedService(FrameworkModuleProvider.SERVICE_NAME);
            builder.addProvidedService(ModuleLoaderProvider.SERVICE_NAME);
            framework = builder.build();
            framework.start();

            // Register the {@link MBeanServer} as OSGi service
            BundleContext systemContext = framework.getBundleContext();
            MBeanServer mbeanServer = injectedMBeanServer.getValue();
            systemContext.registerService(MBeanServer.class.getName(), mbeanServer, null);

            // Register the {@link ServiceContainer} as OSGi service
            systemContext.registerService(ServiceContainer.class.getName(), serviceContainer, null);

        } catch (Throwable t) {
            throw new StartException("Failed to start OSGi Framework: " + framework, t);
        }
    }

    public synchronized void stop(StopContext context) {
        log.infof("Stopping OSGi Framework");
        ServiceContainer serviceContainer = context.getController().getServiceContainer();
        ServiceController<?> controller = serviceContainer.getService(FrameworkExt.SERVICE_NAME);
        if (controller != null) {
            controller.setMode(Mode.REMOVE);
        }
    }

    @Override
    public Framework getValue() throws IllegalStateException {
        return framework;
    }

    private void setupIntegrationProperties(StartContext context, Map<String, Object> props) {

        // Configure the OSGi HttpService port
        // [TODO] This will go away once the HTTP subsystem from AS implements the OSGi HttpService.
        props.put("org.osgi.service.http.port", "" + httpServerPortBinding.getValue().getSocketAddress().getPort());

        // Setup the Framework's storage area. Always clean the framework storage on first init.
        // [TODO] Differentiate beetween user data and persisted bundles. Persist bundle state in the domain model.
        props.put(Constants.FRAMEWORK_STORAGE_CLEAN, Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT);
        String storage = (String) props.get(Constants.FRAMEWORK_STORAGE);
        if (storage == null) {
            ServerEnvironment environment = injectedEnvironment.getValue();
            File dataDir = environment.getServerDataDir();
            storage = dataDir.getAbsolutePath() + File.separator + "osgi-store";
            props.put(Constants.FRAMEWORK_STORAGE, storage);
        }
    }

    private static class ModuleLoaderIntegration extends AbstractService<ModuleLoader> implements ModuleLoaderProvider {

        private final InjectedValue<ServiceModuleLoader> injectedModuleLoader = new InjectedValue<ServiceModuleLoader>();

        private static void addService(final ServiceTarget target) {
            ModuleLoaderIntegration service = new ModuleLoaderIntegration();
            ServiceBuilder<?> serviceBuilder = target.addService(ModuleLoaderProvider.SERVICE_NAME, service);
            serviceBuilder.addDependency(Services.JBOSS_SERVICE_MODULE_LOADER, ServiceModuleLoader.class, service.injectedModuleLoader);
            serviceBuilder.setInitialMode(Mode.ON_DEMAND);
            serviceBuilder.install();
        }

        private ModuleLoaderIntegration() {
        }

        @Override
        public ModuleLoader getValue() throws IllegalStateException {
            return injectedModuleLoader.getValue();
        }
    }

    private static class FrameworkModuleIntegration implements FrameworkModuleProvider {

        private final InjectedValue<Bundle> injectedSystemBundle = new InjectedValue<Bundle>();
        private final InjectedValue<Module> injectedSystemModule = new InjectedValue<Module>();
        private final SubsystemState subsystemState;
        private Module frameworkModule;

        private static void addService(final ServiceTarget target, final SubsystemState subsystemState) {
            FrameworkModuleIntegration service = new FrameworkModuleIntegration(subsystemState);
            ServiceBuilder<?> serviceBuilder = target.addService(FrameworkModuleProvider.SERVICE_NAME, service);
            serviceBuilder.addDependency(SystemBundleProvider.SERVICE_NAME, Bundle.class, service.injectedSystemBundle);
            serviceBuilder.addDependency(SystemModuleProvider.SERVICE_NAME, Module.class, service.injectedSystemModule);
            serviceBuilder.setInitialMode(Mode.PASSIVE);
            serviceBuilder.install();
        }

        private FrameworkModuleIntegration(SubsystemState subsystemState) {
            this.subsystemState = subsystemState;
        }

        @Override
        public void start(StartContext context) throws StartException {
            frameworkModule = createFrameworkModule();
        }

        @Override
        public void stop(StopContext context) {
            frameworkModule = null;
        }

        @Override
        public Module getValue() throws IllegalStateException {
            return frameworkModule;
        }

        private Module createFrameworkModule() {
            // Setup the extended framework module spec
            Module systemModule = injectedSystemModule.getValue();
            ModuleIdentifier systemIdentifier = systemModule.getIdentifier();
            ModuleLoader systemLoader = systemModule.getModuleLoader();
            ModuleSpec.Builder specBuilder = ModuleSpec.build(ModuleIdentifier.create(JBOSGI_PREFIX + ".framework"));
            specBuilder
                    .addDependency(DependencySpec.createModuleDependencySpec(PathFilters.acceptAll(), PathFilters.acceptAll(), systemLoader, systemIdentifier, false));

            // Add a dependency on the default framework module
            ModuleLoader bootLoader = Module.getBootModuleLoader();
            ModuleIdentifier frameworkIdentifier = ModuleIdentifier.create("org.jboss.osgi.framework");
            DependencySpec moduleDep = DependencySpec.createModuleDependencySpec(PathFilters.acceptAll(), PathFilters.acceptAll(), bootLoader, frameworkIdentifier,
                    false);
            specBuilder.addDependency(moduleDep);

            // Add the user defined module dependencies
            String modulesProps = (String) subsystemState.getProperties().get(PROP_JBOSS_OSGI_SYSTEM_MODULES);
            if (modulesProps != null) {
                for (String moduleProp : modulesProps.split(",")) {
                    ModuleIdentifier moduleId = ModuleIdentifier.create(moduleProp.trim());
                    moduleDep = DependencySpec.createModuleDependencySpec(PathFilters.acceptAll(), PathFilters.acceptAll(), bootLoader, moduleId, false);
                    specBuilder.addDependency(moduleDep);
                }
            }

            Bundle systemBundle = injectedSystemBundle.getValue();
            specBuilder.setModuleClassLoaderFactory(new BundleReferenceClassLoader.Factory(systemBundle));

            try {
                final ModuleSpec moduleSpec = specBuilder.create();
                ModuleLoader moduleLoader = new ModuleLoader() {

                    @Override
                    protected ModuleSpec findModule(ModuleIdentifier identifier) throws ModuleLoadException {
                        return (moduleSpec.getModuleIdentifier().equals(identifier) ? moduleSpec : null);
                    }

                    @Override
                    public String toString() {
                        return getClass().getSimpleName();
                    }
                };
                return moduleLoader.loadModule(specBuilder.getIdentifier());
            } catch (ModuleLoadException ex) {
                throw new IllegalStateException(ex);
            }
        }
    }

    private static class AutoInstallIntegration extends AbstractService<Void> {

        private final InjectedValue<BundleContext> injectedBundleContext = new InjectedValue<BundleContext>();
        private final InjectedValue<FrameworkExt> injectedBundleManager = new InjectedValue<FrameworkExt>();
        private SubsystemState subsystemState;

        private static void addService(final ServiceTarget target, final SubsystemState subsystemState) {
            AutoInstallIntegration service = new AutoInstallIntegration(subsystemState);
            ServiceBuilder<?> serviceBuilder = target.addService(SERVICE_NAME.append("autoinstall"), service);
            serviceBuilder.addDependency(BundleContextService.SERVICE_NAME, BundleContext.class, service.injectedBundleContext);
            serviceBuilder.addDependency(FrameworkExt.SERVICE_NAME, FrameworkExt.class, service.injectedBundleManager);
            serviceBuilder.setInitialMode(Mode.PASSIVE);
            serviceBuilder.install();
        }

        private AutoInstallIntegration(SubsystemState subsystemState) {
            this.subsystemState = subsystemState;
        }

        @Override
        public void start(StartContext context) throws StartException {

            try {
                // Create the list of {@link Deployment}s for the configured modules
                List<Deployment> deployments = new ArrayList<Deployment>();
                for (OSGiModule moduleMetaData : subsystemState.getModules()) {
                    ModuleIdentifier identifier = moduleMetaData.getIdentifier();
                    Deployment dep = createDeployment(identifier);
                    dep.setAutoStart(moduleMetaData.isStart());
                    deployments.add(dep);
                }

                // Deploy the bundles through the {@link SystemDeployerService}
                // [TODO] Revisit whether these deployments should go through the {@link DeploymentUnitProcessor} chain
                final BundleContext systemContext = injectedBundleContext.getValue();
                final FrameworkExt bundleManager = injectedBundleManager.getValue();
                DeployerService service = new SystemDeployerService(systemContext) {
                    @Override
                    protected Bundle installBundle(Deployment dep) throws BundleException {
                        return bundleManager.installBundle(dep);
                    }
                };
                service.deploy(deployments.toArray(new Deployment[deployments.size()]));
            } catch (BundleException ex) {
                throw new StartException("Failed to auto install bundles", ex);
            }
        }

        /**
         * Create a {@link Deployment} from the given ModuleIdentifier.
         */
        private Deployment createDeployment(final ModuleIdentifier identifier) throws BundleException {

            String location = "module:" + identifier.getName();
            if ("main".equals(identifier.getSlot()) == false)
                location += ":" + identifier.getSlot();

            // Check if we have a single root file
            File repoFile = getModuleRepositoryEntry(identifier);
            if (repoFile == null)
                throw new IllegalArgumentException("Cannot obtain repository entry for: " + identifier);

            // Try to process this as a valid OSGi bundle
            try {
                VirtualFile rootFile = AbstractVFS.toVirtualFile(repoFile.toURI());
                BundleInfo info = BundleInfo.createBundleInfo(rootFile, location);
                return DeploymentFactory.createDeployment(info);
            } catch (IOException ex) {
                throw new BundleException("Cannot create deployment for: " + identifier, ex);
            }
        }

        /**
         * Get file for the singe jar that corresponds to the given identifier
         */
        private File getModuleRepositoryEntry(ModuleIdentifier identifier) {
            File rootPath = new File(System.getProperty("module.path"));
            String identifierPath = identifier.getName().replace('.', '/') + "/" + identifier.getSlot();
            File moduleDir = new File(rootPath + "/" + identifierPath);
            if (moduleDir.isDirectory() == false) {
                log.warnf("Cannot obtain module directory: %s", moduleDir);
                return null;
            }

            String[] files = moduleDir.list(new FilenameFilter() {

                @Override
                public boolean accept(File dir, String name) {
                    return name.endsWith(".jar");
                }
            });
            if (files.length == 0) {
                log.warnf("Cannot find module jar in: %s", moduleDir);
                return null;
            }
            if (files.length > 1) {
                log.warnf("Multiple module jars in: %s", moduleDir);
                return null;
            }

            File moduleFile = new File(moduleDir + "/" + files[0]);
            if (moduleFile.exists() == false) {
                log.warnf("Module file does not exist: %s", moduleFile);
                return null;
            }

            return moduleFile;
        }
    }
}

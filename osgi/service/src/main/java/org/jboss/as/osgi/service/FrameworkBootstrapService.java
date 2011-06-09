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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import static org.jboss.as.osgi.parser.SubsystemState.PROP_JBOSS_OSGI_SYSTEM_MODULES;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceListener;
import static org.jboss.osgi.framework.Constants.JBOSGI_PREFIX;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanServer;

import org.jboss.as.jmx.MBeanServerService;
import org.jboss.as.network.SocketBinding;
import org.jboss.as.osgi.parser.SubsystemState;
import org.jboss.as.osgi.parser.SubsystemState.Activation;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.ServerEnvironmentService;
import org.jboss.logging.Logger;
import org.jboss.modules.DependencySpec;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.jboss.modules.ModuleSpec;
import org.jboss.modules.filter.PathFilter;
import org.jboss.modules.filter.PathFilters;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.osgi.framework.FrameworkModuleProvider;
import org.jboss.osgi.framework.Services;
import org.jboss.osgi.framework.SystemServicesProvider;
import org.jboss.osgi.framework.internal.FrameworkBuilder;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

/**
 * Service responsible for creating and managing the life-cycle of the OSGi Framework.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 11-Sep-2010
 */
public class FrameworkBootstrapService implements Service<Void> {

    public static final ServiceName SERVICE_BASE_NAME = ServiceName.JBOSS.append("osgi", "as");
    public static final ServiceName FRAMEWORK_BASE_NAME = SERVICE_BASE_NAME.append("framework");
    public static final ServiceName FRAMEWORK_BOOTSTRAP = FRAMEWORK_BASE_NAME.append("bootstrap");

    // Provide logging
    private static final Logger log = Logger.getLogger("org.jboss.as.osgi");

    private final InjectedValue<ServerEnvironment> injectedEnvironment = new InjectedValue<ServerEnvironment>();
    private final InjectedValue<SocketBinding> httpServerPortBinding = new InjectedValue<SocketBinding>();
    private final SubsystemState subsystemState;

    public static Collection<ServiceController<?>> addService(final ServiceTarget target, final SubsystemState subsystemState, final ServiceListener<Object>... listeners) {
        final List<ServiceController<?>> controllers = new ArrayList<ServiceController<?>>();
        FrameworkBootstrapService service = new FrameworkBootstrapService(subsystemState);
        ServiceBuilder<?> builder = target.addService(FRAMEWORK_BOOTSTRAP, service);
        builder.addDependency(ServerEnvironmentService.SERVICE_NAME, ServerEnvironment.class, service.injectedEnvironment);
        builder.addDependency(SocketBinding.JBOSS_BINDING_NAME.append("osgi-http"), SocketBinding.class, service.httpServerPortBinding);
        builder.addListener(listeners);
        controllers.add(builder.install());

        controllers.add(AutoInstallIntegration.addService(target, subsystemState));
        controllers.add(FrameworkModuleIntegration.addService(target, subsystemState));
        controllers.add(ModuleLoaderIntegration.addService(target));
        controllers.add(SystemServicesIntegration.addService(target));
        return controllers;
    }

    private FrameworkBootstrapService(SubsystemState subsystemState) {
        this.subsystemState = subsystemState;
    }

    public synchronized void start(StartContext context) throws StartException {
        ServiceController<?> controller = context.getController();
        log.debugf("Starting: %s in mode %s", controller.getName(), controller.getMode());
        try {
            ServiceContainer serviceContainer = context.getController().getServiceContainer();

            // Setup the OSGi {@link Framework} properties
            Map<String, Object> props = new HashMap<String, Object>(subsystemState.getProperties());
            setupIntegrationProperties(context, props);

            // Register the URLStreamHandlerFactory
            Module coreFrameworkModule = ((ModuleClassLoader) FrameworkBuilder.class.getClassLoader()).getModule();
            Module.registerURLStreamHandlerFactoryModule(coreFrameworkModule);
            Module.registerContentHandlerFactoryModule(coreFrameworkModule);

            // Configure the {@link Framework} builder
            FrameworkBuilder builder = new FrameworkBuilder(props);
            builder.setServiceContainer(serviceContainer);
            builder.setServiceTarget(context.getChildTarget());
            builder.addProvidedService(Services.AUTOINSTALL_PROVIDER);
            builder.addProvidedService(Services.BUNDLE_INSTALL_PROVIDER);
            builder.addProvidedService(Services.FRAMEWORK_MODULE_PROVIDER);
            builder.addProvidedService(Services.MODULE_LOADER_PROVIDER);
            builder.addProvidedService(Services.SYSTEM_SERVICES_PROVIDER);

            // Create the {@link Framework} services
            Activation activation = subsystemState.getActivationPolicy();
            Mode initialMode = (activation == Activation.EAGER ? Mode.ACTIVE : Mode.ON_DEMAND);
            builder.createFrameworkServices(initialMode, true);
        } catch (Throwable t) {
            throw new StartException("Failed to create Framework services", t);
        }
    }

    public synchronized void stop(StopContext context) {
        ServiceController<?> controller = context.getController();
        log.debugf("Stopping: %s in mode %s", controller.getName(), controller.getMode());
        log.infof("Stopping OSGi Framework");
    }

    @Override
    public Void getValue() throws IllegalStateException {
        return null;
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

    private static final class SystemServicesIntegration implements Service<SystemServicesProvider>, SystemServicesProvider {

        private final InjectedValue<MBeanServer> injectedMBeanServer = new InjectedValue<MBeanServer>();
        private ServiceContainer serviceContainer;

        public static ServiceController<?> addService(final ServiceTarget target) {
            SystemServicesIntegration service = new SystemServicesIntegration();
            ServiceBuilder<SystemServicesProvider> builder = target.addService(Services.SYSTEM_SERVICES_PROVIDER, service);
            builder.addDependency(MBeanServerService.SERVICE_NAME, MBeanServer.class, service.injectedMBeanServer);
            builder.addDependency(Services.FRAMEWORK_CREATE);
            builder.setInitialMode(Mode.ON_DEMAND);
            return builder.install();
        }

        private SystemServicesIntegration() {
        }

        @Override
        public void start(StartContext context) throws StartException {
            ServiceController<?> controller = context.getController();
            log.debugf("Starting: %s in mode %s", controller.getName(), controller.getMode());
            serviceContainer = context.getController().getServiceContainer();
        }

        @Override
        public void stop(StopContext context) {
            ServiceController<?> controller = context.getController();
            log.debugf("Stopping: %s in mode %s", controller.getName(), controller.getMode());
        }

        @Override
        public SystemServicesProvider getValue() {
            return this;
        }

        @Override
        public void registerSystemServices(BundleContext systemContext) {

            // Register the {@link MBeanServer} as OSGi service
            MBeanServer mbeanServer = injectedMBeanServer.getValue();
            systemContext.registerService(MBeanServer.class.getName(), mbeanServer, null);

            // Register the {@link ServiceContainer} as OSGi service
            systemContext.registerService(ServiceContainer.class.getName(), serviceContainer, null);
        }
    }

    private static final class FrameworkModuleIntegration implements FrameworkModuleProvider {

        private final InjectedValue<Module> injectedSystemModule = new InjectedValue<Module>();
        private final SubsystemState subsystemState;
        private Module frameworkModule;

        private static ServiceController<?> addService(final ServiceTarget target, final SubsystemState subsystemState) {
            FrameworkModuleIntegration service = new FrameworkModuleIntegration(subsystemState);
            ServiceBuilder<?> builder = target.addService(Services.FRAMEWORK_MODULE_PROVIDER, service);
            builder.addDependency(Services.SYSTEM_MODULE_PROVIDER, Module.class, service.injectedSystemModule);
            builder.setInitialMode(Mode.ON_DEMAND);
            return builder.install();
        }

        private FrameworkModuleIntegration(SubsystemState subsystemState) {
            this.subsystemState = subsystemState;
        }

        @Override
        public void start(StartContext context) throws StartException {
            ServiceController<?> controller = context.getController();
            log.debugf("Starting: %s in mode %s", controller.getName(), controller.getMode());
        }

        @Override
        public void stop(StopContext context) {
            ServiceController<?> controller = context.getController();
            log.debugf("Stopping: %s in mode %s", controller.getName(), controller.getMode());
            frameworkModule = null;
        }

        @Override
        public FrameworkModuleProvider getValue() throws IllegalStateException {
            return this;
        }

        @Override
        public Module getFrameworkModule(Bundle systemBundle) {
            if (frameworkModule == null) {
                frameworkModule = createFrameworkModule(systemBundle);
            }
            return frameworkModule;
        }

        private Module createFrameworkModule(final Bundle systemBundle) {
            // Setup the extended framework module spec
            Module systemModule = injectedSystemModule.getValue();
            ModuleIdentifier systemIdentifier = systemModule.getIdentifier();
            ModuleLoader systemLoader = systemModule.getModuleLoader();
            ModuleSpec.Builder specBuilder = ModuleSpec.build(ModuleIdentifier.create(JBOSGI_PREFIX + ".framework"));
            PathFilter acceptAll = PathFilters.acceptAll();
            specBuilder.addDependency(DependencySpec.createModuleDependencySpec(acceptAll, acceptAll, systemLoader, systemIdentifier, false));

            // Add a dependency on the default framework module
            ModuleLoader bootLoader = Module.getBootModuleLoader();
            ModuleIdentifier frameworkIdentifier = ModuleIdentifier.create("org.jboss.osgi.framework");
            specBuilder.addDependency(DependencySpec.createModuleDependencySpec(acceptAll, acceptAll, bootLoader, frameworkIdentifier, false));

            // Add the user defined module dependencies
            String modulesProps = (String) subsystemState.getProperties().get(PROP_JBOSS_OSGI_SYSTEM_MODULES);
            if (modulesProps != null) {
                for (String moduleProp : modulesProps.split(",")) {
                    moduleProp = moduleProp.trim();
                    if (moduleProp.length() > 0) {
                        ModuleIdentifier moduleId = ModuleIdentifier.create(moduleProp);
                        DependencySpec moduleDep = DependencySpec.createModuleDependencySpec(acceptAll, acceptAll, bootLoader, moduleId, false);
                        specBuilder.addDependency(moduleDep);
                    }
                }
            }

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
}

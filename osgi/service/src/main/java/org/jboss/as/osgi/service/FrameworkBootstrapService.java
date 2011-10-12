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

import static org.jboss.as.osgi.OSGiLogger.ROOT_LOGGER;
import static org.jboss.as.osgi.OSGiMessages.MESSAGES;
import static org.jboss.as.osgi.parser.SubsystemState.PROP_JBOSS_OSGI_SYSTEM_MODULES;
import static org.jboss.as.osgi.parser.SubsystemState.PROP_JBOSS_OSGI_SYSTEM_MODULES_EXTRA;
import static org.jboss.as.osgi.parser.SubsystemState.PROP_JBOSS_OSGI_SYSTEM_PACKAGES;
import static org.jboss.osgi.framework.Constants.JBOSGI_PREFIX;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.MBeanServer;

import org.jboss.as.jmx.MBeanServerService;
import org.jboss.as.network.SocketBinding;
import org.jboss.as.osgi.parser.SubsystemState;
import org.jboss.as.osgi.parser.SubsystemState.Activation;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.ServerEnvironmentService;
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
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceListener;
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
 * @author David Bosschaert
 * @since 11-Sep-2010
 */
public class FrameworkBootstrapService implements Service<Void> {

    public static final ServiceName SERVICE_BASE_NAME = ServiceName.JBOSS.append("osgi", "as");
    public static final ServiceName FRAMEWORK_BASE_NAME = SERVICE_BASE_NAME.append("framework");
    public static final ServiceName FRAMEWORK_BOOTSTRAP = FRAMEWORK_BASE_NAME.append("bootstrap");

    private final InjectedValue<ServerEnvironment> injectedEnvironment = new InjectedValue<ServerEnvironment>();
    private final InjectedValue<SubsystemState> injectedSubsystemState = new InjectedValue<SubsystemState>();
    private final InjectedValue<SocketBinding> httpServerPortBinding = new InjectedValue<SocketBinding>();

    public static ServiceController<?> addService(final ServiceTarget target, final ServiceListener<Object>... listeners) {
        final List<ServiceController<?>> controllers = new ArrayList<ServiceController<?>>();
        FrameworkBootstrapService service = new FrameworkBootstrapService();
        ServiceBuilder<?> builder = target.addService(FRAMEWORK_BOOTSTRAP, service);
        builder.addDependency(ServerEnvironmentService.SERVICE_NAME, ServerEnvironment.class, service.injectedEnvironment);
        builder.addDependency(SubsystemState.SERVICE_NAME, SubsystemState.class, service.injectedSubsystemState);
        builder.addDependency(SocketBinding.JBOSS_BINDING_NAME.append("osgi-http"), SocketBinding.class, service.httpServerPortBinding);
        builder.addListener(listeners);
        return builder.install();
    }

    private FrameworkBootstrapService() {
    }

    public synchronized void start(StartContext context) throws StartException {
        ServiceController<?> controller = context.getController();
        ROOT_LOGGER.debugf("Starting: %s in mode %s", controller.getName(), controller.getMode());
        try {
            ServiceContainer serviceContainer = context.getController().getServiceContainer();

            // Setup the OSGi {@link Framework} properties
            SubsystemState subsystemState = injectedSubsystemState.getValue();
            Map<String, Object> props = new HashMap<String, Object>(subsystemState.getProperties());
            setupIntegrationProperties(context, props);

            // Register the URLStreamHandlerFactory
            Module coreFrameworkModule = ((ModuleClassLoader) FrameworkBuilder.class.getClassLoader()).getModule();
            Module.registerURLStreamHandlerFactoryModule(coreFrameworkModule);
            Module.registerContentHandlerFactoryModule(coreFrameworkModule);

            ServiceTarget target = context.getChildTarget();
            AutoInstallIntegration.addService(target);
            FrameworkModuleIntegration.addService(target, props);
            ModuleLoaderIntegration.addService(target);
            SystemServicesIntegration.addService(target);

            // Configure the {@link Framework} builder
            FrameworkBuilder builder = new FrameworkBuilder(props);
            builder.setServiceContainer(serviceContainer);
            builder.setServiceTarget(target);
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
            throw new StartException(MESSAGES.failedToCreateFrameworkServices(), t);
        }
    }

    public synchronized void stop(StopContext context) {
        ServiceController<?> controller = context.getController();
        ROOT_LOGGER.debugf("Stopping: %s in mode %s", controller.getName(), controller.getMode());
        ROOT_LOGGER.stoppingOsgiFramework();
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

        // Setup default system modules
        String sysmodules = (String) props.get(PROP_JBOSS_OSGI_SYSTEM_MODULES);
        if (sysmodules == null) {
            StringBuffer buffer = new StringBuffer();
            buffer.append("org.apache.commons.logging,");
            buffer.append("org.apache.log4j,");
            buffer.append("org.jboss.as.osgi,");
            buffer.append("org.jboss.logging,");
            buffer.append("org.jboss.osgi.framework,");
            buffer.append("org.slf4j");
            props.put(PROP_JBOSS_OSGI_SYSTEM_MODULES, buffer.toString());
        }

        // Setup default system packages
        String syspackages = (String) props.get(PROP_JBOSS_OSGI_SYSTEM_PACKAGES);
        if (syspackages == null) {
            StringBuffer buffer = new StringBuffer();
            buffer.append("org.apache.commons.logging;version=1.1.1,");
            buffer.append("org.apache.log4j;version=1.2,");
            buffer.append("org.jboss.as.osgi.service;version=7.0,");
            buffer.append("org.jboss.logging;version=3.0.0,");
            buffer.append("org.jboss.osgi.deployment.interceptor;version=1.0,");
            buffer.append("org.jboss.osgi.spi.capability;version=1.0,");
            buffer.append("org.jboss.osgi.spi.util;version=1.0,");
            buffer.append("org.jboss.osgi.testing;version=1.0,");
            buffer.append("org.jboss.osgi.vfs;version=1.0,");
            buffer.append("org.slf4j;version=1.5.10,");
            syspackages = buffer.toString();
            props.put(PROP_JBOSS_OSGI_SYSTEM_PACKAGES, syspackages);
        }

        String extrapackages = (String) props.get(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA);
        if (extrapackages == null) {
            extrapackages = syspackages;
        } else {
            extrapackages += "," + syspackages;
        }
        props.put(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, extrapackages);
    }

    private static final class SystemServicesIntegration implements Service<SystemServicesProvider>, SystemServicesProvider {

        private final InjectedValue<MBeanServer> injectedMBeanServer = new InjectedValue<MBeanServer>();
        private final InjectedValue<BundleContext> injectedBundleContext = new InjectedValue<BundleContext>();
        private ServiceContainer serviceContainer;

        public static ServiceController<?> addService(final ServiceTarget target) {
            SystemServicesIntegration service = new SystemServicesIntegration();
            ServiceBuilder<SystemServicesProvider> builder = target.addService(Services.SYSTEM_SERVICES_PROVIDER, service);
            builder.addDependency(MBeanServerService.SERVICE_NAME, MBeanServer.class, service.injectedMBeanServer);
            builder.addDependency(Services.SYSTEM_CONTEXT, BundleContext.class, service.injectedBundleContext);
            builder.addDependency(Services.FRAMEWORK_CREATE);
            builder.setInitialMode(Mode.ON_DEMAND);
            return builder.install();
        }

        private SystemServicesIntegration() {
        }

        @Override
        public void start(StartContext context) throws StartException {
            ServiceController<?> controller = context.getController();
            ROOT_LOGGER.debugf("Starting: %s in mode %s", controller.getName(), controller.getMode());
            serviceContainer = context.getController().getServiceContainer();
        }

        @Override
        public void stop(StopContext context) {
            ServiceController<?> controller = context.getController();
            ROOT_LOGGER.debugf("Stopping: %s in mode %s", controller.getName(), controller.getMode());
        }

        @Override
        public SystemServicesProvider getValue() {
            return this;
        }

        @Override
        public void registerSystemServices(final BundleContext context) {

            // Register the {@link MBeanServer} as OSGi service
            MBeanServer mbeanServer = injectedMBeanServer.getValue();
            context.registerService(MBeanServer.class.getName(), mbeanServer, null);

            // Register the {@link ServiceContainer} as OSGi service
            context.registerService(ServiceContainer.class.getName(), serviceContainer, null);
        }
    }

    private static final class FrameworkModuleIntegration implements FrameworkModuleProvider {

        private final InjectedValue<Module> injectedSystemModule = new InjectedValue<Module>();
        private final Map<String, Object> props;
        private Module frameworkModule;

        private static ServiceController<?> addService(final ServiceTarget target, Map<String, Object> props) {
            FrameworkModuleIntegration service = new FrameworkModuleIntegration(props);
            ServiceBuilder<?> builder = target.addService(Services.FRAMEWORK_MODULE_PROVIDER, service);
            builder.addDependency(Services.SYSTEM_MODULE_PROVIDER, Module.class, service.injectedSystemModule);
            builder.setInitialMode(Mode.ON_DEMAND);
            return builder.install();
        }

        private FrameworkModuleIntegration(Map<String, Object> props) {
            this.props = props;
        }

        @Override
        public void start(StartContext context) throws StartException {
            ServiceController<?> controller = context.getController();
            ROOT_LOGGER.debugf("Starting: %s in mode %s", controller.getName(), controller.getMode());
        }

        @Override
        public void stop(StopContext context) {
            ServiceController<?> controller = context.getController();
            ROOT_LOGGER.debugf("Stopping: %s in mode %s", controller.getName(), controller.getMode());
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
            specBuilder.addDependency(createSystemModuleDependency(systemLoader, systemIdentifier));

            // Add the framework module dependencies
            String sysmodules = (String) props.get(PROP_JBOSS_OSGI_SYSTEM_MODULES);
            if (sysmodules == null)
                sysmodules = "";

            String extramodules = (String) props.get(PROP_JBOSS_OSGI_SYSTEM_MODULES_EXTRA);
            if (extramodules != null)
                sysmodules += "," + extramodules;

            ModuleLoader bootLoader = Module.getBootModuleLoader();
            for (String moduleProp : sysmodules.split(",")) {
                moduleProp = moduleProp.trim();
                if (moduleProp.length() > 0) {
                    ModuleIdentifier moduleId = ModuleIdentifier.create(moduleProp);
                    DependencySpec moduleDep = createSystemModuleDependency(bootLoader, moduleId);
                    specBuilder.addDependency(moduleDep);
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

        private DependencySpec createSystemModuleDependency(ModuleLoader moduleLoader, ModuleIdentifier identifier) {
            PathFilter acceptAll = PathFilters.acceptAll();
            return DependencySpec.createModuleDependencySpec(acceptAll, acceptAll, moduleLoader, identifier, false);
        }
    }
}

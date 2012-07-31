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

import static org.jboss.as.network.SocketBinding.JBOSS_BINDING_NAME;
import static org.jboss.as.osgi.OSGiConstants.SERVICE_BASE_NAME;
import static org.jboss.as.osgi.OSGiLogger.LOGGER;
import static org.jboss.as.osgi.OSGiMessages.MESSAGES;
import static org.jboss.as.osgi.parser.SubsystemState.PROP_JBOSS_OSGI_SYSTEM_MODULES;
import static org.jboss.as.osgi.parser.SubsystemState.PROP_JBOSS_OSGI_SYSTEM_MODULES_EXTRA;
import static org.jboss.as.osgi.parser.SubsystemState.PROP_JBOSS_OSGI_SYSTEM_PACKAGES;
import static org.jboss.as.server.Services.JBOSS_SERVER_CONTROLLER;
import static org.jboss.osgi.framework.Constants.JBOSGI_PREFIX;
import static org.jboss.osgi.repository.XRepository.MODULE_IDENTITY_NAMESPACE;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.network.SocketBinding;
import org.jboss.as.osgi.SubsystemExtension;
import org.jboss.as.osgi.management.OSGiRuntimeResource;
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
import org.jboss.modules.log.ModuleLogger;
import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceListener.Inheritance;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.osgi.framework.BundleManager;
import org.jboss.osgi.framework.FrameworkModulePlugin;
import org.jboss.osgi.framework.IntegrationServices;
import org.jboss.osgi.framework.Services;
import org.jboss.osgi.framework.SystemPathsPlugin;
import org.jboss.osgi.framework.SystemServicesPlugin;
import org.jboss.osgi.framework.internal.FrameworkBuilder;
import org.jboss.osgi.repository.RepositoryStorage;
import org.jboss.osgi.repository.RepositoryStorageException;
import org.jboss.osgi.repository.RepositoryStorageFactory;
import org.jboss.osgi.repository.XRepository;
import org.jboss.osgi.repository.XRepositoryBuilder;
import org.jboss.osgi.repository.core.FileBasedRepositoryStorage;
import org.jboss.osgi.resolver.XResource;
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

    static final ServiceName FRAMEWORK_BOOTSTRAP_NAME = SERVICE_BASE_NAME.append("framework", "bootstrap");
    static final String MAPPED_OSGI_SOCKET_BINDINGS = "org.jboss.as.osgi.socket.bindings";

    private final InjectedValue<ServerEnvironment> injectedServerEnvironment = new InjectedValue<ServerEnvironment>();
    private final InjectedValue<SubsystemState> injectedSubsystemState = new InjectedValue<SubsystemState>();
    private final ServiceVerificationHandler verificationHandler;
    private final List<SubsystemExtension> extensions;
    private final OSGiRuntimeResource resource;

    public static ServiceController<Void> addService(ServiceTarget target, OSGiRuntimeResource resource, List<SubsystemExtension> extensions, ServiceVerificationHandler verificationHandler) {
        FrameworkBootstrapService service = new FrameworkBootstrapService(resource, extensions, verificationHandler);
        ServiceBuilder<Void> builder = target.addService(FRAMEWORK_BOOTSTRAP_NAME, service);
        builder.addDependency(ServerEnvironmentService.SERVICE_NAME, ServerEnvironment.class, service.injectedServerEnvironment);
        builder.addDependency(SubsystemState.SERVICE_NAME, SubsystemState.class, service.injectedSubsystemState);
        builder.addListener(Inheritance.ONCE, verificationHandler);
        return builder.install();
    }

    private FrameworkBootstrapService(OSGiRuntimeResource resource, List<SubsystemExtension> extensions, ServiceVerificationHandler verificationHandler) {
        this.verificationHandler = verificationHandler;
        this.extensions = extensions;
        this.resource = resource;
    }

    @Override
    public synchronized void start(StartContext context) throws StartException {
        ServiceController<?> controller = context.getController();
        LOGGER.tracef("Starting: %s in mode %s", controller.getName(), controller.getMode());
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

            ServiceTarget serviceTarget = context.getChildTarget();
            Activation activation = subsystemState.getActivationPolicy();
            FrameworkActivationService.create(serviceTarget, activation, verificationHandler);

            BundleInstallIntegration.addService(serviceTarget);
            FrameworkModuleIntegration.addService(serviceTarget, props);
            JAXPServiceProvider.addService(serviceTarget);
            ModuleLoaderIntegration.addService(serviceTarget);
            ResolverService.addService(serviceTarget);
            SystemServicesIntegration.addService(serviceTarget, resource, extensions);

            // Configure the {@link Framework} builder
            FrameworkBuilder builder = new FrameworkBuilder(props);
            builder.setServiceContainer(serviceContainer);
            builder.setServiceTarget(serviceTarget);

            // Create the {@link Framework} services
            builder.createFrameworkServices(true);

        } catch (Throwable th) {
            throw MESSAGES.startFailedToCreateFrameworkServices(th);
        }
    }

    @Override
    public synchronized void stop(StopContext context) {
        ServiceController<?> controller = context.getController();
        LOGGER.tracef("Stopping: %s in mode %s", controller.getName(), controller.getMode());
    }

    @Override
    public Void getValue() throws IllegalStateException {
        return null;
    }

    private void setupIntegrationProperties(StartContext context, Map<String, Object> props) {

        // Setup the Framework's storage area.
        String storage = (String) props.get(Constants.FRAMEWORK_STORAGE);
        if (storage == null) {
            ServerEnvironment environment = injectedServerEnvironment.getValue();
            File dataDir = environment.getServerDataDir();
            storage = dataDir.getAbsolutePath() + File.separator + "osgi-store";
            props.put(Constants.FRAMEWORK_STORAGE, storage);
        }

        // Provide the ModuleLogger
        ModuleLogger moduleLogger = Module.getModuleLogger();
        if (moduleLogger != null)
            props.put(ModuleLogger.class.getName(), moduleLogger.getClass().getName());

        // Setup default system modules
        String sysmodules = (String) props.get(PROP_JBOSS_OSGI_SYSTEM_MODULES);
        if (sysmodules == null) {
            StringBuffer buffer = new StringBuffer();
            buffer.append("javax.api,");
            buffer.append("javax.inject.api,");
            buffer.append("org.apache.xerces,");
            buffer.append("org.jboss.as.configadmin,");
            buffer.append("org.jboss.as.controller-client,");
            buffer.append("org.jboss.as.osgi,");
            buffer.append("org.jboss.logging,");
            buffer.append("org.jboss.modules,");
            buffer.append("org.jboss.msc,");
            buffer.append("org.jboss.osgi.framework,");
            buffer.append("org.jboss.osgi.repository,");
            buffer.append("org.slf4j");
            props.put(PROP_JBOSS_OSGI_SYSTEM_MODULES, buffer.toString());
        }

        // Setup default system packages
        String syspackages = (String) props.get(PROP_JBOSS_OSGI_SYSTEM_PACKAGES);
        if (syspackages == null) {
            Set<String> sysPackages = new LinkedHashSet<String>();
            sysPackages.addAll(Arrays.asList(SystemPathsPlugin.DEFAULT_SYSTEM_PACKAGES));
            sysPackages.addAll(Arrays.asList(SystemPathsPlugin.DEFAULT_FRAMEWORK_PACKAGES));
            sysPackages.add("javax.inject");
            sysPackages.add("org.apache.xerces.jaxp");
            sysPackages.add("org.jboss.as.configadmin.service");
            sysPackages.add("org.jboss.as.controller.client");
            sysPackages.add("org.jboss.as.controller.client.helpers");
            sysPackages.add("org.jboss.as.controller.client.helpers.domain");
            sysPackages.add("org.jboss.as.controller.client.helpers.standalone");
            sysPackages.add("org.jboss.logging;version=3.1.0");
            sysPackages.add("org.jboss.osgi.repository;version=1.0");
            sysPackages.add("org.osgi.service.repository;version=1.0");
            sysPackages.add("org.slf4j;version=1.6.1");
            syspackages = sysPackages.toString();
            syspackages = syspackages.substring(1, syspackages.length() - 1);
            props.put(PROP_JBOSS_OSGI_SYSTEM_PACKAGES, syspackages);
        }

        String extrapackages = (String) props.get(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA);
        if (extrapackages != null) {
            syspackages += "," + extrapackages;
        }
        props.put(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, syspackages);
    }

    private static final class SystemServicesIntegration implements Service<SystemServicesPlugin>, SystemServicesPlugin {

        private final InjectedValue<ServerEnvironment> injectedServerEnvironment = new InjectedValue<ServerEnvironment>();
        private final InjectedValue<ModelController> injectedModelController = new InjectedValue<ModelController>();
        private final InjectedValue<BundleManager> injectedBundleManager = new InjectedValue<BundleManager>();
        private final InjectedValue<BundleContext> injectedBundleContext = new InjectedValue<BundleContext>();
        private final List<SubsystemExtension> extensions;
        private final OSGiRuntimeResource resource;
        private ServiceContainer serviceContainer;
        private ExecutorService controllerThreadExecutor;

        public static ServiceController<?> addService(final ServiceTarget target, OSGiRuntimeResource resource, List<SubsystemExtension> extensions) {
            SystemServicesIntegration service = new SystemServicesIntegration(resource, extensions);
            ServiceBuilder<SystemServicesPlugin> builder = target.addService(IntegrationServices.SYSTEM_SERVICES_PLUGIN, service);
            builder.addDependency(JBOSS_SERVER_CONTROLLER, ModelController.class, service.injectedModelController);
            builder.addDependency(ServerEnvironmentService.SERVICE_NAME, ServerEnvironment.class, service.injectedServerEnvironment);
            builder.addDependency(Services.BUNDLE_MANAGER, BundleManager.class, service.injectedBundleManager);
            builder.addDependency(Services.SYSTEM_CONTEXT, BundleContext.class, service.injectedBundleContext);
            builder.addDependency(Services.FRAMEWORK_CREATE);

            // Subsystem extension dependencies
            for(SubsystemExtension extension : extensions) {
                extension.configureSystemServiceDependencies(builder);
            }

            builder.setInitialMode(Mode.ON_DEMAND);
            return builder.install();
        }

        private SystemServicesIntegration(OSGiRuntimeResource resource, List<SubsystemExtension> extensions) {
            this.extensions = extensions;
            this.resource = resource;
        }

        @Override
        public void start(StartContext startContext) throws StartException {
            ServiceController<?> controller = startContext.getController();
            LOGGER.tracef("Starting: %s in mode %s", controller.getName(), controller.getMode());
            serviceContainer = startContext.getController().getServiceContainer();
            final BundleContext syscontext = injectedBundleContext.getValue();

            // Inject the system bundle context into the runtime resource
            BundleManager bundleManager = injectedBundleManager.getValue();
            resource.getInjectedBundleManager().inject(bundleManager);

            // Register the socket-binding services
            String bindingNames = syscontext.getProperty(MAPPED_OSGI_SOCKET_BINDINGS);
            if (bindingNames != null) {
                final Set<ServiceName> socketBindingNames = new HashSet<ServiceName>();
                for (String suffix : bindingNames.split(",")) {
                    socketBindingNames.add(JBOSS_BINDING_NAME.append(suffix));
                }
                ServiceTarget serviceTarget = startContext.getChildTarget();
                ServiceName serviceName = IntegrationServices.SYSTEM_SERVICES_PLUGIN.append("BINDINGS");
                ServiceBuilder<Void> builder = serviceTarget.addService(serviceName, new AbstractService<Void>() {
                    @Override
                    public void start(StartContext context) throws StartException {
                        for (ServiceName serviceName : socketBindingNames) {
                            SocketBinding binding = (SocketBinding) serviceContainer.getRequiredService(serviceName).getValue();
                            Dictionary<String, String> props = new Hashtable<String, String>();
                            props.put("socketBinding", serviceName.getSimpleName());
                            InetSocketAddress value = binding.getSocketAddress();
                            syscontext.registerService(InetSocketAddress.class.getName(), value, props);
                        }
                    }
                });
                ServiceName[] serviceNameArray = socketBindingNames.toArray(new ServiceName[socketBindingNames.size()]);
                builder.addDependencies(serviceNameArray);
                builder.install();
            }

            // The ExecutorService that is used by the ModelControllerClient service
            controllerThreadExecutor = Executors.newSingleThreadExecutor(new ThreadFactory() {
                @Override
                public Thread newThread(Runnable run) {
                    Thread thread = new Thread(run);
                    thread.setName("OSGi ModelControllerClient Thread");
                    thread.setDaemon(true);
                    return thread;
                }
            });

            // Register the {@link ModelControllerClient} service
            ModelController modelController = injectedModelController.getValue();
            ModelControllerClient client = modelController.createClient(controllerThreadExecutor);
            syscontext.registerService(ModelControllerClient.class.getName(), client, null);

            // Register the {@link XRepository} service
            final ServerEnvironment serverenv = injectedServerEnvironment.getValue();
            final File storageDir = new File(serverenv.getServerDataDir().getPath() + File.separator + "repository");
            RepositoryStorageFactory factory = new RepositoryStorageFactory() {
                @Override
                public RepositoryStorage create(XRepository repository) {
                    return new FileBasedRepositoryStorage(repository, storageDir) {
                        @Override
                        public XResource addResource(XResource res) throws RepositoryStorageException {
                            // Do not add modules to repository storage
                            if (res.getCapabilities(MODULE_IDENTITY_NAMESPACE).isEmpty()) {
                                return super.addResource(res);
                            } else {
                                return res;
                            }
                        }
                    };
                }
            };
            XRepositoryBuilder builder = XRepositoryBuilder.create(syscontext);
            builder.addRepository(new ModuleIdentityRepository(serverenv));
            builder.addRepositoryStorage(factory);
            builder.addDefaultRepositories();

            // Register the {@link ServiceContainer} as OSGi service
            syscontext.registerService(ServiceContainer.class.getName(), serviceContainer, null);

            // Perform subsystem extension start
            for(SubsystemExtension extension : extensions) {
                extension.startSystemServices(startContext, syscontext);
            }
        }

        @Override
        public void stop(StopContext context) {
            ServiceController<?> controller = context.getController();
            LOGGER.tracef("Stopping: %s in mode %s", controller.getName(), controller.getMode());

            // Perform subsystem extension stop
            BundleContext syscontext = injectedBundleContext.getValue();
            for(SubsystemExtension extension : extensions) {
                extension.stopSystemServices(context, syscontext);
            }

            resource.getInjectedBundleManager().uninject();
            controllerThreadExecutor.shutdown();
        }

        @Override
        public SystemServicesPlugin getValue() {
            return this;
        }

        @Override
        public void registerSystemServices(final BundleContext context) {
        }
    }

    private static final class FrameworkModuleIntegration implements FrameworkModulePlugin {

        private final Map<String, Object> props;
        private Module frameworkModule;

        private static ServiceController<?> addService(final ServiceTarget target, Map<String, Object> props) {
            FrameworkModuleIntegration service = new FrameworkModuleIntegration(props);
            ServiceBuilder<?> builder = target.addService(IntegrationServices.FRAMEWORK_MODULE_PLUGIN, service);
            builder.setInitialMode(Mode.ON_DEMAND);
            return builder.install();
        }

        private FrameworkModuleIntegration(Map<String, Object> props) {
            this.props = props;
        }

        @Override
        public void start(StartContext context) throws StartException {
            ServiceController<?> controller = context.getController();
            LOGGER.tracef("Starting: %s in mode %s", controller.getName(), controller.getMode());
        }

        @Override
        public void stop(StopContext context) {
            ServiceController<?> controller = context.getController();
            LOGGER.tracef("Stopping: %s in mode %s", controller.getName(), controller.getMode());
            frameworkModule = null;
        }

        @Override
        public FrameworkModulePlugin getValue() throws IllegalStateException {
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
            ModuleSpec.Builder specBuilder = ModuleSpec.build(ModuleIdentifier.create(JBOSGI_PREFIX + ".framework"));

            // Add the framework module dependencies
            String sysmodules = (String) props.get(PROP_JBOSS_OSGI_SYSTEM_MODULES);
            if (sysmodules == null)
                sysmodules = "";

            String extramodules = (String) props.get(PROP_JBOSS_OSGI_SYSTEM_MODULES_EXTRA);
            if (extramodules != null)
                sysmodules += "," + extramodules;

            // Add a dependency on the default framework modules
            ModuleLoader bootLoader = Module.getBootModuleLoader();
            PathFilter acceptAll = PathFilters.acceptAll();
            for (String modid : sysmodules.split(",")) {
                modid = modid.trim();
                if (modid.length() > 0) {
                    ModuleIdentifier identifier = ModuleIdentifier.create(modid);
                    specBuilder.addDependency(DependencySpec.createModuleDependencySpec(acceptAll, acceptAll, bootLoader, identifier, false));
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
                        return "FrameworkModuleLoader";
                    }
                };
                return moduleLoader.loadModule(specBuilder.getIdentifier());
            } catch (ModuleLoadException ex) {
                throw new IllegalStateException(ex);
            }
        }
    }
}

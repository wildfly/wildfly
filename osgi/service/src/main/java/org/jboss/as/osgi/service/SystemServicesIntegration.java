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
import static org.jboss.as.osgi.OSGiLogger.LOGGER;
import static org.jboss.as.server.Services.JBOSS_SERVER_CONTROLLER;
import static org.jboss.osgi.repository.XRepository.MODULE_IDENTITY_NAMESPACE;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.network.SocketBinding;
import org.jboss.as.osgi.SubsystemExtension;
import org.jboss.as.osgi.management.OSGiRuntimeResource;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.ServerEnvironmentService;
import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.value.InjectedValue;
import org.jboss.osgi.framework.BundleManager;
import org.jboss.osgi.framework.IntegrationService;
import org.jboss.osgi.framework.Services;
import org.jboss.osgi.framework.SystemServicesPlugin;
import org.jboss.osgi.repository.RepositoryStorage;
import org.jboss.osgi.repository.RepositoryStorageException;
import org.jboss.osgi.repository.RepositoryStorageFactory;
import org.jboss.osgi.repository.XRepository;
import org.jboss.osgi.repository.XRepositoryBuilder;
import org.jboss.osgi.repository.core.FileBasedRepositoryStorage;
import org.jboss.osgi.resolver.XResource;
import org.osgi.framework.BundleContext;

/**
 * An {@link IntegrationService} that provides system services on framework startup
 *
 * @author Thomas.Diesler@jboss.com
 * @since 11-Sep-2010
 */
final class SystemServicesIntegration implements Service<SystemServicesPlugin>, SystemServicesPlugin, IntegrationService<SystemServicesPlugin> {

    private final InjectedValue<ServerEnvironment> injectedServerEnvironment = new InjectedValue<ServerEnvironment>();
    private final InjectedValue<ModelController> injectedModelController = new InjectedValue<ModelController>();
    private final InjectedValue<BundleManager> injectedBundleManager = new InjectedValue<BundleManager>();
    private final InjectedValue<BundleContext> injectedBundleContext = new InjectedValue<BundleContext>();
    private final List<SubsystemExtension> extensions;
    private final OSGiRuntimeResource resource;
    private ServiceContainer serviceContainer;
    private ExecutorService controllerThreadExecutor;

    SystemServicesIntegration(OSGiRuntimeResource resource, List<SubsystemExtension> extensions) {
        this.extensions = extensions;
        this.resource = resource;
    }

    @Override
    public ServiceName getServiceName() {
        return SYSTEM_SERVICES_PLUGIN;
    }

    @Override
    public ServiceController<SystemServicesPlugin> install(ServiceTarget serviceTarget) {
        ServiceBuilder<SystemServicesPlugin> builder = serviceTarget.addService(getServiceName(), this);
        builder.addDependency(JBOSS_SERVER_CONTROLLER, ModelController.class, injectedModelController);
        builder.addDependency(ServerEnvironmentService.SERVICE_NAME, ServerEnvironment.class, injectedServerEnvironment);
        builder.addDependency(Services.BUNDLE_MANAGER, BundleManager.class, injectedBundleManager);
        builder.addDependency(Services.FRAMEWORK_CREATE, BundleContext.class, injectedBundleContext);
        builder.addDependency(Services.FRAMEWORK_CREATE);

        // Subsystem extension dependencies
        for(SubsystemExtension extension : extensions) {
            extension.configureSystemServiceDependencies(builder);
        }

        builder.setInitialMode(Mode.ON_DEMAND);
        return builder.install();
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
        String bindingNames = syscontext.getProperty(FrameworkBootstrapService.MAPPED_OSGI_SOCKET_BINDINGS);
        if (bindingNames != null) {
            final Set<ServiceName> socketBindingNames = new HashSet<ServiceName>();
            for (String suffix : bindingNames.split(",")) {
                socketBindingNames.add(JBOSS_BINDING_NAME.append(suffix));
            }
            ServiceTarget serviceTarget = startContext.getChildTarget();
            ServiceName serviceName = IntegrationService.SYSTEM_SERVICES_PLUGIN.append("BINDINGS");
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
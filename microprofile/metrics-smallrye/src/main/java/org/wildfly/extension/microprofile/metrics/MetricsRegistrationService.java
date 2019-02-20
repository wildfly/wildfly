/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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
package org.wildfly.extension.microprofile.metrics;

import static org.wildfly.extension.microprofile.config.smallrye.ServiceNames.CONFIG_PROVIDER;
import static org.wildfly.extension.microprofile.metrics.MicroProfileMetricsSubsystemDefinition.CLIENT_FACTORY_CAPABILITY;
import static org.wildfly.extension.microprofile.metrics.MicroProfileMetricsSubsystemDefinition.MANAGEMENT_EXECUTOR;
import static org.wildfly.extension.microprofile.metrics.MicroProfileMetricsSubsystemDefinition.WILDFLY_REGISTRATION_SERVICE;
import static org.wildfly.extension.microprofile.metrics._private.MicroProfileMetricsLogger.LOGGER;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Function;
import java.util.function.Supplier;

import io.smallrye.metrics.MetricRegistries;
import io.smallrye.metrics.setup.JmxRegistrar;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.jboss.as.controller.LocalModelControllerClient;
import org.jboss.as.controller.ModelControllerClientFactory;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.server.Services;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

public class MetricsRegistrationService implements Service<MetricsRegistrationService> {

    private final ImmutableManagementResourceRegistration rootResourceRegistration;
    private final Resource rootResource;
    private final Supplier<ModelControllerClientFactory> modelControllerClientFactory;
    private final Supplier<Executor> managementExecutor;
    private Supplier<ExecutorService> executorService;
    private List<String> exposedSubsystems;
    private String globalPrefix;
    private MetricCollector metricCollector;
    private JmxRegistrar jmxRegistrar;
    private LocalModelControllerClient modelControllerClient;

    private MetricCollector.MetricRegistration registration;

    static void install(OperationContext context, List<String> exposedSubsystems, String prefix) {
        ImmutableManagementResourceRegistration rootResourceRegistration = context.getRootResourceRegistration();
        Resource rootResource = context.readResourceFromRoot(PathAddress.EMPTY_ADDRESS);

        ServiceBuilder<?> serviceBuilder = context.getServiceTarget().addService(WILDFLY_REGISTRATION_SERVICE);
        Supplier<ModelControllerClientFactory> modelControllerClientFactory = serviceBuilder.requires(context.getCapabilityServiceName(CLIENT_FACTORY_CAPABILITY, ModelControllerClientFactory.class));
        Supplier<Executor> managementExecutor = serviceBuilder.requires(context.getCapabilityServiceName(MANAGEMENT_EXECUTOR, Executor.class));
        serviceBuilder.requires(CONFIG_PROVIDER);
        Supplier<ExecutorService> executorService = Services.requireServerExecutor(serviceBuilder);
        MetricsRegistrationService service = new MetricsRegistrationService(rootResourceRegistration, rootResource, modelControllerClientFactory, managementExecutor, executorService, exposedSubsystems, prefix);
        serviceBuilder.setInstance(service)
                .install();
    }

    public MetricsRegistrationService(ImmutableManagementResourceRegistration rootResourceRegistration, Resource rootResource, Supplier<ModelControllerClientFactory> modelControllerClientFactory, Supplier<Executor> managementExecutor, Supplier<ExecutorService> executorService, List<String> exposedSubsystems, String globalPrefix) {
        this.rootResourceRegistration = rootResourceRegistration;
        this.rootResource = rootResource;
        this.modelControllerClientFactory = modelControllerClientFactory;
        this.managementExecutor = managementExecutor;
        this.executorService = executorService;
        this.exposedSubsystems = exposedSubsystems;
        this.globalPrefix = globalPrefix;
    }

    @Override
    public void start(StartContext context) throws StartException {
        final Runnable task = new Runnable() {
            @Override
            public void run() {
                try {
                    doStart(context);
                } catch (StartException e) {
                    context.failed(e);
                } finally {
                    context.complete();
                }
            }
        };
        try {
            executorService.get().submit(task);
        } catch (RejectedExecutionException e) {
            task.run();
        }

    }

    private void doStart(StartContext context) throws StartException {
        final ServiceContainer serviceContainer = context.getController().getServiceContainer();
        try {
            serviceContainer.awaitStability();
        } catch (InterruptedException e) {
        }
        jmxRegistrar = new JmxRegistrar();
        try {
            jmxRegistrar.init();
        } catch (IOException e) {
            throw LOGGER.failedInitializeJMXRegistrar(e);
        }

        modelControllerClient = modelControllerClientFactory.get().createClient(managementExecutor.get());

        metricCollector = new MetricCollector(modelControllerClient, rootResourceRegistration, exposedSubsystems, globalPrefix);

        registration = metricCollector.collectResourceMetrics(rootResource, rootResourceRegistration, Function.identity());
    }

    @Override
    public void stop(StopContext context) {
        for (MetricRegistry registry : new MetricRegistry[]{
                MetricRegistries.get(MetricRegistry.Type.BASE),
                MetricRegistries.get(MetricRegistry.Type.VENDOR)}) {
            for (String name : registry.getNames()) {
                registry.remove(name);
            }
        }

        registration.unregister();
        metricCollector.close();

        modelControllerClient.close();

        jmxRegistrar = null;
    }

    @Override
    public MetricsRegistrationService getValue() {
        return this;
    }

    public MetricCollector.MetricRegistration registerMetrics(Resource rootResource,
                                                              ImmutableManagementResourceRegistration managementResourceRegistration,
                                                              Function<PathAddress, PathAddress> resourceAddressResolver) {
        return metricCollector.collectResourceMetrics(rootResource, managementResourceRegistration, resourceAddressResolver);
    }
}
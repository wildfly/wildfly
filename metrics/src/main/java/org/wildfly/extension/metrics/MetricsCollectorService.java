/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.metrics;

import static org.wildfly.extension.metrics.MetricsSubsystemDefinition.WILDFLY_COLLECTOR;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jboss.as.controller.LocalModelControllerClient;
import org.jboss.as.controller.ModelControllerClientFactory;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.ProcessStateNotifier;
import org.jboss.as.controller.RequirementServiceBuilder;
import org.jboss.as.controller.management.Capabilities;
import org.jboss.as.controller.notification.NotificationHandlerRegistry;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;

/**
 * Service to create a metric collector
 */
public class MetricsCollectorService implements Service<MetricCollector> {

    private final Supplier<ModelControllerClientFactory> modelControllerClientFactory;
    private final Supplier<Executor> managementExecutor;
    private final Supplier<ProcessStateNotifier> processStateNotifier;
    private final Supplier<NotificationHandlerRegistry> notificationHandlerRegistry;
    private final boolean exposeAnySubsystem;
    private final List<String> exposedSubsystems;
    private Consumer<MetricCollector> metricCollectorConsumer;

    private MetricCollector metricCollector;
    private LocalModelControllerClient modelControllerClient;
    private LocalModelControllerClient discoveryClient;
    private MetricsNotificationHandler notificationHandler;

    static void install(OperationContext context, boolean exposeAnySubsystem,
            List<String> exposedSubsystems) {
        RequirementServiceBuilder<?> serviceBuilder = context.getCapabilityServiceTarget().addService(WILDFLY_COLLECTOR);
        Supplier<ModelControllerClientFactory> modelControllerClientFactory = serviceBuilder.requires(ModelControllerClientFactory.SERVICE_DESCRIPTOR);
        Supplier<Executor> managementExecutor = serviceBuilder.requires(Capabilities.MANAGEMENT_EXECUTOR);
        Supplier<ProcessStateNotifier> processStateNotifier = serviceBuilder.requires(ProcessStateNotifier.SERVICE_DESCRIPTOR);
        Supplier<NotificationHandlerRegistry> notificationHandlerRegistry = serviceBuilder.requires(NotificationHandlerRegistry.SERVICE_DESCRIPTOR);
        Consumer<MetricCollector> metricCollectorConsumer = serviceBuilder.provides(WILDFLY_COLLECTOR);
        MetricsCollectorService service = new MetricsCollectorService(modelControllerClientFactory, managementExecutor,
                processStateNotifier, notificationHandlerRegistry, metricCollectorConsumer,
                exposeAnySubsystem, exposedSubsystems);
        serviceBuilder.setInstance(service)
                .install();
    }

    MetricsCollectorService(Supplier<ModelControllerClientFactory> modelControllerClientFactory, Supplier<Executor> managementExecutor,
                            Supplier<ProcessStateNotifier> processStateNotifier,
                            Supplier<NotificationHandlerRegistry> notificationHandlerRegistry,
                            Consumer<MetricCollector> metricCollectorConsumer,
                            boolean exposeAnySubsystem,
                            List<String> exposedSubsystems) {
        this.modelControllerClientFactory = modelControllerClientFactory;
        this.managementExecutor = managementExecutor;
        this.processStateNotifier = processStateNotifier;
        this.notificationHandlerRegistry = notificationHandlerRegistry;
        this.metricCollectorConsumer = metricCollectorConsumer;
        this.exposeAnySubsystem = exposeAnySubsystem;
        this.exposedSubsystems = exposedSubsystems;
    }

    @Override
    public void start(StartContext context) {
        // [WFLY-11933] if RBAC is enabled, the local client does not have enough priviledges to read metrics
        modelControllerClient = modelControllerClientFactory.get().createClient(managementExecutor.get());
        // [WFLY-22219] Discovery of metrics for resources added at runtime runs
        // asynchronously on a management thread that carries no authenticated identity, so
        // it must use a SuperUser client; otherwise RBAC denies every read-resource and the
        // retry queue spins until it times out. This only reads resource structure to register
        // metric metadata; the per-user value reads above still enforce RBAC at scrape time.
        discoveryClient = modelControllerClientFactory.get().createSuperUserClient(managementExecutor.get());

        metricCollector = new MetricCollector(modelControllerClient,
                                              discoveryClient,
                                              processStateNotifier.get(),
                                              managementExecutor.get(),
                                              exposeAnySubsystem,
                                              exposedSubsystems);
        notificationHandler = new MetricsNotificationHandler(notificationHandlerRegistry.get(),
                metricCollector::resourceAdded, metricCollector::resourceRemoved);
        notificationHandler.start();

        metricCollectorConsumer.accept(metricCollector);
    }

    @Override
    public void stop(StopContext context) {
        notificationHandler.stop();
        metricCollector.stop();
        metricCollectorConsumer.accept(null);
        metricCollector = null;

        modelControllerClient.close();
        discoveryClient.close();
    }

    @Override
    public MetricCollector getValue() throws IllegalStateException, IllegalArgumentException {
        return metricCollector;
    }
}

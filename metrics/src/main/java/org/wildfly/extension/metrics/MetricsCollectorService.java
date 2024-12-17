/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.metrics;

import static org.wildfly.extension.metrics.MetricsSubsystemDefinition.WILDFLY_COLLECTOR;

import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jboss.as.controller.LocalModelControllerClient;
import org.jboss.as.controller.ModelControllerClientFactory;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.ProcessStateNotifier;
import org.jboss.as.controller.RequirementServiceBuilder;
import org.jboss.as.controller.management.Capabilities;
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
    private Consumer<MetricCollector> metricCollectorConsumer;

    private MetricCollector metricCollector;
    private LocalModelControllerClient modelControllerClient;

    static void install(OperationContext context) {
        RequirementServiceBuilder<?> serviceBuilder = context.getCapabilityServiceTarget().addService(WILDFLY_COLLECTOR);
        Supplier<ModelControllerClientFactory> modelControllerClientFactory = serviceBuilder.requires(ModelControllerClientFactory.SERVICE_DESCRIPTOR);
        Supplier<Executor> managementExecutor = serviceBuilder.requires(Capabilities.MANAGEMENT_EXECUTOR);
        Supplier<ProcessStateNotifier> processStateNotifier = serviceBuilder.requires(ProcessStateNotifier.SERVICE_DESCRIPTOR);
        Consumer<MetricCollector> metricCollectorConsumer = serviceBuilder.provides(WILDFLY_COLLECTOR);
        MetricsCollectorService service = new MetricsCollectorService(modelControllerClientFactory, managementExecutor, processStateNotifier, metricCollectorConsumer);
        serviceBuilder.setInstance(service)
                .install();
    }

    MetricsCollectorService(Supplier<ModelControllerClientFactory> modelControllerClientFactory, Supplier<Executor> managementExecutor,
                            Supplier<ProcessStateNotifier> processStateNotifier, Consumer<MetricCollector> metricCollectorConsumer) {
        this.modelControllerClientFactory = modelControllerClientFactory;
        this.managementExecutor = managementExecutor;
        this.processStateNotifier = processStateNotifier;
        this.metricCollectorConsumer = metricCollectorConsumer;
    }

    @Override
    public void start(StartContext context) {
        // [WFLY-11933] if RBAC is enabled, the local client does not have enough priviledges to read metrics
        modelControllerClient = modelControllerClientFactory.get().createClient(managementExecutor.get());

        metricCollector = new MetricCollector(modelControllerClient, processStateNotifier.get());

        metricCollectorConsumer.accept(metricCollector);
    }

    @Override
    public void stop(StopContext context) {
        metricCollectorConsumer.accept(null);
        metricCollector = null;

        modelControllerClient.close();
    }

    @Override
    public MetricCollector getValue() throws IllegalStateException, IllegalArgumentException {
        return metricCollector;
    }
}

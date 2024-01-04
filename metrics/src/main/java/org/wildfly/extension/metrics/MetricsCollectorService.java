/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.metrics;

import static org.wildfly.extension.metrics.MetricsSubsystemDefinition.CLIENT_FACTORY_CAPABILITY;
import static org.wildfly.extension.metrics.MetricsSubsystemDefinition.MANAGEMENT_EXECUTOR;
import static org.wildfly.extension.metrics.MetricsSubsystemDefinition.PROCESS_STATE_NOTIFIER;
import static org.wildfly.extension.metrics.MetricsSubsystemDefinition.WILDFLY_COLLECTOR;

import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jboss.as.controller.LocalModelControllerClient;
import org.jboss.as.controller.ModelControllerClientFactory;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.ProcessStateNotifier;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
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
        ServiceBuilder<?> serviceBuilder = context.getServiceTarget().addService(WILDFLY_COLLECTOR);
        Supplier<ModelControllerClientFactory> modelControllerClientFactory = serviceBuilder.requires(context.getCapabilityServiceName(CLIENT_FACTORY_CAPABILITY, ModelControllerClientFactory.class));
        Supplier<Executor> managementExecutor = serviceBuilder.requires(context.getCapabilityServiceName(MANAGEMENT_EXECUTOR, Executor.class));
        Supplier<ProcessStateNotifier> processStateNotifier = serviceBuilder.requires(context.getCapabilityServiceName(PROCESS_STATE_NOTIFIER, ProcessStateNotifier.class));
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

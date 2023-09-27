/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.metrics;

import static org.wildfly.extension.metrics.MetricsSubsystemDefinition.METRICS_REGISTRY_RUNTIME_CAPABILITY;

import java.io.IOException;
import java.util.function.Consumer;

import org.jboss.as.controller.OperationContext;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.wildfly.extension.metrics._private.MetricsLogger;
import org.wildfly.extension.metrics.jmx.JmxMetricCollector;

/**
 * Service to create a registry for WildFly (and JMX) metrics.
 */
public class WildFlyMetricRegistryService implements Service<WildFlyMetricRegistry> {

    private final Consumer<WildFlyMetricRegistry> consumer;
    private WildFlyMetricRegistry registry;

    static void install(OperationContext context) {
        ServiceBuilder<?> serviceBuilder = context.getServiceTarget().addService(METRICS_REGISTRY_RUNTIME_CAPABILITY.getCapabilityServiceName());

        Consumer<WildFlyMetricRegistry> registry = serviceBuilder.provides(METRICS_REGISTRY_RUNTIME_CAPABILITY.getCapabilityServiceName());
        serviceBuilder.setInstance(new WildFlyMetricRegistryService(registry)).install();
    }

    WildFlyMetricRegistryService(Consumer<WildFlyMetricRegistry> consumer) {
        this.consumer = consumer;
    }

    @Override
    public void start(StartContext context) {
        registry = new WildFlyMetricRegistry();

        // register metrics from JMX MBeans for base metrics
        JmxMetricCollector jmxMetricCollector = new JmxMetricCollector(registry);
        try {
            jmxMetricCollector.init();
        } catch (IOException e) {
            throw MetricsLogger.LOGGER.failedInitializeJMXRegistrar(e);
        }
        consumer.accept(registry);
    }

    @Override
    public void stop(StopContext context) {
        consumer.accept(null);
        registry.close();
        registry = null;
    }

    @Override
    public WildFlyMetricRegistry getValue() throws IllegalStateException, IllegalArgumentException {
        return registry;
    }
}
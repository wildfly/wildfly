/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.micrometer;

import static org.wildfly.extension.micrometer.MicrometerExtensionLogger.MICROMETER_LOGGER;

import java.io.IOException;
import java.util.function.Function;

import org.jboss.as.controller.LocalModelControllerClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ProcessStateNotifier;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.wildfly.extension.micrometer.jmx.JmxMicrometerCollector;
import org.wildfly.extension.micrometer.metrics.MetricRegistration;
import org.wildfly.extension.micrometer.metrics.MicrometerCollector;
import org.wildfly.extension.micrometer.registry.NoOpRegistry;
import org.wildfly.extension.micrometer.registry.WildFlyOtlpRegistry;
import org.wildfly.extension.micrometer.registry.WildFlyRegistry;

public class MicrometerService implements AutoCloseable {
    private final WildFlyMicrometerConfig micrometerConfig;
    private final LocalModelControllerClient modelControllerClient;
    private final ProcessStateNotifier processStateNotifier;
    private MicrometerCollector micrometerCollector;
    private WildFlyRegistry micrometerRegistry;

    public MicrometerService(WildFlyMicrometerConfig micrometerConfig,
                             LocalModelControllerClient modelControllerClient,
                             ProcessStateNotifier processStateNotifier) {
        this.micrometerConfig = micrometerConfig;
        this.modelControllerClient = modelControllerClient;
        this.processStateNotifier = processStateNotifier;
    }

    public WildFlyRegistry getMicrometerRegistry() {
        return micrometerRegistry;
    }

    public synchronized MetricRegistration collectResourceMetrics(final Resource resource,
                                                                  ImmutableManagementResourceRegistration mrr,
                                                                  Function<PathAddress, PathAddress> addressResolver) {
        return micrometerCollector.collectResourceMetrics(resource, mrr, addressResolver);
    }

    public void start() {
        micrometerRegistry = micrometerConfig.url() != null ?
            new WildFlyOtlpRegistry(micrometerConfig) :
            new NoOpRegistry();
        micrometerCollector = new MicrometerCollector(modelControllerClient, processStateNotifier, micrometerRegistry,
            micrometerConfig.getSubsystemFilter());

        registerJmxMetrics();
    }

    private void registerJmxMetrics() {
        try {
            new JmxMicrometerCollector(micrometerRegistry).init();
        } catch (IOException e) {
            throw MICROMETER_LOGGER.failedInitializeJMXRegistrar(e);
        }
    }

    @Override
    public void close() {
        micrometerRegistry.close();
    }
}

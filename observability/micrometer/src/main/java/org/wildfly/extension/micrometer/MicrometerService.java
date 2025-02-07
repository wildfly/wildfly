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
import org.wildfly.extension.micrometer.registry.WildFlyCompositeRegistry;
import org.wildfly.extension.micrometer.registry.WildFlyRegistry;

public class MicrometerService implements AutoCloseable {
    private WildFlyMicrometerConfig micrometerConfig;
    private LocalModelControllerClient modelControllerClient;
    private ProcessStateNotifier processStateNotifier;
    private MicrometerCollector micrometerCollector;
    private WildFlyCompositeRegistry micrometerRegistry;

    private MicrometerService() {
    }

    public void start() {
        micrometerCollector = new MicrometerCollector(modelControllerClient, processStateNotifier, micrometerRegistry,
            micrometerConfig.getSubsystemFilter());

        registerJmxMetrics();
    }

    public WildFlyCompositeRegistry getMicrometerRegistry() {
        return micrometerRegistry;
    }

    public void addRegistry(WildFlyRegistry registry) {
        micrometerRegistry.addRegistry(registry);
    }

    public synchronized MetricRegistration collectResourceMetrics(final Resource resource,
                                                                  ImmutableManagementResourceRegistration mrr,
                                                                  Function<PathAddress, PathAddress> addressResolver) {
        return micrometerCollector.collectResourceMetrics(resource, mrr, addressResolver);
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


    void setMicrometerConfig(WildFlyMicrometerConfig micrometerConfig) {
        this.micrometerConfig = micrometerConfig;
    }

    void setModelControllerClient(LocalModelControllerClient modelControllerClient) {
        this.modelControllerClient = modelControllerClient;
    }

    void setProcessStateNotifier(ProcessStateNotifier processStateNotifier) {
        this.processStateNotifier = processStateNotifier;
    }

    void setMicrometerRegistry(WildFlyCompositeRegistry micrometerRegistry) {
        this.micrometerRegistry = micrometerRegistry;
    }


    public static class Builder {
        private final MicrometerService service = new MicrometerService();

        public Builder micrometerConfig(WildFlyMicrometerConfig micrometerConfig) {
            service.setMicrometerConfig(micrometerConfig);
            return this;
        }
        public Builder modelControllerClient(LocalModelControllerClient modelControllerClient) {
            service.setModelControllerClient(modelControllerClient);
            return this;
        }
        public Builder processStateNotifier(ProcessStateNotifier processStateNotifier) {
            service.setProcessStateNotifier(processStateNotifier);
            return this;
        }
        public Builder micrometerRegistry(WildFlyCompositeRegistry micrometerRegistry) {
            service.setMicrometerRegistry(micrometerRegistry);
            return this;
        }

        public MicrometerService build() {
            assert service.micrometerRegistry != null &&
                service.micrometerConfig != null &&
                service.modelControllerClient != null &&
                service.processStateNotifier != null;

            return service;
        }
    }
}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.micrometer;

import static org.wildfly.extension.micrometer.MicrometerExtensionLogger.MICROMETER_LOGGER;

import java.io.IOException;
import java.util.function.Function;

import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadDeadlockMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import org.jboss.as.controller.LocalModelControllerClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ProcessStateNotifier;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.wildfly.extension.micrometer.jmx.JmxMicrometerCollector;
import org.wildfly.extension.micrometer.metrics.MetricRegistration;
import org.wildfly.extension.micrometer.metrics.MicrometerCollector;
import org.wildfly.extension.micrometer.registry.WildFlyCompositeRegistry;

public class MicrometerService {
    private final WildFlyMicrometerConfig micrometerConfig;
    private final LocalModelControllerClient modelControllerClient;
    private final ProcessStateNotifier processStateNotifier;
    private final WildFlyCompositeRegistry micrometerRegistry;

    private MicrometerCollector micrometerCollector;
    private ImmutableManagementResourceRegistration modelRegistration;
    private MetricRegistration modelMetrics;

    private MicrometerService(WildFlyMicrometerConfig micrometerConfig,
                              LocalModelControllerClient modelControllerClient,
                              ProcessStateNotifier processStateNotifier,
                              WildFlyCompositeRegistry micrometerRegistry) {
        this.micrometerConfig = micrometerConfig;
        this.modelControllerClient = modelControllerClient;
        this.processStateNotifier = processStateNotifier;
        this.micrometerRegistry = micrometerRegistry;
    }

    public void start() {
        registerSystemMetrics();
        registerModelMetrics();
        registerJmxMetrics();
    }

    public WildFlyCompositeRegistry getMicrometerRegistry() {
        return micrometerRegistry;
    }

    public synchronized MetricRegistration collectDeploymentResourceMetrics(final Resource resource,
                                                                            ImmutableManagementResourceRegistration registration,
                                                                            Function<PathAddress, PathAddress> addressResolver) {
        return micrometerCollector.collectResourceMetrics(resource, registration, addressResolver);
    }

    public synchronized MetricRegistration collectRootResourceMetrics(Resource resource,
                                                                      ImmutableManagementResourceRegistration registration) {
        modelRegistration = registration;
        modelMetrics = micrometerCollector.collectResourceMetrics(resource, registration, Function.identity());
        return modelMetrics;
    }

    public void resourceAdded(PathAddress address) {
        ImmutableManagementResourceRegistration registration;
        MetricRegistration metrics;
        synchronized (this) {
            registration = modelRegistration;
            metrics = modelMetrics;
        }
        if (registration == null || metrics == null) {
            return;
        }
        RuntimeException lastFailure = null;
        for (int attempt = 0; attempt < 10; attempt++) {
            ModelNode result = null;
            try {
                result = modelControllerClient.execute(Operations.createReadResourceOperation(address.toModelNode(), true));
            } catch (RuntimeException e) {
                lastFailure = e;
            }
            if (result != null && Operations.isSuccessfulOutcome(result)) {
                Resource resource = Resource.Factory.create();
                resource.writeModel(result.get(ModelDescriptionConstants.RESULT));
                synchronized (this) {
                    if (modelRegistration == registration && modelMetrics == metrics) {
                        micrometerCollector.collectResourceMetrics(resource, registration, address,
                                Function.identity(), metrics);
                    }
                }
                return;
            }
            if (attempt == 9) {
                break;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                MICROMETER_LOGGER.debug("Interrupted while collecting metrics for resource " + address);
                return;
            }
        }
        MICROMETER_LOGGER.unableToCollectMetrics(address, 10,
             lastFailure == null ? "" : ": " + lastFailure.getMessage());
    }

    public synchronized void resourceRemoved(PathAddress address) {
        if (modelMetrics != null) {
            modelMetrics.unregister(address);
        }
    }

    public synchronized void stop() {
        if (modelMetrics != null) {
            modelMetrics.unregister();
            modelMetrics = null;
            modelRegistration = null;
        }
    }

    private void registerSystemMetrics() {
        new ClassLoaderMetrics().bindTo(micrometerRegistry);
        new JvmMemoryMetrics().bindTo(micrometerRegistry);
        new JvmGcMetrics().bindTo(micrometerRegistry);
        new ProcessorMetrics().bindTo(micrometerRegistry);
        new JvmThreadMetrics().bindTo(micrometerRegistry);
        new JvmThreadDeadlockMetrics().bindTo(micrometerRegistry);
    }

    private void registerModelMetrics() {
        micrometerCollector = new MicrometerCollector(modelControllerClient, processStateNotifier, micrometerRegistry,
                micrometerConfig.getSubsystemFilter());
    }

    private void registerJmxMetrics() {
        try {
            new JmxMicrometerCollector(micrometerRegistry).init();
        } catch (IOException e) {
            throw MICROMETER_LOGGER.failedInitializeJMXRegistrar(e);
        }
    }

    public static class Builder {
        private WildFlyMicrometerConfig micrometerConfig;
        private LocalModelControllerClient modelControllerClient;
        private ProcessStateNotifier processStateNotifier;
        private WildFlyCompositeRegistry micrometerRegistry;

        public Builder micrometerConfig(WildFlyMicrometerConfig micrometerConfig) {
            this.micrometerConfig = micrometerConfig;
            return this;
        }

        public Builder modelControllerClient(LocalModelControllerClient modelControllerClient) {
            this.modelControllerClient = modelControllerClient;
            return this;
        }

        public Builder processStateNotifier(ProcessStateNotifier processStateNotifier) {
            this.processStateNotifier = processStateNotifier;
            return this;
        }

        public Builder micrometerRegistry(WildFlyCompositeRegistry micrometerRegistry) {
            this.micrometerRegistry = micrometerRegistry;
            return this;
        }

        public MicrometerService build() {
            assert micrometerRegistry != null &&
                micrometerConfig != null &&
                modelControllerClient != null &&
                processStateNotifier != null;

            return new MicrometerService(micrometerConfig, modelControllerClient, processStateNotifier, micrometerRegistry);
        }
    }
}

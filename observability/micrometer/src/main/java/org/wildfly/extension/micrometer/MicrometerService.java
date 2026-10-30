/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.micrometer;

import static org.wildfly.extension.micrometer.MicrometerExtensionLogger.MICROMETER_LOGGER;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
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
    private final ResourceMetricsRetryQueue resourceMetricsRetryQueue;

    private MicrometerCollector micrometerCollector;
    private ImmutableManagementResourceRegistration modelRegistration;
    private MetricRegistration modelMetrics;
    private final Map<PathAddress, RuntimeException> lastFailures = new ConcurrentHashMap<>();

    private MicrometerService(WildFlyMicrometerConfig micrometerConfig,
                              LocalModelControllerClient modelControllerClient,
                              ProcessStateNotifier processStateNotifier,
                              WildFlyCompositeRegistry micrometerRegistry,
                              Executor executor) {
        this.micrometerConfig = micrometerConfig;
        this.modelControllerClient = modelControllerClient;
        this.processStateNotifier = processStateNotifier;
        this.micrometerRegistry = micrometerRegistry;
        this.resourceMetricsRetryQueue = new ResourceMetricsRetryQueue(executor, this::collectResourceMetrics,
                this::unableToCollectMetrics);
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

    /**
     * Queues a newly added management resource for asynchronous metric collection.
     *
     * @param address the address of the added resource
     */
    public synchronized void resourceAdded(PathAddress address) {
        if (modelRegistration == null || modelMetrics == null) {
            return;
        }
        resourceMetricsRetryQueue.add(address);
    }

    /**
     * Removes metrics and pending collection work for a removed management resource.
     *
     * @param address the address of the removed resource
     */
    public synchronized void resourceRemoved(PathAddress address) {
        resourceMetricsRetryQueue.remove(address);
        lastFailures.remove(address);
        if (modelMetrics != null) {
            modelMetrics.unregister(address);
        }
    }

    /**
     * Stops asynchronous collection and unregisters all model metrics.
     */
    public synchronized void stop() {
        resourceMetricsRetryQueue.stop();
        lastFailures.clear();
        if (modelMetrics != null) {
            modelMetrics.unregister();
            modelMetrics = null;
            modelRegistration = null;
        }
    }

    /**
     * Attempts one read and registration for a queued resource.
     *
     * @param address the resource address to collect
     * @param attempt the one-based attempt number
     * @return {@code true} when processing is complete, otherwise retry
     */
    private boolean collectResourceMetrics(ResourceMetricsRetryQueue.PendingResource pendingResource, int attempt) {
        PathAddress address = pendingResource.address();
        ImmutableManagementResourceRegistration registration;
        MetricRegistration metrics;
        synchronized (this) {
            registration = modelRegistration;
            metrics = modelMetrics;
        }
        if (registration == null || metrics == null) {
            return true;
        }
        ModelNode result = null;
        try {
            result = modelControllerClient.execute(Operations.createReadResourceOperation(address.toModelNode(), true));
        } catch (RuntimeException e) {
            synchronized (this) {
                if (resourceMetricsRetryQueue.isPending(pendingResource)) {
                    lastFailures.put(address, e);
                }
            }
        }
        if (result != null && Operations.isSuccessfulOutcome(result)) {
            Resource resource = Resource.Factory.create();
            resource.writeModel(result.get(ModelDescriptionConstants.RESULT));
            synchronized (this) {
                if (System.nanoTime() < pendingResource.deadline()
                        && resourceMetricsRetryQueue.isPending(pendingResource)
                        && modelRegistration == registration && modelMetrics == metrics) {
                    micrometerCollector.collectResourceMetrics(resource, registration, address,
                            Function.identity(), metrics);
                    lastFailures.remove(address);
                    return true;
                }
                if (resourceMetricsRetryQueue.isPending(pendingResource)) {
                    lastFailures.remove(address);
                }
            }
        }
        return false;
    }

    /**
     * Logs the final failure after an address has exhausted its retry deadline.
     *
     * @param address the resource address that could not be read
     * @param attempts the number of attempts made
     */
    private synchronized void unableToCollectMetrics(PathAddress address, int attempts) {
        RuntimeException lastFailure = lastFailures.remove(address);
        MICROMETER_LOGGER.unableToCollectMetrics(address, attempts,
                lastFailure == null ? "" : ": " + lastFailure.getMessage());
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
        private Executor executor;

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

        /**
         * Supplies the executor used for asynchronous resource metric collection.
         *
         * @param executor the server-managed executor
         * @return this builder
         */
        public Builder executor(Executor executor) {
            this.executor = executor;
            return this;
        }

        public MicrometerService build() {
            assert micrometerRegistry != null &&
                micrometerConfig != null &&
                modelControllerClient != null &&
                processStateNotifier != null &&
                executor != null;

            return new MicrometerService(micrometerConfig, modelControllerClient, processStateNotifier, micrometerRegistry, executor);
        }
    }
}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.micrometer.service;

import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jboss.as.controller.LocalModelControllerClient;
import org.jboss.as.controller.ModelControllerClientFactory;
import org.jboss.as.controller.ProcessStateNotifier;
import org.jboss.msc.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.wildfly.extension.micrometer.metrics.MicrometerCollector;
import org.wildfly.extension.micrometer.registry.WildFlyCompositeRegistry;

/**
 * Service to create a metric collector
 */
public class MicrometerCollectorService implements Service {

    private final Supplier<ModelControllerClientFactory> modelControllerClientFactory;
    private final Supplier<Executor> managementExecutor;
    private final Supplier<ProcessStateNotifier> processStateNotifier;
    private final Consumer<MicrometerCollector> metricCollectorConsumer;
    private final WildFlyCompositeRegistry wildFlyRegistry;

    private LocalModelControllerClient modelControllerClient;

    public MicrometerCollectorService(Supplier<ModelControllerClientFactory> modelControllerClientFactory,
                                      Supplier<Executor> managementExecutor,
                                      Supplier<ProcessStateNotifier> processStateNotifier,
                                      WildFlyCompositeRegistry wildFlyRegistry,
                                      Consumer<MicrometerCollector> metricCollectorConsumer) {
        this.modelControllerClientFactory = modelControllerClientFactory;
        this.managementExecutor = managementExecutor;
        this.processStateNotifier = processStateNotifier;
        this.wildFlyRegistry = wildFlyRegistry;
        this.metricCollectorConsumer = metricCollectorConsumer;
    }

    @Override
    public void start(StartContext context) {
        // [WFLY-11933] if RBAC is enabled, the local client does not have enough privileges to read metrics
        modelControllerClient = modelControllerClientFactory.get().createClient(managementExecutor.get());

        MicrometerCollector micrometerCollector =
                new MicrometerCollector(modelControllerClient, processStateNotifier.get(), wildFlyRegistry);

        metricCollectorConsumer.accept(micrometerCollector);
    }

    @Override
    public void stop(StopContext context) {
        metricCollectorConsumer.accept(null);

        modelControllerClient.close();
    }

    /* Caches the MicrometerCollector created in Service.start for use by the subsystem. */
    public static final class MicrometerCollectorSupplier implements Consumer<MicrometerCollector>, Supplier<MicrometerCollector> {
        private final Consumer<MicrometerCollector> wrapped;
        private volatile MicrometerCollector collector;

        public MicrometerCollectorSupplier(Consumer<MicrometerCollector> wrapped) {
            this.wrapped = wrapped;
        }

        @Override
        public void accept(MicrometerCollector micrometerCollector) {
            this.collector = micrometerCollector;
            // Pass the collector on to MSC's consumer
            wrapped.accept(micrometerCollector);
        }

        @Override
        public MicrometerCollector get() {
            return collector;
        }
    }
}

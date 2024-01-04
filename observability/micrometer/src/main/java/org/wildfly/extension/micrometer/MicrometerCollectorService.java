/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.micrometer;

import static org.wildfly.extension.micrometer.MicrometerSubsystemDefinition.CLIENT_FACTORY_CAPABILITY;
import static org.wildfly.extension.micrometer.MicrometerSubsystemDefinition.MANAGEMENT_EXECUTOR;
import static org.wildfly.extension.micrometer.MicrometerSubsystemDefinition.MICROMETER_COLLECTOR;
import static org.wildfly.extension.micrometer.MicrometerSubsystemDefinition.MICROMETER_COLLECTOR_RUNTIME_CAPABILITY;
import static org.wildfly.extension.micrometer.MicrometerSubsystemDefinition.MICROMETER_REGISTRY_RUNTIME_CAPABILITY;
import static org.wildfly.extension.micrometer.MicrometerSubsystemDefinition.PROCESS_STATE_NOTIFIER;

import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jboss.as.controller.CapabilityServiceBuilder;
import org.jboss.as.controller.LocalModelControllerClient;
import org.jboss.as.controller.ModelControllerClientFactory;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.ProcessStateNotifier;
import org.jboss.msc.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.wildfly.extension.micrometer.metrics.MicrometerCollector;
import org.wildfly.extension.micrometer.registry.WildFlyRegistry;

/**
 * Service to create a metric collector
 */
class MicrometerCollectorService implements Service {

    private final Supplier<ModelControllerClientFactory> modelControllerClientFactory;
    private final Supplier<Executor> managementExecutor;
    private final Supplier<ProcessStateNotifier> processStateNotifier;
    private final Supplier<WildFlyRegistry> registrySupplier;
    private final Consumer<MicrometerCollector> metricCollectorConsumer;

    private LocalModelControllerClient modelControllerClient;

    /**
     * Installs a service that provides {@link MicrometerCollector}, and provides a {@link Supplier} the
     * subsystem can use to obtain that collector.
     * @param context the management operation context to use to install the service. Cannot be {@code null}
     * @return the {@link Supplier}. Will not return {@code null}.
     */
    static Supplier<MicrometerCollector> install(OperationContext context) {
        CapabilityServiceBuilder<?> serviceBuilder = context.getCapabilityServiceTarget().addCapability(MICROMETER_COLLECTOR_RUNTIME_CAPABILITY);
        Supplier<ModelControllerClientFactory> modelControllerClientFactory =
                serviceBuilder.requiresCapability(CLIENT_FACTORY_CAPABILITY, ModelControllerClientFactory.class);
        Supplier<Executor> managementExecutor = serviceBuilder.requiresCapability(MANAGEMENT_EXECUTOR, Executor.class);
        Supplier<ProcessStateNotifier> processStateNotifier =
                serviceBuilder.requiresCapability(PROCESS_STATE_NOTIFIER, ProcessStateNotifier.class);
        Supplier<WildFlyRegistry> registrySupplier =
                serviceBuilder.requiresCapability(MICROMETER_REGISTRY_RUNTIME_CAPABILITY.getName(), WildFlyRegistry.class);
        MicrometerCollectorSupplier collectorSupplier = new MicrometerCollectorSupplier(serviceBuilder.provides(MICROMETER_COLLECTOR));
        MicrometerCollectorService service = new MicrometerCollectorService(modelControllerClientFactory, managementExecutor,
                processStateNotifier, registrySupplier, collectorSupplier);
        serviceBuilder.setInstance(service)
                .install();
        return collectorSupplier;
    }

    MicrometerCollectorService(Supplier<ModelControllerClientFactory> modelControllerClientFactory,
                               Supplier<Executor> managementExecutor,
                               Supplier<ProcessStateNotifier> processStateNotifier,
                               Supplier<WildFlyRegistry> registrySupplier,
                               Consumer<MicrometerCollector> metricCollectorConsumer) {
        this.modelControllerClientFactory = modelControllerClientFactory;
        this.managementExecutor = managementExecutor;
        this.processStateNotifier = processStateNotifier;
        this.registrySupplier = registrySupplier;
        this.metricCollectorConsumer = metricCollectorConsumer;
    }

    @Override
    public void start(StartContext context) {
        // [WFLY-11933] if RBAC is enabled, the local client does not have enough privileges to read metrics
        modelControllerClient = modelControllerClientFactory.get().createClient(managementExecutor.get());

        MicrometerCollector micrometerCollector = new MicrometerCollector(modelControllerClient, processStateNotifier.get(),
                registrySupplier.get());

        metricCollectorConsumer.accept(micrometerCollector);
    }

    @Override
    public void stop(StopContext context) {
        metricCollectorConsumer.accept(null);

        modelControllerClient.close();
    }

    /* Caches the MicrometerCollector created in Service.start for use by the subsystem. */
    private static final class MicrometerCollectorSupplier implements Consumer<MicrometerCollector>, Supplier<MicrometerCollector> {
        private final Consumer<MicrometerCollector> wrapped;
        private volatile MicrometerCollector collector;

        private MicrometerCollectorSupplier(Consumer<MicrometerCollector> wrapped) {
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

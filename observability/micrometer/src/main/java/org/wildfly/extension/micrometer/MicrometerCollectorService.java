/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.wildfly.extension.micrometer;

import static org.wildfly.extension.micrometer.MicrometerSubsystemDefinition.CLIENT_FACTORY_CAPABILITY;
import static org.wildfly.extension.micrometer.MicrometerSubsystemDefinition.MANAGEMENT_EXECUTOR;
import static org.wildfly.extension.micrometer.MicrometerSubsystemDefinition.MICROMETER_COLLECTOR;
import static org.wildfly.extension.micrometer.MicrometerSubsystemDefinition.MICROMETER_REGISTRY_RUNTIME_CAPABILITY;
import static org.wildfly.extension.micrometer.MicrometerSubsystemDefinition.PROCESS_STATE_NOTIFIER;

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
import org.wildfly.extension.micrometer.metrics.MicrometerCollector;
import org.wildfly.extension.micrometer.metrics.WildFlyRegistry;

/**
 * Service to create a metric collector
 */
public class MicrometerCollectorService implements Service<MicrometerCollector> {

    private final Supplier<ModelControllerClientFactory> modelControllerClientFactory;
    private final Supplier<Executor> managementExecutor;
    private final Supplier<ProcessStateNotifier> processStateNotifier;
    private final Supplier<WildFlyRegistry> registrySupplier;
    private Consumer<MicrometerCollector> metricCollectorConsumer;

    private MicrometerCollector micrometerCollector;
    private LocalModelControllerClient modelControllerClient;

    public static void install(OperationContext context) {
        ServiceBuilder<?> serviceBuilder = context.getServiceTarget().addService(MICROMETER_COLLECTOR);
        Supplier<ModelControllerClientFactory> modelControllerClientFactory = serviceBuilder.requires(
                context.getCapabilityServiceName(CLIENT_FACTORY_CAPABILITY, ModelControllerClientFactory.class));
        Supplier<Executor> managementExecutor = serviceBuilder.requires(
                context.getCapabilityServiceName(MANAGEMENT_EXECUTOR, Executor.class));
        Supplier<ProcessStateNotifier> processStateNotifier = serviceBuilder.requires(
                context.getCapabilityServiceName(PROCESS_STATE_NOTIFIER, ProcessStateNotifier.class));
        Supplier<WildFlyRegistry> registrySupplier = serviceBuilder.requires(MICROMETER_REGISTRY_RUNTIME_CAPABILITY.getCapabilityServiceName());
        Consumer<MicrometerCollector> metricCollectorConsumer = serviceBuilder.provides(MICROMETER_COLLECTOR);
        MicrometerCollectorService service = new MicrometerCollectorService(modelControllerClientFactory, managementExecutor,
                processStateNotifier, registrySupplier, metricCollectorConsumer);
        serviceBuilder.setInstance(service)
                .install();
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

        micrometerCollector = new MicrometerCollector(modelControllerClient, processStateNotifier.get(),
                registrySupplier.get());

        metricCollectorConsumer.accept(micrometerCollector);
    }

    @Override
    public void stop(StopContext context) {
        metricCollectorConsumer.accept(null);
        micrometerCollector = null;

        modelControllerClient.close();
    }

    @Override
    public MicrometerCollector getValue() throws IllegalStateException, IllegalArgumentException {
        return micrometerCollector;
    }
}

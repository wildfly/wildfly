/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2022 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.extension.micrometer;

import static org.wildfly.extension.micrometer.MicrometerSubsystemDefinition.HTTP_EXTENSIBILITY_CAPABILITY;
import static org.wildfly.extension.micrometer.MicrometerSubsystemDefinition.MICROMETER_HTTP_CONTEXT_CAPABILITY;
import static org.wildfly.extension.micrometer.MicrometerSubsystemDefinition.MICROMETER_REGISTRY_RUNTIME_CAPABILITY;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Supplier;

import io.undertow.util.HttpString;
import org.jboss.as.controller.CapabilityServiceBuilder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.server.mgmt.domain.ExtensibleHttpManagement;
import org.jboss.msc.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.wildfly.extension.micrometer.metrics.WildFlyRegistry;

class MicrometerContextService implements Service {
    public static final String CONTEXT = "/metrics";

    private final Consumer<MicrometerContextService> consumer;
    private final Supplier<ExtensibleHttpManagement> extensibleHttpManagement;
    private final Boolean securityEnabled;
    private final Supplier<WildFlyRegistry> registrySupplier;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    static void install(OperationContext context, boolean securityEnabled) {
        CapabilityServiceBuilder<?> serviceBuilder = context.getCapabilityServiceTarget().addCapability(MICROMETER_HTTP_CONTEXT_CAPABILITY);

        Supplier<ExtensibleHttpManagement> extensibleHttpManagement =
                serviceBuilder.requiresCapability(HTTP_EXTENSIBILITY_CAPABILITY, ExtensibleHttpManagement.class);
        Supplier<WildFlyRegistry> registrySupplier =
                serviceBuilder.requiresCapability(MICROMETER_REGISTRY_RUNTIME_CAPABILITY.getName(), WildFlyRegistry.class);
        Consumer<MicrometerContextService> metricsContext =
                serviceBuilder.provides(MICROMETER_HTTP_CONTEXT_CAPABILITY.getCapabilityServiceName());
        MicrometerContextService service = new MicrometerContextService(metricsContext,
                extensibleHttpManagement,
                securityEnabled,
                registrySupplier);

        serviceBuilder.setInstance(service)
                .install();
    }

    private MicrometerContextService(Consumer<MicrometerContextService> consumer,
                                     Supplier<ExtensibleHttpManagement> extensibleHttpManagement,
                                     Boolean securityEnabled,
                                     Supplier<WildFlyRegistry> registrySupplier) {
        this.consumer = consumer;
        this.extensibleHttpManagement = extensibleHttpManagement;
        this.securityEnabled = securityEnabled;
        this.registrySupplier = registrySupplier;
    }

    @Override
    public void start(StartContext context) {
        extensibleHttpManagement.get().addManagementHandler(CONTEXT, securityEnabled, exchange -> {
            lock.readLock().lock();
            try {
                // Just a debug/testing thing for now
                exchange.getResponseHeaders().put(HttpString.tryFromString("X-WildFly-Metrics"), "micrometer");
                exchange.getResponseSender().send(registrySupplier.get().scrape());
            } finally {
                lock.readLock().unlock();
            }
        });
        consumer.accept(this);
    }

    @Override
    public void stop(StopContext context) {
        extensibleHttpManagement.get().removeContext(CONTEXT);
        consumer.accept(null);
    }
}

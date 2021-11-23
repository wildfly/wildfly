/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2021 Red Hat, Inc., and individual contributors
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

import static org.wildfly.extension.micrometer.MicrometerSubsystemDefinition.METRICS_HTTP_CONTEXT_CAPABILITY;
import static org.wildfly.extension.micrometer.MicrometerSubsystemDefinition.MICROMETER_HTTP_CONTEXT_CAPABILITY;
import static org.wildfly.extension.micrometer.MicrometerSubsystemDefinition.MICROMETER_HTTP_SECURITY_CAPABILITY;
import static org.wildfly.extension.micrometer.MicrometerSubsystemDefinition.MICROMETER_REGISTRY_RUNTIME_CAPABILITY;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Supplier;

import io.undertow.util.HttpString;
import org.jboss.as.controller.OperationContext;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.wildfly.extension.metrics.MetricsContextService;
import org.wildfly.extension.micrometer.metrics.WildFlyRegistry;

class MicrometerContextService implements Service {
    public static final String CONTEXT = "/metrics";

    private final Consumer<MicrometerContextService> consumer;
    private final Supplier<MetricsContextService> metricsContextService;
    private final Supplier<Boolean> securityEnabledSupplier;
    private final Supplier<WildFlyRegistry> registrySupplier;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    static MicrometerContextService install(OperationContext context, boolean securityEnabled) {
        ServiceBuilder<?> serviceBuilder = context.getServiceTarget().addService(MICROMETER_HTTP_CONTEXT_CAPABILITY.getCapabilityServiceName());

        Supplier<MetricsContextService> metricsContextService = serviceBuilder.requires(context.getCapabilityServiceName(METRICS_HTTP_CONTEXT_CAPABILITY, MetricsContextService.class));

        Supplier<WildFlyRegistry> registries =
                serviceBuilder.requires(context.getCapabilityServiceName(MICROMETER_REGISTRY_RUNTIME_CAPABILITY.getName(),
                        WildFlyRegistry.class));
        Consumer<MicrometerContextService> metricsContext =
                serviceBuilder.provides(MICROMETER_HTTP_CONTEXT_CAPABILITY.getCapabilityServiceName());

        final Supplier<Boolean> securityEnabledSupplier;
        if (context.getCapabilityServiceSupport().hasCapability(MICROMETER_HTTP_SECURITY_CAPABILITY)) {
            securityEnabledSupplier = serviceBuilder.requires(ServiceName.parse(MICROMETER_HTTP_SECURITY_CAPABILITY));
        } else {
            securityEnabledSupplier = () -> securityEnabled;
        }

        // ???
        MicrometerContextService service = new MicrometerContextService(metricsContext,
                metricsContextService,
                securityEnabledSupplier,
                registries);

        serviceBuilder.setInstance(service)
                .install();

        return service;
    }


    private MicrometerContextService(Consumer<MicrometerContextService> consumer,
                                     Supplier<MetricsContextService> metricsContextService,
                                     Supplier<Boolean> securityEnabledSupplier,
                                     Supplier<WildFlyRegistry> registrySupplier) {
        this.consumer = consumer;
        this.metricsContextService = metricsContextService;
        this.securityEnabledSupplier = securityEnabledSupplier;
        this.registrySupplier = registrySupplier;
    }

    @Override
    public void start(StartContext context) {
        metricsContextService.get().setOverrideableMetricHandler(exchange -> {
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
        metricsContextService.get().setOverrideableMetricHandler(null);
        registrySupplier.get().clear();
        consumer.accept(null);
    }
}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.micrometer.prometheus;

import static org.wildfly.extension.micrometer.prometheus.PrometheusRegistryDefinitionRegistrar.MICROMETER_PROMETHEUS_CONTEXT_CAPABILITY;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.server.mgmt.domain.ExtensibleHttpManagement;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2018 Red Hat inc.
 */
public class PrometheusContextService implements Service {
    static final String HTTP_EXTENSIBILITY_CAPABILITY = "org.wildfly.management.http.extensible";
    private final Consumer<PrometheusContextService> consumer;
    private final Supplier<ExtensibleHttpManagement> extensibleHttpManagement;
    private final Supplier<Boolean> securityEnabledSupplier;
    private final String prometheusContext;
    private final WildFlyPrometheusRegistry registry;

    static void install(OperationContext operationContext,
                        WildFlyPrometheusRegistry registry,
                        String prometheusContext,
                        boolean securityEnabled) {
        ServiceBuilder<?> serviceBuilder = operationContext.getCapabilityServiceTarget()
                .addCapability(MICROMETER_PROMETHEUS_CONTEXT_CAPABILITY);

        Supplier<ExtensibleHttpManagement> extensibleHttpManagement =
                serviceBuilder.requires(operationContext.getCapabilityServiceName(HTTP_EXTENSIBILITY_CAPABILITY, ExtensibleHttpManagement.class));
        Consumer<PrometheusContextService> prometheusContextConsumer =
                serviceBuilder.provides(MICROMETER_PROMETHEUS_CONTEXT_CAPABILITY.getCapabilityServiceName());
        final Supplier<Boolean> securityEnabledSupplier = () -> securityEnabled;

        Service PrometheusContextService = new PrometheusContextService(extensibleHttpManagement,
                prometheusContextConsumer, prometheusContext,
                registry, securityEnabledSupplier);

        serviceBuilder.setInstance(PrometheusContextService).install();
    }

    public PrometheusContextService(Supplier<ExtensibleHttpManagement> extensibleHttpManagement,
                                    Consumer<PrometheusContextService> consumer,
                                    String context,
                                    WildFlyPrometheusRegistry registry,
                                    Supplier<Boolean> securityEnabledSupplier) {
        this.extensibleHttpManagement = extensibleHttpManagement;
        this.consumer = consumer;
        this.prometheusContext = context;
        this.registry = registry;
        this.securityEnabledSupplier = securityEnabledSupplier;
    }

    @Override
    public void start(StartContext context) {
        extensibleHttpManagement.get().addManagementHandler(prometheusContext, securityEnabledSupplier.get(),
                exchange -> exchange.getResponseSender().send(registry.scrape()));
        consumer.accept(this);
    }

    @Override
    public void stop(StopContext context) {
        extensibleHttpManagement.get().removeContext(prometheusContext);
        consumer.accept(null);
    }
}

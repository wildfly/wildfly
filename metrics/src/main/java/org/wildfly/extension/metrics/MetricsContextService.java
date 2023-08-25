/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.metrics;

import static org.wildfly.extension.metrics.MetricsSubsystemDefinition.HTTP_EXTENSIBILITY_CAPABILITY;
import static org.wildfly.extension.metrics.MetricsSubsystemDefinition.METRICS_HTTP_CONTEXT_CAPABILITY;
import static org.wildfly.extension.metrics.MetricsSubsystemDefinition.METRICS_HTTP_SECURITY_CAPABILITY;
import static org.wildfly.extension.metrics.MetricsSubsystemDefinition.METRICS_REGISTRY_RUNTIME_CAPABILITY;

import java.util.function.Consumer;
import java.util.function.Supplier;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.server.mgmt.domain.ExtensibleHttpManagement;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2018 Red Hat inc.
 */
public class MetricsContextService implements Service {

    private static final String CONTEXT_NAME = "/metrics";

    private final Consumer<MetricsContextService> consumer;
    private final Supplier<ExtensibleHttpManagement> extensibleHttpManagement;
    private Supplier<WildFlyMetricRegistry> wildflyMetricRegistry;
    private final Supplier<Boolean> securityEnabledSupplier;
    private HttpHandler overrideableMetricHandler;

    static void install(OperationContext context, boolean securityEnabled) {
        ServiceBuilder<?> serviceBuilder = context.getServiceTarget().addService(METRICS_HTTP_CONTEXT_CAPABILITY.getCapabilityServiceName());

        Supplier<ExtensibleHttpManagement> extensibleHttpManagement = serviceBuilder.requires(context.getCapabilityServiceName(HTTP_EXTENSIBILITY_CAPABILITY, ExtensibleHttpManagement.class));
        Supplier<WildFlyMetricRegistry> wildflyMetricRegistry = serviceBuilder.requires(METRICS_REGISTRY_RUNTIME_CAPABILITY.getCapabilityServiceName());
        Consumer<MetricsContextService> metricsContext = serviceBuilder.provides(METRICS_HTTP_CONTEXT_CAPABILITY.getCapabilityServiceName());
        final Supplier<Boolean> securityEnabledSupplier;
        if (context.getCapabilityServiceSupport().hasCapability(METRICS_HTTP_SECURITY_CAPABILITY)) {
            securityEnabledSupplier = serviceBuilder.requires(ServiceName.parse(METRICS_HTTP_SECURITY_CAPABILITY));
        } else {
            securityEnabledSupplier = new Supplier<Boolean>() {
                @Override
                public Boolean get() {
                    return securityEnabled;
                }
            };
        }
        Service metricsContextService = new MetricsContextService(metricsContext, extensibleHttpManagement, wildflyMetricRegistry, securityEnabledSupplier);

        serviceBuilder.setInstance(metricsContextService)
                .install();
    }
    public MetricsContextService(Consumer<MetricsContextService> consumer, Supplier<ExtensibleHttpManagement> extensibleHttpManagement, Supplier<WildFlyMetricRegistry> wildflyMetricRegistry, Supplier<Boolean> securityEnabledSupplier) {
        this.consumer = consumer;
        this.extensibleHttpManagement = extensibleHttpManagement;
        this.wildflyMetricRegistry = wildflyMetricRegistry;
        this.securityEnabledSupplier = securityEnabledSupplier;
    }

    @Override
    public void start(StartContext context) {
        extensibleHttpManagement.get().addManagementHandler(CONTEXT_NAME, securityEnabledSupplier.get(), new HttpHandler() {
            @Override
            public void handleRequest(HttpServerExchange exchange) throws Exception {
                if (overrideableMetricHandler != null) {
                    overrideableMetricHandler.handleRequest(exchange);
                    return;
                }

                WildFlyMetricRegistry metricRegistry = wildflyMetricRegistry.get();
                metricRegistry.readLock();
                try {
                    String wildFlyMetrics = new PrometheusExporter().export(metricRegistry);
                    exchange.getResponseSender().send(wildFlyMetrics);
                } finally {
                    metricRegistry.unlock();
                }
            }
        });
        consumer.accept(this);
    }

    @Override
    public void stop(StopContext context) {
        extensibleHttpManagement.get().removeContext(CONTEXT_NAME);
        consumer.accept(null);
    }

    public void setOverrideableMetricHandler(HttpHandler handler) {
        this.overrideableMetricHandler = handler;
    }
}

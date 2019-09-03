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

package org.wildfly.extension.microprofile.metrics;

import static org.wildfly.extension.microprofile.metrics.MicroProfileMetricsSubsystemDefinition.HTTP_CONTEXT_SERVICE;

import java.io.StringWriter;
import java.util.Map;
import java.util.function.Supplier;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.common.TextFormat;
import io.smallrye.metrics.MetricsRequestHandler;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderValues;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.server.mgmt.domain.ExtensibleHttpManagement;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2018 Red Hat inc.
 */
public class MetricsContextService implements Service {

    private static final String CONTEXT_NAME = "/metrics";

    private final Supplier<ExtensibleHttpManagement> extensibleHttpManagement;
    private final boolean securityEnabled;
    private final MetricsRequestHandler metricsRequestHandler;

    static void install(OperationContext context, boolean securityEnabled) {
        ServiceBuilder<?> serviceBuilder = context.getServiceTarget().addService(HTTP_CONTEXT_SERVICE);

        Supplier<ExtensibleHttpManagement> extensibleHttpManagement = serviceBuilder.requires(context.getCapabilityServiceName(MicroProfileMetricsSubsystemDefinition.HTTP_EXTENSIBILITY_CAPABILITY, ExtensibleHttpManagement.class));

        Service healthContextService = new MetricsContextService(extensibleHttpManagement, securityEnabled, new MetricsRequestHandler());

        serviceBuilder.setInstance(healthContextService)
                .install();
    }

    MetricsContextService(Supplier<ExtensibleHttpManagement> extensibleHttpManagement, boolean securityEnabled, MetricsRequestHandler metricsRequestHandler) {
        this.extensibleHttpManagement = extensibleHttpManagement;
        this.securityEnabled = securityEnabled;
        this.metricsRequestHandler = metricsRequestHandler;
    }

    @Override
    public void start(StartContext context) {
        extensibleHttpManagement.get().addManagementHandler(CONTEXT_NAME, securityEnabled,
                new HttpHandler() {

                    @Override
                    public void handleRequest(HttpServerExchange exchange) throws Exception {
                        String requestPath = exchange.getRequestPath();
                        String method = exchange.getRequestMethod().toString();
                        HeaderValues acceptHeaders = exchange.getRequestHeaders().get(Headers.ACCEPT);
                        metricsRequestHandler.handleRequest(requestPath, method, acceptHeaders == null ? null : acceptHeaders.stream(), (status, message, headers) -> {
                            exchange.setStatusCode(status);
                            StringBuffer buffer = new StringBuffer();
                            for (Map.Entry<String, String> entry : headers.entrySet()) {
                                exchange.getResponseHeaders().put(new HttpString(entry.getKey()), entry.getValue());
                            }
                            buffer.append(message);

                            // add metrics from the subsystems only when the request is acceptable
                            if (status / 100 == 2
                                && (exchange.getRequestPath().equals(CONTEXT_NAME) ||
                                    exchange.getRequestPath().equals(CONTEXT_NAME + '/'))) {
                                String acceptHeader = exchange.getRequestHeaders().getFirst(Headers.ACCEPT);
                                boolean jsonOutput = acceptHeader != null && acceptHeader.startsWith("application/json");
                                if (!jsonOutput) {
                                    try (StringWriter sw = new StringWriter()) {
                                        CollectorRegistry registry = CollectorRegistry.defaultRegistry;
                                        TextFormat.write004(sw, registry.metricFamilySamples());
                                        buffer.append(sw.toString());
                                    }
                                }
                            }
                            exchange.getResponseSender().send(buffer.toString());
                        });
                    }
                });
    }

    @Override
    public void stop(StopContext context) {
        extensibleHttpManagement.get().removeContext(CONTEXT_NAME);
    }
}

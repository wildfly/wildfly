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


import static org.wildfly.extension.microprofile.metrics.MicroProfileMetricsSubsystemDefinition.METRICS_HTTP_CONTEXT_CAPABILITY;
import static org.wildfly.extension.microprofile.metrics.MicroProfileMetricsSubsystemDefinition.MICROPROFILE_METRIC_HTTP_CONTEXT_CAPABILITY;

import java.util.Map;
import java.util.function.Supplier;

import io.smallrye.metrics.MetricsRequestHandler;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderValues;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import org.jboss.as.controller.OperationContext;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.wildfly.extension.metrics.MetricsContextService;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2018 Red Hat inc.
 */
public class MicroProfileMetricsContextService implements Service {

    private final Supplier<MetricsContextService> metricsContextService;
    private final MetricsRequestHandler metricsRequestHandler = new MetricsRequestHandler();

    static void install(OperationContext context) {
        ServiceBuilder<?> serviceBuilder = context.getServiceTarget().addService(MICROPROFILE_METRIC_HTTP_CONTEXT_CAPABILITY.getCapabilityServiceName());

        Supplier<MetricsContextService> metricsContextService = serviceBuilder.requires(context.getCapabilityServiceName(METRICS_HTTP_CONTEXT_CAPABILITY, MetricsContextService.class));

        Service microprofileMetricsContextService = new MicroProfileMetricsContextService(metricsContextService);

        serviceBuilder.setInstance(microprofileMetricsContextService).install();
    }

    MicroProfileMetricsContextService(Supplier<MetricsContextService> metricsContextService) {
        this.metricsContextService = metricsContextService;
    }

    @Override
    public void start(StartContext context) {
        metricsContextService.get().setOverrideableMetricHandler(new HttpHandler() {
            @Override
            public void handleRequest(HttpServerExchange exchange) throws Exception {
                String requestPath = exchange.getRequestPath();
                String method = exchange.getRequestMethod().toString();
                HeaderValues acceptHeaders = exchange.getRequestHeaders().get(Headers.ACCEPT);

                metricsRequestHandler.handleRequest(requestPath, method, acceptHeaders == null ? null : acceptHeaders.stream(), (status, message, headers) -> {
                    exchange.setStatusCode(status);
                    for (Map.Entry<String, String> entry : headers.entrySet()) {
                        exchange.getResponseHeaders().put(new HttpString(entry.getKey()), entry.getValue());
                    }
                    exchange.getResponseSender().send(message);
                });
            }
        });
    }

    @Override
    public void stop(StopContext context) {
        metricsContextService.get().setOverrideableMetricHandler(null);

        MicroProfileVendorMetricRegistry.removeAllMetrics();
    }
}

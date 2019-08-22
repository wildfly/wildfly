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

package org.wildfly.extension.microprofile.health;

import static org.wildfly.extension.microprofile.health.MicroProfileHealthSubsystemDefinition.HTTP_CONTEXT_SERVICE;

import java.util.function.Supplier;

import io.smallrye.health.SmallRyeHealth;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.server.mgmt.domain.ExtensibleHttpManagement;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2018 Red Hat inc.
 */
public class HealthContextService implements Service {

    private static final String CONTEXT_NAME = "health";

    private final Supplier<ExtensibleHttpManagement> extensibleHttpManagement;
    private final boolean securityEnabled;
    private final Supplier<HealthReporter> healthReporter;


    static void install(OperationContext context, boolean securityEnabled) {
        ServiceBuilder<?> serviceBuilder = context.getServiceTarget().addService(HTTP_CONTEXT_SERVICE);

        Supplier<ExtensibleHttpManagement> extensibleHttpManagement = serviceBuilder.requires(context.getCapabilityServiceName(MicroProfileHealthSubsystemDefinition.HTTP_EXTENSIBILITY_CAPABILITY, ExtensibleHttpManagement.class));
        Supplier<HealthReporter> healthReporter = serviceBuilder.requires(context.getCapabilityServiceName(MicroProfileHealthSubsystemDefinition.HEALTH_REPORTER_CAPABILITY, HealthReporter.class));

        Service healthContextService = new HealthContextService(extensibleHttpManagement, securityEnabled, healthReporter);

        serviceBuilder.setInstance(healthContextService)
                .install();
    }

    HealthContextService(Supplier<ExtensibleHttpManagement> extensibleHttpManagement, boolean securityEnabled, Supplier<HealthReporter> healthReporter) {
        this.extensibleHttpManagement = extensibleHttpManagement;
        this.securityEnabled = securityEnabled;
        this.healthReporter = healthReporter;
    }

    @Override
    public void start(StartContext context) {
        // access to the /health endpoint is unsecured
        extensibleHttpManagement.get().addManagementHandler(CONTEXT_NAME, securityEnabled,
                new HealthCheckHandler(healthReporter.get()));
    }

    @Override
    public void stop(StopContext context) {
        extensibleHttpManagement.get().removeContext(CONTEXT_NAME);
    }

    private class HealthCheckHandler implements HttpHandler {
        private final HealthReporter healthReporter;

        public static final String HEALTH = "/" + CONTEXT_NAME;

        public static final String HEALTH_LIVE = HEALTH + "/live";

        public static final String HEALTH_READY = HEALTH + "/ready";


        public HealthCheckHandler(HealthReporter healthReporter) {
            this.healthReporter = healthReporter;
        }

        @Override
        public void handleRequest(HttpServerExchange exchange) {
            final SmallRyeHealth health;
            if (HEALTH.equals(exchange.getRequestPath())) {
                health = healthReporter.getHealth();
            } else if (HEALTH_LIVE.equals(exchange.getRequestPath())) {
                health = healthReporter.getLiveness();
            } else if (HEALTH_READY.equals(exchange.getRequestPath())) {
                health = healthReporter.getReadiness();
            } else {
                exchange.setStatusCode(404);
                return;
            }

            exchange.setStatusCode(health.isDown() ? 503 : 200)
                    .getResponseHeaders().add(Headers.CONTENT_TYPE, "application/json");
            exchange.getResponseSender().send(health.getPayload().toString());
        }
    }
}

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

package org.wildfly.extension.health;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.wildfly.extension.health.HealthSubsystemDefinition.HEALTH_HTTP_CONTEXT_CAPABILITY;
import static org.wildfly.extension.health.HealthSubsystemDefinition.HEALTH_HTTP_SECURITY_CAPABILITY;
import static org.wildfly.extension.health.HealthSubsystemDefinition.HTTP_EXTENSIBILITY_CAPABILITY;

import java.util.function.Consumer;
import java.util.function.Supplier;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.server.mgmt.domain.ExtensibleHttpManagement;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2018 Red Hat inc.
 */
public class HealthContextService implements Service {

    private static final String CONTEXT_NAME = "health";
    public static final String MICROPROFILE_HEALTH_REPORTER_CAPABILITY = "org.wildfly.extension.microprofile.health.reporter";

    private final Consumer<HealthContextService> consumer;
    private final Supplier<ExtensibleHttpManagement> extensibleHttpManagement;
    private final Supplier<Boolean> securityEnabled;
    private Supplier<ServerProbesService> serverProbesService;
    private HttpHandler overrideableHealthHandler;
    private static boolean mpHealthSubsystemActive = false;


    static void install(OperationContext context, boolean securityEnabled) {
        ServiceBuilder<?> serviceBuilder = context.getServiceTarget().addService(HEALTH_HTTP_CONTEXT_CAPABILITY.getCapabilityServiceName());

        Supplier<ExtensibleHttpManagement> extensibleHttpManagement = serviceBuilder.requires(context.getCapabilityServiceName(HTTP_EXTENSIBILITY_CAPABILITY, ExtensibleHttpManagement.class));
        Consumer<HealthContextService> consumer = serviceBuilder.provides(HEALTH_HTTP_CONTEXT_CAPABILITY.getCapabilityServiceName());
        Supplier<ServerProbesService> serverProbesService = serviceBuilder.requires(HealthSubsystemDefinition.SERVER_HEALTH_PROBES_CAPABILITY.getCapabilityServiceName());
        final Supplier<Boolean> securityEnabledSupplier;
        if (context.getCapabilityServiceSupport().hasCapability(HEALTH_HTTP_SECURITY_CAPABILITY)) {
            securityEnabledSupplier = serviceBuilder.requires(ServiceName.parse(HEALTH_HTTP_SECURITY_CAPABILITY));
        } else {
            securityEnabledSupplier = new Supplier<Boolean>() {
                @Override
                public Boolean get() {
                    return securityEnabled;
                }
            };
        }
        // check if we will be also booting the MP Health subsystem. In that case, this subsystem should not be responding.
        if (context.getCapabilityServiceSupport().hasCapability(MICROPROFILE_HEALTH_REPORTER_CAPABILITY)) {
            mpHealthSubsystemActive = true;
        }
        serviceBuilder.setInstance(new HealthContextService(extensibleHttpManagement, consumer, securityEnabledSupplier, serverProbesService))
                .install();
    }

    HealthContextService(Supplier<ExtensibleHttpManagement> extensibleHttpManagement, Consumer<HealthContextService> consumer, Supplier<Boolean> securityEnabled, Supplier<ServerProbesService> serverProbesService) {
        this.extensibleHttpManagement = extensibleHttpManagement;
        this.consumer = consumer;
        this.securityEnabled = securityEnabled;
        this.serverProbesService = serverProbesService;
    }

    @Override
    public void start(StartContext context) {
        extensibleHttpManagement.get().addManagementHandler(CONTEXT_NAME, securityEnabled.get(), new HealthCheckHandler(serverProbesService.get()));
        consumer.accept(this);
    }

    @Override
    public void stop(StopContext context) {
        extensibleHttpManagement.get().removeContext(CONTEXT_NAME);
        consumer.accept(null);
    }

    public void setOverrideableHealthHandler(HttpHandler handler) {
        this.overrideableHealthHandler = handler;
    }

    private class HealthCheckHandler implements HttpHandler{

        public static final String HEALTH = "/" + CONTEXT_NAME;
        public static final String HEALTH_LIVE = HEALTH + "/live";
        public static final String HEALTH_READY = HEALTH + "/ready";
        public static final String HEALTH_STARTED = HEALTH + "/started";
        private ServerProbesService serverProbes;

        public HealthCheckHandler(ServerProbesService serverProbesService) {
            this.serverProbes = serverProbesService;
        }

        @Override
        public void handleRequest(HttpServerExchange exchange) throws Exception {
            if (overrideableHealthHandler != null) {
                overrideableHealthHandler.handleRequest(exchange);
                return;
            }

            if (mpHealthSubsystemActive) {
                // if MP Health subsystem is present but not started yet we can't respond with management operation
                // as the clients expect JSON which is an object not an array. So respond with 404 status code to
                // not confuse consumers with wrong responses. This will still be correctly rejected in kubernetes.
                exchange.setStatusCode(404);
                return;
            }

            String requestPath = exchange.getRequestPath();
            if (!HEALTH.equals(requestPath) && !HEALTH_LIVE.equals(requestPath) &&
                !HEALTH_READY.equals(requestPath) && !HEALTH_STARTED.equals(requestPath)) {
                exchange.setStatusCode(404);
                return;
            }

            boolean globalOutcome = true;
            ModelNode response = new ModelNode();
            response.setEmptyList();
            if (HEALTH.equals(requestPath) || HEALTH_READY.equals(requestPath)) {
                for (ServerProbe serverProbe : serverProbes.getServerProbes()) {
                    ServerProbe.Outcome outcome = serverProbe.getOutcome();
                    if (!outcome.isSuccess()) {
                        globalOutcome = false;
                    }
                    ModelNode probeOutcome = new ModelNode();
                    probeOutcome.get(NAME).set(serverProbe.getName());
                    probeOutcome.get(OUTCOME).set(outcome.isSuccess());
                    if (outcome.getData().isDefined()) {
                        probeOutcome.get("data").set(outcome.getData());
                    }
                    response.add(probeOutcome);
                }
            }
            if (HEALTH.equals(requestPath) || HEALTH_LIVE.equals(requestPath)) {
                // always respond to the /health/live positively
                ModelNode probeOutcome = new ModelNode();
                probeOutcome.get(NAME).set("live-server");
                probeOutcome.get(OUTCOME).set(true);
                response.add(probeOutcome);
            }
            if (HEALTH.equals(requestPath) || HEALTH_STARTED.equals(requestPath)) {
                // always respond to the /health/started positively
                ModelNode probeOutcome = new ModelNode();
                probeOutcome.get(NAME).set("started-server");
                probeOutcome.get(OUTCOME).set(true);
                response.add(probeOutcome);
            }
            response.add(OUTCOME, globalOutcome);

            buildHealthResponse(exchange, globalOutcome ? 200: 503, response.toJSONString(true));
        }

        private void buildHealthResponse(HttpServerExchange exchange, int status, String response) {
            exchange.setStatusCode(status)
                .getResponseHeaders().add(Headers.CONTENT_TYPE, "application/json");
            exchange.getResponseSender().send(response);
        }
    }
}

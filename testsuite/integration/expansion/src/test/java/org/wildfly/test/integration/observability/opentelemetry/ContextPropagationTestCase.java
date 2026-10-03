/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.observability.opentelemetry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response;
import org.arquillian.testcontainers.api.TestcontainersRequired;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.shared.observability.setuptasks.OpenTelemetryWithCollectorSetupTask;
import org.jboss.as.test.shared.observability.signals.jaeger.JaegerSpan;
import org.jboss.as.test.shared.observability.signals.jaeger.JaegerTrace;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.wildfly.test.integration.observability.opentelemetry.application.OtelService2;

/**
 * This test exercises the context propagation functionality. Two services are deployed, with the first calling the
 * second. The second service attempts to retrieve the trace propagation header and record it in a span attribute. The
 * test then retrieves the traces from the Jaeger container and verifies that all spans produced belong to the same trace.
 */
@RunAsClient
@ServerSetup({OpenTelemetryWithCollectorSetupTask.class})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestcontainersRequired
public class ContextPropagationTestCase extends BaseOpenTelemetryTest {

    private static final String DEPLOYMENT_SERVICE1 = "service1";
    private static final String DEPLOYMENT_SERVICE2 = "service2";

    @ArquillianResource
    private Deployer deployer;

    @Deployment(name = DEPLOYMENT_SERVICE1, managed = false, testable = false)
    public static WebArchive getDeployment1() {
        return buildBaseArchive(DEPLOYMENT_SERVICE1);
    }

    @Deployment(name = DEPLOYMENT_SERVICE2, managed = false, testable = false)
    public static WebArchive getDeployment2() {
        return buildBaseArchive(DEPLOYMENT_SERVICE2).addClass(OtelService2.class);
    }

    @Test
    @Order(1)
    void deploy() {
        deployer.deploy(DEPLOYMENT_SERVICE1);
        deployer.deploy(DEPLOYMENT_SERVICE2);
    }

    @Test
    @Order(2)
    void contextPropagation() throws Exception {
        try (Client client = ClientBuilder.newClient()) {
            Response response = client.target(getDeploymentUrl(DEPLOYMENT_SERVICE1) + "contextProp1")
                    .request().get();
            assertEquals(204, response.getStatus());

            otelCollector.assertTraces(DEPLOYMENT_SERVICE1 + ".war", traces -> {
                assertFalse(traces.isEmpty(), "Traces not found for service");

                JaegerTrace trace = traces.get(0);
                String traceId = trace.getTraceID();
                List<JaegerSpan> spans = trace.getSpans();

                spans.forEach(s ->
                    assertEquals(traceId,
                        s.getTraceID(), "The traceId of the span did not match the first span's. Context propagation failed."));
            });
        }
    }

    @Test
    @Order(3)
    void undeploy() {
        deployer.undeploy(DEPLOYMENT_SERVICE2);
    }
}

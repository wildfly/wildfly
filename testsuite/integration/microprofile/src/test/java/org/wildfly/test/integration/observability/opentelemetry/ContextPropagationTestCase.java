/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.observability.opentelemetry;

import java.net.URI;
import java.util.List;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.shared.IntermittentFailure;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.observability.container.OpenTelemetryCollectorContainer;
import org.wildfly.test.integration.observability.opentelemetry.application.OtelService2;
import org.wildfly.test.integration.observability.opentelemetry.jaeger.JaegerSpan;
import org.wildfly.test.integration.observability.opentelemetry.jaeger.JaegerTrace;
import org.wildfly.test.integration.observability.setuptask.OpenTelemetrySetupTask;

/**
 * This test exercises the context propagation functionality. Two services are deployed, with the first calling the
 * second. The second service attempts to retrieve the trace propagation header and record it in a span attribute. The
 * test then retrieves the traces from the Jaeger container and verifies that all spans produced belong to the same trace.
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({OpenTelemetrySetupTask.class})
public class ContextPropagationTestCase extends BaseOpenTelemetryTest {

    @BeforeClass
    public static void disableForWFLY19509() {
        IntermittentFailure.thisTestIsFailingIntermittently("WFLY-19509");
    }

    @ArquillianResource
    private Deployer deployer;

    @Deployment(name = "service1")
    public static WebArchive getDeployment1() {
        return buildBaseArchive("service1");
    }

    @Deployment(name = "service2", managed = false)
    public static WebArchive getDeployment2() {
        return buildBaseArchive("service2").addClass(OtelService2.class);
    }

    @Test
    @InSequence(1)
    public void deploy() {
        deployer.deploy("service2");
    }

    @Test
    @InSequence(2)
    public void testContextPropagation() throws InterruptedException {
        try (Client client = ClientBuilder.newClient()) {
            Response response = client.target(URI.create(url + "contextProp1")).request().get();
            Assert.assertEquals(204, response.getStatus());

            List<JaegerTrace> traces = OpenTelemetryCollectorContainer.getInstance()
                    .getTraces("service1.war");
            Assert.assertFalse("Traces not found for service", traces.isEmpty());

            JaegerTrace trace = traces.get(0);
            String traceId = trace.getTraceID();
            List<JaegerSpan> spans = trace.getSpans();

            spans.forEach(s -> {
                Assert.assertEquals("The traceId of the span did not match the first span's. Context propagation failed.",
                        traceId, s.getTraceID());
            });
        }
    }

    @Test
    @InSequence(3)
    public void undeploy() {
        deployer.undeploy("service2");
    }
}

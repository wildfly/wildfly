/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.observability.opentelemetry;

import java.net.URI;
import java.util.List;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;

import io.opentelemetry.sdk.trace.data.SpanData;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * This test exercises the context propagation functionality. Two services are deployed, with the first calling the
 * second. The second service attempts to retrieve the trace propagation header and return it. The first returns a JSON
 * object containing the traceparent header value and the traceId. We then query the Jaeger collector, started using the
 * ClassRule, to verify that the trace was successfully exported.
 */
@RunWith(Arquillian.class)
@ServerSetup(OpenTelemetrySetupTask.class)
public class ContextPropagationTestCase extends BaseOpenTelemetryTest {

    @Deployment
    public static Archive getDeployment() {
        return buildBaseArchive(ContextPropagationTestCase.class.getSimpleName());
    }

    @Test
    public void testContextPropagation() {
        String contextPropUrl = url.toString() + "/contextProp1";
        try (Client client = ClientBuilder.newClient()) {
            client.target(URI.create(contextPropUrl))
                    .request()
                    .get();
            // 6 Expected spans named:
            // Recording traceparent
            // /ContextPropagationTestCase/contextProp2
            // HTTP GET
            // Making second request
            // /ContextPropagationTestCase/contextProp1
            // HTTP GET
            List<SpanData> finishedSpans = spanExporter.getFinishedSpanItems(6);

            SpanData lastSpan = finishedSpans.get(finishedSpans.size() - 1);
            String traceId = lastSpan.getSpanContext().getTraceId();

            finishedSpans.forEach(s -> {
                Assert.assertEquals("The traceId of the span did not match the first span's. Context propagation failed.",
                        traceId, s.getSpanContext().getTraceId());
            });
        }
    }
}

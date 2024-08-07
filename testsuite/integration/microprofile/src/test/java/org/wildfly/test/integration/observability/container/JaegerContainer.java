/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.observability.container;

import java.util.List;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import org.junit.Assert;
import org.wildfly.test.integration.observability.opentelemetry.jaeger.JaegerResponse;
import org.wildfly.test.integration.observability.opentelemetry.jaeger.JaegerTrace;

/*
 * This class is really intended to be called ONLY from OpenTelemetryCollectorContainer. Any test working with
 * tracing data should be passing through the otel collector and any methods on its Container.
 */
class JaegerContainer extends BaseContainer<JaegerContainer> {
    public static final String IMAGE_NAME = "jaegertracing/all-in-one";
    public static final String IMAGE_VERSION = "1.53.0";
    public static final int PORT_JAEGER_QUERY = 16686;
    public static final int PORT_JAEGER_OTLP = 4317;

    public JaegerContainer() {
        super("Jaeger", IMAGE_NAME, IMAGE_VERSION, List.of(PORT_JAEGER_QUERY, PORT_JAEGER_OTLP));
        withNetworkAliases("jaeger")
                .withEnv("COLLECTOR_OTLP_ENABLED", "true");
    }

    @Override
    public void start() {
        super.start();
        debugLog("Query port: " + getMappedPort(PORT_JAEGER_QUERY));
        debugLog("OTLP port: " + getMappedPort(PORT_JAEGER_OTLP));
        debugLog("port bindings: " + getPortBindings());
    }

    public List<JaegerTrace> getTraces(String serviceName) throws InterruptedException {
        try (Client client = ClientBuilder.newClient()) {
            waitForDataToAppear(serviceName);
            return client.target(getJaegerEndpoint() + "/api/traces?service=" + serviceName).request()
                    .get()
                    .readEntity(JaegerResponse.class)
                    .getData();
        }
    }

    private String getJaegerEndpoint() {
        return "http://localhost:" + getMappedPort(PORT_JAEGER_QUERY);
    }

    private void waitForDataToAppear(String serviceName) {
        try (Client client = ClientBuilder.newClient()) {
            boolean found = false;
            int count = 0;
            while (count < 30) {
                String response = client.target(getJaegerEndpoint() + "/api/services").request()
                        .get()
                        .readEntity(String.class);
                if (response.contains(serviceName)) {
                    found = true;
                    break;
                }
                count++;
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    //
                }
            }

            Assert.assertTrue("Expected service name not found", found);
        }
    }
}

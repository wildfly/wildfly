/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.observability.container;

import java.util.List;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;

import org.junit.Assert;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.wildfly.common.annotation.NotNull;
import org.wildfly.test.integration.observability.opentelemetry.jaeger.JaegerResponse;
import org.wildfly.test.integration.observability.opentelemetry.jaeger.JaegerTrace;

/*
 * This class is really intended to be called ONLY from OpenTelemetryCollectorContainer. Any test working with
 * tracing data should be passing through the otel collector and any methods on its Container.
 */
class JaegerContainer extends BaseContainer<JaegerContainer> {
    private static JaegerContainer INSTANCE = null;

    public static final int PORT_JAEGER_QUERY = 16686;
    public static final int PORT_JAEGER_OTLP = 4317;

    private String jaegerEndpoint;

    private JaegerContainer() {
        super("Jaeger", "jaegertracing/all-in-one", "latest",
                List.of(PORT_JAEGER_QUERY, PORT_JAEGER_OTLP),
                List.of(Wait.forHttp("/").forPort(PORT_JAEGER_QUERY)));
    }

    @NotNull
    public static synchronized JaegerContainer getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new JaegerContainer()
                    .withNetwork(Network.SHARED)
                    .withNetworkAliases("jaeger")
                    .withEnv("JAEGER_DISABLED", "true");
            INSTANCE.start();
        }

        return INSTANCE;
    }

    @Override
    public void start() {
        super.start();
        jaegerEndpoint = "http://localhost:" + getMappedPort(PORT_JAEGER_QUERY);
    }

    @Override
    public synchronized void stop() {
        INSTANCE = null;
        super.stop();
    }

    List<JaegerTrace> getTraces(String serviceName) throws InterruptedException {
        try (Client client = ClientBuilder.newClient()) {
            waitForDataToAppear(serviceName);
            String jaegerUrl = jaegerEndpoint + "/api/traces?service=" + serviceName;
            JaegerResponse jaegerResponse = client.target(jaegerUrl).request().get().readEntity(JaegerResponse.class);
            return jaegerResponse.getData();
        }
    }

    private void waitForDataToAppear(String serviceName) {
        try (Client client = ClientBuilder.newClient()) {
            String uri = jaegerEndpoint + "/api/services";
            WebTarget target = client.target(uri);
            boolean found = false;
            int count = 0;
            while (count < 10) {
                String response = target.request().get().readEntity(String.class);
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

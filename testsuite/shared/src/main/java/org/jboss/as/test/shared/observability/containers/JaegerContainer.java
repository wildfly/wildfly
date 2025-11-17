/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.shared.observability.containers;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import org.jboss.as.test.shared.observability.signals.jaeger.JaegerResponse;
import org.jboss.as.test.shared.observability.signals.jaeger.JaegerTrace;

/*
 * This class is really intended to be called ONLY from OpenTelemetryCollectorContainer. Any test working with
 * tracing data should be passing through the otel collector and any methods on its Container.
 */
class JaegerContainer extends BaseContainer<JaegerContainer> {
    public static final String IMAGE_NAME = "jaegertracing/all-in-one";
    public static final String IMAGE_VERSION = "1.64.0";
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
        return getTraces(serviceName, null);
    }

    public List<JaegerTrace> getTraces(String serviceName, Map<String, String> tags) throws InterruptedException {
        StringBuilder url = new StringBuilder(getJaegerEndpoint() + "/api/traces?service=" + serviceName);

        if (tags != null && !tags.isEmpty()) {
            // Jaeger expects tags parameter as JSON: tags={"key":"value","key2":"value2"}
            StringBuilder jsonTags = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<String, String> entry : tags.entrySet()) {
                if (!first) {
                    jsonTags.append(",");
                }
                jsonTags.append("\"").append(entry.getKey()).append("\":\"").append(entry.getValue()).append("\"");
                first = false;
            }
            jsonTags.append("}");

            // URL encode the JSON string
            String encodedTags = URLEncoder.encode(jsonTags.toString(), StandardCharsets.UTF_8);
            url.append("&tags=").append(encodedTags);
        }

        try (Client client = ClientBuilder.newClient()) {
            return client.target(url.toString()).request()
                    .get()
                    .readEntity(JaegerResponse.class)
                    .getData();
        }
    }

    private String getJaegerEndpoint() {
        return "http://localhost:" + getMappedPort(PORT_JAEGER_QUERY);
    }
}

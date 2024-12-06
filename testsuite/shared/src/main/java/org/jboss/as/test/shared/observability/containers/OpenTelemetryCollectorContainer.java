/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.shared.observability.containers;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import org.jboss.as.test.shared.observability.signals.PrometheusMetric;
import org.jboss.as.test.shared.observability.signals.jaeger.JaegerTrace;
import org.testcontainers.utility.MountableFile;

public class OpenTelemetryCollectorContainer extends BaseContainer<OpenTelemetryCollectorContainer> {
    public static final String IMAGE_NAME = "otel/opentelemetry-collector";
    public static final String IMAGE_VERSION = "0.103.1";
    public static final int OTLP_GRPC_PORT = 4317;
    public static final int OTLP_HTTP_PORT = 4318;
    public static final int PROMETHEUS_PORT = 1234;
    public static final int HEALTH_CHECK_PORT = 13133;
    public static final String OTEL_COLLECTOR_CONFIG_YAML = "/etc/otel-collector-config.yaml";

    private JaegerContainer jaegerContainer;

    public OpenTelemetryCollectorContainer() {
        super("OpenTelemetryCollector", IMAGE_NAME, IMAGE_VERSION,
                List.of(OTLP_GRPC_PORT, OTLP_HTTP_PORT, HEALTH_CHECK_PORT, PROMETHEUS_PORT));
        withCopyToContainer(
                MountableFile.forClasspathResource(
                        getClass().getPackageName().replace(".", "/") +
                                "/otel-collector-config.yaml"),
                OpenTelemetryCollectorContainer.OTEL_COLLECTOR_CONFIG_YAML
        );
        withCommand("--config " + OpenTelemetryCollectorContainer.OTEL_COLLECTOR_CONFIG_YAML);
        jaegerContainer = new JaegerContainer();
    }

    @Override
    public void start() {
        super.start();
        jaegerContainer.start();

        debugLog("OTLP gRPC port: " + getMappedPort(OTLP_GRPC_PORT));
        debugLog("OTLP HTTP port: " + getMappedPort(OTLP_HTTP_PORT));
        debugLog("Prometheus port: " + getMappedPort(PROMETHEUS_PORT));
        debugLog("port bindings: " + getPortBindings());
    }

    @Override
    public synchronized void stop() {
        jaegerContainer.stop();
        super.stop();
    }

    public String getOtlpGrpcEndpoint() {
        return "http://localhost:" + getMappedPort(OTLP_GRPC_PORT);
    }

    public String getOtlpHttpEndpoint() {
        return "http://localhost:" + getMappedPort(OTLP_HTTP_PORT);
    }

    public String getPrometheusUrl() {
        return "http://localhost:" + getMappedPort(PROMETHEUS_PORT) + "/metrics";
    }

    public List<JaegerTrace> getTraces(String serviceName) throws InterruptedException {
        return (jaegerContainer != null ? jaegerContainer.getTraces(serviceName) : Collections.emptyList());
    }

    public List<PrometheusMetric> fetchMetrics(String nameToMonitor) throws InterruptedException {
        String body = "";
        try (Client client = ClientBuilder.newClient()) {
            WebTarget target = client.target(getPrometheusUrl());

            int attemptCount = 0;
            boolean found = false;

            // Request counts can vary. Setting high to help ensure test stability
            while (!found && attemptCount < 30) {
                // Wait to give Micrometer time to export
                Thread.sleep(1000);

                body = target.request().get().readEntity(String.class);
                found = body.contains(nameToMonitor);
                attemptCount++;
            }
        }

        return buildPrometheusMetrics(body);
    }

    private List<PrometheusMetric> buildPrometheusMetrics(String body) {
        if (body.isEmpty()) {
            return Collections.emptyList();
        }
        String[] entries = body.split("\n");
        Map<String, String> help = new HashMap<>();
        Map<String, String> type = new HashMap<>();
        List<PrometheusMetric> metrics = new LinkedList<>();
        Arrays.stream(entries).forEach(e -> {
            if (e.startsWith("# HELP")) {
                extractMetadata(help, e);
            } else if (e.startsWith("# TYPE")) {
                extractMetadata(type, e);
            } else {
                String[] parts = e.split("[{}]");
                String key = parts[0];
                Map<String, String> tags = Arrays.stream(parts[1].split(","))
                        .map(t -> t.split("="))
                        .collect(Collectors.toMap(i -> i[0],
                                i -> i[1]
                                        .replaceAll("^\"", "")
                                        .replaceAll("\"$", "")
                        ));
                metrics.add(new PrometheusMetric(key, tags, parts[2].trim(), type.get(key), help.get(key)));
            }
        });

        return metrics;
    }

    private void extractMetadata(Map<String, String> target, String source) {
        String[] parts = source.split(" ");
        target.put(parts[2],
                Arrays.stream(Arrays.copyOfRange(parts, 3, parts.length))
                        .reduce("", (total, element) -> total + " " + element));
    }
}

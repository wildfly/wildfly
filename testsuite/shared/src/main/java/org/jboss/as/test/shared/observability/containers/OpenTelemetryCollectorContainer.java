/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.shared.observability.containers;

import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.as.test.shared.observability.signals.PrometheusMetric;
import org.jboss.as.test.shared.observability.signals.jaeger.JaegerTrace;
import org.junit.Assert;
import org.testcontainers.utility.MountableFile;

/**
 * @author Jason Lee
 * @author Radoslav Husar
 */
public class OpenTelemetryCollectorContainer extends BaseContainer<OpenTelemetryCollectorContainer> {
    public static final String IMAGE_NAME = "otel/opentelemetry-collector";
    public static final String IMAGE_VERSION = "0.115.1";
    public static final int OTLP_GRPC_PORT = 4317;
    public static final int OTLP_HTTP_PORT = 4318;
    public static final int PROMETHEUS_PORT = 1234;
    public static final int HEALTH_CHECK_PORT = 13133;
    public static final String OTEL_COLLECTOR_CONFIG_YAML = "/etc/otel-collector-config.yaml";

    private final JaegerContainer jaegerContainer;
    private final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(
            TimeoutUtil.adjust(Integer.parseInt(System.getProperty("testsuite.integration.container.timeout", "30"))));

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
        return jaegerContainer.getTraces(serviceName);
    }

    /**
     * Given a list of <code>PrometheusMetric</code> instances, return a sublist whose key matches <code>key</code>. Note
     * that the key name must match the Prometheus conventions (see <a href="https://prometheus.io/docs/practices/naming/">
     * here</a> for details.
     * @param metrics List of PrometheusMetrics to filter.
     * @param key The name of the metric to find
     * @return a sublist of <code>metrics</code> that matches <code>key</code>
     */
    public List<PrometheusMetric> getMetricsByName(List<PrometheusMetric> metrics, String key) {
        return metrics.stream()
            .filter(m -> Objects.equals(m.getKey(), key))
            .toList();
    }


    /**
     * Continually evaluates assertions provided in a consumer until the state obtained from the Jaeger endpoint
     * matches the expected state or until a timeout elapses. By default, polls the collector every second for 30 seconds.
     * Returns snapshot of the Jaeger traces that passed the assertions; typically ignored.
     *
     * @param assertionConsumer consumer implementation that contains {@link Assert}ions throwing
     *                          {@link AssertionError#AssertionError()}s if the state obtained from the Jaeger endpoint
     *                          does not match the expected state
     * @return list of Jaeger traces; typically ignored.
     * @throws AssertionError       last {@link AssertionError} thrown by the provided {@code assertionConsumer} before timeout elapsed
     * @throws InterruptedException if interrupted
     */
    public List<JaegerTrace> assertTraces(String serviceName, Consumer<List<JaegerTrace>> assertionConsumer) throws InterruptedException {
        return assertTraces(serviceName, assertionConsumer, DEFAULT_TIMEOUT);
    }

    /**
     * Variant of {@link OpenTelemetryCollectorContainer#assertTraces(String, Consumer)} that can be configured with a
     * timeout duration.
     */
    public List<JaegerTrace> assertTraces(String serviceName, Consumer<List<JaegerTrace>> assertionConsumer, Duration timeout) throws InterruptedException {
        debugLog("assertTraces(...) validation starting.");
        Instant endTime = Instant.now().plus(timeout);
        AssertionError lastAssertionError = null;

        while (Instant.now().isBefore(endTime)) {
            try {
                List<JaegerTrace> traces = jaegerContainer.getTraces(serviceName);
                assertionConsumer.accept(traces);
                debugLog("assertTraces(...) validation passed.");
                return traces;
            } catch (AssertionError assertionError) {
                debugLog("assertTraces(...) validation failed - retrying.");
                lastAssertionError = assertionError;
                Thread.sleep(1000);
            }
        }

        throw Objects.requireNonNullElseGet(lastAssertionError, AssertionError::new);
    }

    /**
     * Continually evaluates assertions provided in a consumer until the state obtained from the Prometheus endpoint
     * matches the expected state or until a timeout elapses.
     * By default, polls the collector every second for 30 seconds.
     * Typically {@code otel.metric.export.interval} must be adjusted to lower values since the default is attempting
     * to push every 60 seconds, which is well above the default timeout of this method.
     * Returns snapshot of the prometheus registry that passed the assertions; typically ignored.
     *
     * @param assertionConsumer consumer implementation that contains {@link Assert}ions throwing {@link AssertionError#AssertionError()}s
     *                          if the state obtained from the Prometheus endpoint does not match the expected state
     * @return list of prometheus metrics; typically ignored.
     * @throws AssertionError       last {@link AssertionError} thrown by the provided {@code assertionConsumer} before timeout elapsed
     * @throws InterruptedException if interrupted
     */
    public List<PrometheusMetric> assertMetrics(Consumer<List<PrometheusMetric>> assertionConsumer) throws AssertionError, InterruptedException {
        return this.assertMetrics(assertionConsumer, DEFAULT_TIMEOUT);
    }

    /**
     * Variant of {@link OpenTelemetryCollectorContainer#assertMetrics(Consumer)} that can be configured with a timeout duration.
     */
    public List<PrometheusMetric> assertMetrics(Consumer<List<PrometheusMetric>> assertionConsumer, Duration timeout) throws AssertionError, InterruptedException {
        debugLog("assertMetrics(..) validation starting.");

        Instant endTime = Instant.now().plus(timeout);

        AssertionError lastAssertionError = null;

        while (Instant.now().isBefore(endTime)) {
            try {
                List<PrometheusMetric> prometheusMetrics = fetchMetrics();

                assertionConsumer.accept(prometheusMetrics);

                debugLog("assertMetrics(..) validation passed.");

                return prometheusMetrics;
            } catch (AssertionError assertionError) {
                debugLog("assertMetrics(..) validation failed - retrying.");
                lastAssertionError = assertionError;
                Thread.sleep(1000);
            }
        }

        throw Objects.requireNonNullElseGet(lastAssertionError, AssertionError::new);
    }

    /**
     * Fetches a current snapshot of the metrics from the Prometheus endpoint.
     *
     * @return list of prometheus metrics
     */
    public List<PrometheusMetric> fetchMetrics() {
        try (Client client = ClientBuilder.newClient()) {
            WebTarget target = client.target(getPrometheusUrl());
            return buildPrometheusMetrics(target.request().get().readEntity(String.class));
        }
    }

    /**
     * @deprecated Use {@link OpenTelemetryCollectorContainer#assertMetrics(Consumer)} instead.
     */
    @Deprecated
    public List<PrometheusMetric> fetchMetrics(String nameToMonitor) throws InterruptedException {
        return assertMetrics(prometheusMetrics -> {
            assertTrue(
                    String.format("Metric %s not seen in Prometheus within timeout.", nameToMonitor),
                    prometheusMetrics.stream().anyMatch(x -> x.getKey().contains(nameToMonitor))
            );
        });
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

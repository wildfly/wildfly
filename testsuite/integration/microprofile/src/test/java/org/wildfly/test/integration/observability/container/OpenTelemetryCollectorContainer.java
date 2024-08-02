/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.observability.container;

import java.util.Collections;
import java.util.List;

import org.testcontainers.utility.MountableFile;
import org.wildfly.test.integration.observability.opentelemetry.jaeger.JaegerTrace;

public class OpenTelemetryCollectorContainer extends BaseContainer<OpenTelemetryCollectorContainer> {
    public static final String IMAGE_NAME = "otel/opentelemetry-collector";
    public static final String IMAGE_VERSION = "0.93.0";
    public static final int OTLP_GRPC_PORT = 4317;
    public static final int OTLP_HTTP_PORT = 4318;
    public static final int PROMETHEUS_PORT = 49152;
    public static final int HEALTH_CHECK_PORT = 13133;
    public static final String OTEL_COLLECTOR_CONFIG_YAML = "/etc/otel-collector-config.yaml";

    private JaegerContainer jaegerContainer;

    private String otlpGrpcEndpoint;
    private String otlpHttpEndpoint;
    private String prometheusUrl;

    public OpenTelemetryCollectorContainer() {
        super("OpenTelemetryCollector", IMAGE_NAME, IMAGE_VERSION,
                List.of(OTLP_GRPC_PORT, OTLP_HTTP_PORT, HEALTH_CHECK_PORT, PROMETHEUS_PORT));
        withCopyToContainer(
                MountableFile.forClasspathResource("org/wildfly/test/integration/observability/container/otel-collector-config.yaml"),
                OpenTelemetryCollectorContainer.OTEL_COLLECTOR_CONFIG_YAML)
                .withCommand("--config " + OpenTelemetryCollectorContainer.OTEL_COLLECTOR_CONFIG_YAML);
        jaegerContainer = new JaegerContainer();
    }

    @Override
    public void start() {
        super.start();
        jaegerContainer.start();

        otlpGrpcEndpoint = "http://localhost:" + getMappedPort(OTLP_GRPC_PORT);
        otlpHttpEndpoint = "http://localhost:" + getMappedPort(OTLP_HTTP_PORT);
        prometheusUrl = "http://localhost:" + getMappedPort(PROMETHEUS_PORT) + "/metrics";

        debugLog("OTLP gRPC port: " + getMappedPort(OTLP_GRPC_PORT));
        debugLog("OTLP HTTP port: " + getMappedPort(OTLP_HTTP_PORT));
        debugLog("Prometheus port: " + getMappedPort(PROMETHEUS_PORT));
        debugLog("port bindings: " + getPortBindings());
    }

    @Override
    public synchronized void stop() {
        if (jaegerContainer != null) {
            jaegerContainer.stop();
            jaegerContainer = null;
        }
        super.stop();
    }

    public String getOtlpGrpcEndpoint() {
        return otlpGrpcEndpoint;
    }

    public String getOtlpHttpEndpoint() {
        return otlpHttpEndpoint;
    }

    public String getPrometheusUrl() {
        return prometheusUrl;
    }

    public List<JaegerTrace> getTraces(String serviceName) throws InterruptedException {
        return (jaegerContainer != null ? jaegerContainer.getTraces(serviceName) : Collections.emptyList());
    }
}

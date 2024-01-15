/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.observability.container;

import java.util.Collections;
import java.util.List;

import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;
import org.wildfly.common.annotation.NotNull;
import org.wildfly.test.integration.observability.opentelemetry.jaeger.JaegerTrace;

public class OpenTelemetryCollectorContainer extends BaseContainer<OpenTelemetryCollectorContainer> {
    private static OpenTelemetryCollectorContainer INSTANCE = null;
    private static JaegerContainer jaegerContainer;

    public static final int OTLP_GRPC_PORT = 4317;
    public static final int OTLP_HTTP_PORT = 4318;
    public static final int PROMETHEUS_PORT = 49152;
    public static final int HEALTH_CHECK_PORT = 13133;

    public static final String OTEL_COLLECTOR_CONFIG_YAML = "/etc/otel-collector-config.yaml";

    private OpenTelemetryCollectorContainer() {
        super("OpenTelemetryCollector",
                "otel/opentelemetry-collector",
                "0.93.0",
                List.of(OTLP_GRPC_PORT, OTLP_HTTP_PORT, HEALTH_CHECK_PORT, PROMETHEUS_PORT),
                List.of(Wait.forHttp("/").forPort(HEALTH_CHECK_PORT)));
    }

    @NotNull
    public static synchronized OpenTelemetryCollectorContainer getInstance() {
        if (INSTANCE == null) {
            jaegerContainer = JaegerContainer.getInstance();

            INSTANCE = new OpenTelemetryCollectorContainer()
                    .withNetwork(Network.SHARED)
                    .withCopyToContainer(MountableFile.forClasspathResource(
                                    "org/wildfly/test/integration/observability/container/otel-collector-config.yaml"),
                            OpenTelemetryCollectorContainer.OTEL_COLLECTOR_CONFIG_YAML)
                    .withCommand("--config " + OpenTelemetryCollectorContainer.OTEL_COLLECTOR_CONFIG_YAML);
            INSTANCE.start();
        }
        return INSTANCE;
    }

    @Override
    public void start() {
        super.start();
    }

    @Override
    public void stop() {
        synchronized(this) {
            INSTANCE = null;
            if (jaegerContainer != null) {
                jaegerContainer.stop();
            }
            super.stop();
        }
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
}

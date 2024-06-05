/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.observability.micrometer;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;
import org.testcontainers.shaded.com.google.common.collect.Lists;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

public class OpenTelemetryCollectorContainer extends GenericContainer<OpenTelemetryCollectorContainer> {
    public static final int HTTP_OTLP_PORT = 4318;
    public static final int PROMETHEUS_PORT = 49152;
    public static final int HEALTH_CHECK_PORT = 13133;
    public static final String OTEL_COLLECTOR_CONFIG_YAML = "/etc/otel-collector-config.yaml";
    private static final String imageName = "otel/opentelemetry-collector";
    private static final String imageVersion = "0.87.0";
    private static final int STARTUP_ATTEMPTS = Integer.parseInt(
            System.getProperty("testsuite.integration.otelcollector.container.startup.attempts", "5"));
    private static final Duration ATTEMPT_DURATION = Duration.parse(
            System.getProperty("testsuite.integration.otelcollector.container.attempt.duration", "PT30S"));

    private String otlpEndpoint;
    private String prometheusUrl;

    public OpenTelemetryCollectorContainer() {
        super(DockerImageName.parse(imageName + ":" + imageVersion));
        setExposedPorts(Lists.newArrayList(HTTP_OTLP_PORT, HEALTH_CHECK_PORT, PROMETHEUS_PORT));
        setWaitStrategy(
                new WaitAllStrategy()
                        .withStrategy(Wait.forHttp("/").forPort(HEALTH_CHECK_PORT))
                        .withStrategy(Wait.forHttp("/metrics").forPort(PROMETHEUS_PORT))
                        .withStrategy(Wait.forHttp("/v1/metrics").forPort(HTTP_OTLP_PORT).forStatusCode(405))
                        .withStartupTimeout(ATTEMPT_DURATION)
        );
        setStartupAttempts(STARTUP_ATTEMPTS);
    }

    @Override
    public OpenTelemetryCollectorContainer withExposedPorts(Integer... ports) {
        getExposedPorts().addAll(Lists.newArrayList(ports));
        return this;
    }

    @Override
    public void start() {
        super.start();
        otlpEndpoint = "http://" + getHost() + ":" + getMappedPort(HTTP_OTLP_PORT) + "/v1/metrics";
        prometheusUrl = "http://" + getHost() + ":" + getMappedPort(PROMETHEUS_PORT) + "/metrics";
    }

    public String getOtlpEndpoint() {
        return otlpEndpoint;
    }

    public String getPrometheusUrl() {
        return prometheusUrl;
    }
}

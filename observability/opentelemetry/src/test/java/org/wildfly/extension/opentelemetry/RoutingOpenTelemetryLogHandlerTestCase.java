/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.opentelemetry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.opentelemetry.sdk.logs.export.SimpleLogRecordProcessor;
import io.opentelemetry.sdk.resources.Resource;
import org.junit.Test;

/** Unit tests for class-loader-aware OpenTelemetry log routing. */
public class RoutingOpenTelemetryLogHandlerTestCase {
    private static final AttributeKey<String> SERVICE_NAME = AttributeKey.stringKey("service.name");

    /** Verifies a deployment context class loader selects the deployment logger provider. */
    @Test
    public void routesLogsToDeploymentResource() {
        CapturingExporter serverExporter = new CapturingExporter();
        CapturingExporter deploymentExporter = new CapturingExporter();
        SdkLoggerProvider serverProvider = loggerProvider("server", serverExporter);
        SdkLoggerProvider deploymentProvider = loggerProvider("deployment", deploymentExporter);
        RoutingOpenTelemetryLogHandler handler = new RoutingOpenTelemetryLogHandler(openTelemetry(serverProvider));
        ClassLoader deploymentClassLoader = new ClassLoader() { };
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();

        try {
            handler.register(deploymentClassLoader, openTelemetry(deploymentProvider));
            Thread.currentThread().setContextClassLoader(deploymentClassLoader);
            handler.publish(new LogRecord(Level.INFO, "deployment log"));

            assertTrue("Deployment log must not reach the server exporter", serverExporter.records.isEmpty());
            assertEquals("Deployment log must be routed to the deployment logger provider",
                    "deployment",
                    deploymentExporter.records.get(0).getResource().getAttribute(SERVICE_NAME));
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
            handler.close();
            deploymentProvider.close();
            serverProvider.close();
        }
    }

    /** Verifies unregistering a deployment restores the server logger fallback. */
    @Test
    public void unregisterRestoresServerFallback() {
        CapturingExporter serverExporter = new CapturingExporter();
        CapturingExporter deploymentExporter = new CapturingExporter();
        SdkLoggerProvider serverProvider = loggerProvider("server", serverExporter);
        SdkLoggerProvider deploymentProvider = loggerProvider("deployment", deploymentExporter);
        RoutingOpenTelemetryLogHandler handler = new RoutingOpenTelemetryLogHandler(openTelemetry(serverProvider));
        ClassLoader deploymentClassLoader = new ClassLoader() { };
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();

        try {
            handler.register(deploymentClassLoader, openTelemetry(deploymentProvider));
            handler.unregister(deploymentClassLoader);
            Thread.currentThread().setContextClassLoader(deploymentClassLoader);
            handler.publish(new LogRecord(Level.INFO, "server log"));

            assertTrue("Unregistered deployment must not receive logs", deploymentExporter.records.isEmpty());
            assertEquals("After unregister, logs must fall back to the server logger provider",
                    "server", serverExporter.records.get(0).getResource().getAttribute(SERVICE_NAME));
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
            handler.close();
            deploymentProvider.close();
            serverProvider.close();
        }
    }

    /**
     * Creates a logger provider with a service-name resource and synchronous test exporter.
     *
     * @param serviceName the resource service name
     * @param exporter the exporter that captures emitted records
     * @return the configured logger provider
     */
    private static SdkLoggerProvider loggerProvider(String serviceName, LogRecordExporter exporter) {
        return SdkLoggerProvider.builder()
                .setResource(Resource.create(Attributes.of(SERVICE_NAME, serviceName)))
                .addLogRecordProcessor(SimpleLogRecordProcessor.create(exporter))
                .build();
    }

    /**
     * Wraps a logger provider in an OpenTelemetry SDK for routing tests.
     *
     * @param loggerProvider the provider to expose
     * @return the configured SDK
     */
    private static OpenTelemetry openTelemetry(SdkLoggerProvider loggerProvider) {
        return OpenTelemetrySdk.builder().setLoggerProvider(loggerProvider).build();
    }

    /** Captures exported log records in memory for assertions. */
    private static final class CapturingExporter implements LogRecordExporter {
        private final List<LogRecordData> records = new ArrayList<>();

        /** {@inheritDoc} */
        @Override
        public CompletableResultCode export(Collection<LogRecordData> logs) {
            records.addAll(logs);
            return CompletableResultCode.ofSuccess();
        }

        /** {@inheritDoc} */
        @Override
        public CompletableResultCode flush() {
            return CompletableResultCode.ofSuccess();
        }

        /** {@inheritDoc} */
        @Override
        public CompletableResultCode shutdown() {
            return CompletableResultCode.ofSuccess();
        }
    }
}

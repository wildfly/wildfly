/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.opentelemetry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import org.junit.Test;

/**
 * Regression tests for deployment-specific OpenTelemetry providers and lifecycle handling.
 */
public class OpenTelemetryServiceTestCase {

    /** Verifies deployments use the subsystem's configured sampler. */
    @Test
    public void testDeploymentTracerProviderUsesConfiguredSampler() {
        SdkTracerProvider tracerProvider = OpenTelemetryService.createDeploymentTracerProvider(
                Resource.empty(), Sampler.alwaysOff(), List.of());

        Span span = tracerProvider.get("test").spanBuilder("not-recorded").startSpan();

        assertFalse("Span should not be recording when the deployment sampler is alwaysOff", span.isRecording());
        tracerProvider.close();
    }

    /** Verifies deployment shutdown flushes but does not close server-owned span processors. */
    @Test
    public void testDeploymentTracerProviderUsesConfiguredSpanProcessorWithoutClosingIt() {
        SpanProcessor spanProcessor = mock(SpanProcessor.class);
        when(spanProcessor.isEndRequired()).thenReturn(true);
        when(spanProcessor.forceFlush()).thenReturn(CompletableResultCode.ofSuccess());
        SdkTracerProvider tracerProvider = OpenTelemetryService.createDeploymentTracerProvider(
                Resource.empty(), Sampler.alwaysOn(), List.of(spanProcessor));

        Span span = tracerProvider.get("test").spanBuilder("recorded").startSpan();
        assertTrue("Span should be recording when the deployment sampler is alwaysOn", span.isRecording());
        span.end();
        tracerProvider.close();

        verify(spanProcessor).onEnd(any());
        verify(spanProcessor).forceFlush();
        verify(spanProcessor, never()).shutdown();
    }

    /** Verifies undeployment serializes and awaits the final export before removing the reader. */
    @Test
    public void testUnregisterDeploymentMetricReaderExportsPendingMetrics() throws Exception {
        MetricData metric = mock(MetricData.class);
        DeploymentMetricReader reader = mock(DeploymentMetricReader.class);
        when(reader.collectAllMetrics()).thenReturn(List.of()).thenReturn(List.of(metric));
        AggregatingMetricExporter.DeploymentMetricReaderHandle handle =
                new AggregatingMetricExporter.DeploymentMetricReaderHandle(reader, "deployment");
        Map<String, AggregatingMetricExporter.DeploymentMetricReaderHandle> readers = new ConcurrentHashMap<>();
        readers.put("deployment", handle);
        MetricExporter downstream = mock(MetricExporter.class);
        CompletableResultCode periodicExport = new CompletableResultCode();
        CompletableResultCode finalExport = new CompletableResultCode();
        when(downstream.export(any())).thenReturn(periodicExport, finalExport);
        AggregatingMetricExporter exporter = new AggregatingMetricExporter(downstream, () -> readers);

        exporter.export(List.of());
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<?> unregister = executor.submit(() ->
                    OpenTelemetryService.unregisterDeploymentMetricReader("deployment", null, readers, exporter));

            verify(downstream, after(100).times(1)).export(any());
            assertTrue("Reader must remain registered while the in-flight periodic export completes",
                    readers.containsKey("deployment"));

            periodicExport.succeed();
            verify(downstream, timeout(1000).times(2)).export(any());
            assertTrue("Reader must remain registered until the final export has been triggered",
                    readers.containsKey("deployment"));
            assertFalse("Unregistration must block until the final export succeeds", unregister.isDone());

            finalExport.succeed();
            unregister.get(1, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }

        assertFalse("Reader must be removed once the final export has completed", readers.containsKey("deployment"));
        verify(downstream).export(argThat(metrics -> metrics.size() == 1 && metrics.contains(metric)));
    }

    /** Verifies a stale undeployment cannot remove a newer reader registered under the same name. */
    @Test
    public void testStaleUndeploymentDoesNotRemoveReplacementReader() {
        DeploymentMetricReader oldReader = mock(DeploymentMetricReader.class);
        DeploymentMetricReader replacementReader = mock(DeploymentMetricReader.class);
        AggregatingMetricExporter.DeploymentMetricReaderHandle oldHandle =
                new AggregatingMetricExporter.DeploymentMetricReaderHandle(oldReader, "deployment");
        AggregatingMetricExporter.DeploymentMetricReaderHandle replacementHandle =
                new AggregatingMetricExporter.DeploymentMetricReaderHandle(replacementReader, "deployment");
        Map<String, AggregatingMetricExporter.DeploymentMetricReaderHandle> readers = new ConcurrentHashMap<>();
        readers.put("deployment", replacementHandle);
        AggregatingMetricExporter exporter = mock(AggregatingMetricExporter.class);

        OpenTelemetryService.unregisterDeploymentMetricReader("deployment", oldHandle, readers, exporter);

        assertSame("Stale undeployment must not remove the replacement reader",
                replacementHandle, readers.get("deployment"));
        verify(exporter, never()).exportDeployment(any());
    }

    /** Verifies deployment resource attributes use the standard OpenTelemetry parser. */
    @Test
    public void testDeploymentConfigResourceIncludesCustomAttributes() {
        Resource resource = OpenTelemetryDeploymentProcessor.createDeploymentConfigResource(Map.of(
                "otel.service.name", "deployment-service",
                "otel.resource.attributes", "custom.key=custom%20value"));

        assertEquals("Configured service name should populate the service.name attribute",
                "deployment-service", resource.getAttribute(AttributeKey.stringKey("service.name")));
        assertEquals("Custom resource attribute value should be URL-decoded",
                "custom value", resource.getAttribute(AttributeKey.stringKey("custom.key")));
    }

    /** Verifies custom deployment attributes survive all resource merges. */
    @Test
    public void testDeploymentResourceMergesDeploymentAttributes() {
        Resource serverResource = Resource.create(Attributes.of(AttributeKey.stringKey("server.key"), "server"));
        Resource deploymentResource = Resource.create(Attributes.builder()
                .put("service.name", "deployment-service")
                .put("custom.key", "deployment")
                .build());

        Resource resource = OpenTelemetryService.createDeploymentResource(
                serverResource, "configured-service", "deployment.war", "default-service", deploymentResource);

        assertEquals("Server resource attributes should be preserved through the merge",
                "server", resource.getAttribute(AttributeKey.stringKey("server.key")));
        assertEquals("Deployment attribute should take precedence over the server value",
                "deployment", resource.getAttribute(AttributeKey.stringKey("custom.key")));
        assertEquals("Deployment service.name should override the server value",
                "deployment-service", resource.getAttribute(AttributeKey.stringKey("service.name")));
        assertEquals("deployment.name should be derived from the deployment unit name",
                "deployment.war", resource.getAttribute(AttributeKey.stringKey("deployment.name")));
    }
}

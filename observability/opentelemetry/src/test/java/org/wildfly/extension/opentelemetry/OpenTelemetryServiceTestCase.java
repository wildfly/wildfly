/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.opentelemetry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.logs.LoggerProvider;
import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.TracerBuilder;
import io.opentelemetry.api.trace.TracerProvider;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.common.export.MemoryMode;
import io.opentelemetry.sdk.metrics.Aggregation;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import org.junit.Test;
import org.wildfly.extension.opentelemetry.api.WildFlyOpenTelemetryConfig;
import org.wildfly.extension.opentelemetry.exporters.AggregatingMetricExporter;
import org.wildfly.extension.opentelemetry.service.OpenTelemetryService;
import org.wildfly.extension.opentelemetry.service.RoutingOpenTelemetry;

/**
 * Regression tests for deployment-specific OpenTelemetry providers and lifecycle handling.
 */
public class OpenTelemetryServiceTestCase {

    /** Verifies a connector tracer created before CDI can use the deployment provider registered later. */
    @Test
    public void testCachedGlobalTracerUsesLaterDeploymentProvider() {
        RoutingOpenTelemetry router = new RoutingOpenTelemetry(OpenTelemetry.noop());
        ClassLoader deploymentClassLoader = Thread.currentThread().getContextClassLoader();
        Tracer tracer = router.getTracerProvider().tracerBuilder("connector").build();
        CountingTracerProvider tracerProvider = new CountingTracerProvider(
                SdkTracerProvider.builder().setSampler(Sampler.alwaysOn()).build());
        CountingOpenTelemetry deploymentTelemetry = new CountingOpenTelemetry(tracerProvider);
        router.register(deploymentClassLoader, deploymentTelemetry);

        try {
            Span span = tracer.spanBuilder("message receive").startSpan();
            assertTrue("The cached tracer must use the deployment provider", span.isRecording());
            span.end();
            Span secondSpan = tracer.spanBuilder("second message").startSpan();
            secondSpan.end();
            assertEquals("The delegate tracer must be cached", 1, tracerProvider.tracerBuilderCalls);
        } finally {
            router.unregister(deploymentClassLoader);
            tracerProvider.close();
        }
    }

    /** Verifies cleanup for one service cannot remove a replacement service's router registration. */
    @Test
    public void testRouterCleanupIsScopedToOwner() {
        OpenTelemetry fallback = OpenTelemetry.noop();
        MeterProvider fallbackMeterProvider = fallback.getMeterProvider();
        RoutingOpenTelemetry router = new RoutingOpenTelemetry(fallback);
        ClassLoader firstClassLoader = new ClassLoader() { };
        ClassLoader replacementClassLoader = new ClassLoader() { };
        Object firstOwner = new Object();
        Object replacementOwner = new Object();
        SdkMeterProvider replacementMeterProvider = SdkMeterProvider.builder().build();
        OpenTelemetrySdk replacement = OpenTelemetrySdk.builder()
                .setMeterProvider(replacementMeterProvider)
                .build();

        try {
            router.register(firstClassLoader, replacement, firstOwner);
            router.register(replacementClassLoader, replacement, replacementOwner);
            router.unregisterAll(firstOwner);

            ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(firstClassLoader);
                assertSame("The first owner's registration must be removed", fallbackMeterProvider,
                        router.getMeterProvider());
                Thread.currentThread().setContextClassLoader(replacementClassLoader);
                assertNotSame("A replacement owner's registration must remain", fallbackMeterProvider,
                        router.getMeterProvider());
            } finally {
                Thread.currentThread().setContextClassLoader(originalClassLoader);
            }
        } finally {
            replacement.close();
        }
    }

    /** Verifies deployment cleanup does not wait for a previous metric export to finish. */
    @Test
    public void testDeploymentMetricCleanupDoesNotBlockOnExport() throws Exception {
        DeploymentMetricReader reader = new DeploymentMetricReader(AggregationTemporality.DELTA) {
            @Override
            public Collection<MetricData> collectAllMetrics() {
                return List.of();
            }
        };
        AggregatingMetricExporter.DeploymentMetricReaderHandle handle =
                new AggregatingMetricExporter.DeploymentMetricReaderHandle(reader, "deployment");
        Map<String, AggregatingMetricExporter.DeploymentMetricReaderHandle> readers = new ConcurrentHashMap<>();
        readers.put("deployment", handle);
        BlockingMetricExporter downstream = new BlockingMetricExporter();
        AggregatingMetricExporter exporter = new AggregatingMetricExporter(downstream, () -> readers);
        exporter.export(List.of());

        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<CompletableResultCode> cleanup = executor.submit(() -> exporter.exportDeployment(handle));
            assertFalse("Cleanup must not wait for an in-flight export", cleanup.isDone());
            downstream.periodicExport.succeed();
            CompletableResultCode finalExport = cleanup.get(1, TimeUnit.SECONDS);
            assertSame("Cleanup should return the final export result", downstream.finalExport, finalExport);
            assertFalse("Cleanup must not wait for the final export", finalExport.isDone());
            downstream.finalExport.succeed();
        } finally {
            executor.shutdownNow();
        }
    }

    /** Verifies deployment logger providers share one batching processor. */
    @Test
    public void testDeploymentLoggerProvidersShareBatchProcessor() throws Exception {
        WildFlyOpenTelemetryConfig.Builder configBuilder = new WildFlyOpenTelemetryConfig.Builder();
        Field builderProperties = WildFlyOpenTelemetryConfig.Builder.class.getDeclaredField("properties");
        builderProperties.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, String> properties = (Map<String, String>) builderProperties.get(configBuilder);
        properties.put("otel.sdk.disabled", "true");
        OpenTelemetryService service = new OpenTelemetryService(configBuilder.build(), null);
        Field exporterField = OpenTelemetryService.class.getDeclaredField("logRecordExporter");
        exporterField.setAccessible(true);
        exporterField.set(service, new NoopLogRecordExporter());

        try {
            SdkLoggerProvider first = service.createDeploymentLoggerProvider(Resource.empty());
            SdkLoggerProvider second = service.createDeploymentLoggerProvider(Resource.empty());
            Field processorField = OpenTelemetryService.class.getDeclaredField("deploymentLogRecordProcessor");
            processorField.setAccessible(true);
            Object processor = processorField.get(service);

            assertNotNull("Deployment log batching must be initialized", processor);
            assertSame("Deployment providers must share one batch processor", processor,
                    processorField.get(service));
            first.close();
            second.close();
        } finally {
            service.shutdown();
        }
    }

    /** Verifies deployments use the subsystem's configured sampler. */
    @Test
    public void testDeploymentTracerProviderUsesConfiguredSampler() {
        SdkTracerProvider tracerProvider = OpenTelemetryService.createDeploymentTracerProvider(
                Resource.empty(), Sampler.alwaysOff(), List.of());

        Span span = tracerProvider.get("test").spanBuilder("not-recorded").startSpan();

        assertFalse("Span should not be recording when the deployment sampler is alwaysOff", span.isRecording());
        tracerProvider.close();
    }

    /** Verifies rebuilding the subsystem service does not register the global router twice. */
    @Test
    public void testServiceCanBeRebuiltAfterRestart() {
        WildFlyOpenTelemetryConfig config = mock(WildFlyOpenTelemetryConfig.class);
        when(config.properties()).thenReturn(Map.of("otel.sdk.disabled", "true"));
        OpenTelemetryService first = new OpenTelemetryService(config, null);
        try {
            OpenTelemetryService second = new OpenTelemetryService.Builder().config(config).build();
            second.shutdown();
        } finally {
            first.shutdown();
        }
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

    /** Verifies undeployment starts the final export without waiting for its completion. */
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

            unregister.get(1, TimeUnit.SECONDS);
            verify(downstream, times(2)).export(any());
            assertFalse("Unregistration must remove the reader without waiting for export completion",
                    readers.containsKey("deployment"));
            periodicExport.succeed();
            finalExport.succeed();
        } finally {
            executor.shutdownNow();
        }

        assertFalse("Reader must be removed before the final export has completed", readers.containsKey("deployment"));
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

    /** Supplies controllable export results for lifecycle timing tests. */
    private static final class BlockingMetricExporter implements MetricExporter {
        private final CompletableResultCode periodicExport = new CompletableResultCode();
        private final CompletableResultCode finalExport = new CompletableResultCode();
        private int exportCount;

        /** {@inheritDoc} */
        @Override
        public CompletableResultCode export(Collection<MetricData> metrics) {
            return exportCount++ == 0 ? periodicExport : finalExport;
        }

        /** {@inheritDoc} */
        @Override
        public AggregationTemporality getAggregationTemporality(InstrumentType instrumentType) {
            return AggregationTemporality.DELTA;
        }

        /** {@inheritDoc} */
        @Override
        public MemoryMode getMemoryMode() {
            return MemoryMode.REUSABLE_DATA;
        }

        /** {@inheritDoc} */
        @Override
        public Aggregation getDefaultAggregation(InstrumentType instrumentType) {
            return Aggregation.defaultAggregation();
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

    /** Accepts log records without exporting them for lifecycle tests. */
    private static final class NoopLogRecordExporter implements LogRecordExporter {
        /** {@inheritDoc} */
        @Override
        public CompletableResultCode export(Collection<LogRecordData> logs) {
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

    /** Counts delegate tracer construction while preserving the real provider behavior. */
    private static final class CountingTracerProvider implements TracerProvider {
        private final TracerProvider delegate;
        private int tracerBuilderCalls;

        /** Creates a counting wrapper around a tracer provider. */
        private CountingTracerProvider(TracerProvider delegate) {
            this.delegate = delegate;
        }

        /** {@inheritDoc} */
        @Override
        public Tracer get(String instrumentationScopeName) {
            return delegate.get(instrumentationScopeName);
        }

        /** {@inheritDoc} */
        @Override
        public Tracer get(String instrumentationScopeName, String instrumentationScopeVersion) {
            return delegate.get(instrumentationScopeName, instrumentationScopeVersion);
        }

        /** {@inheritDoc} */
        @Override
        public TracerBuilder tracerBuilder(String instrumentationScopeName) {
            tracerBuilderCalls++;
            return delegate.tracerBuilder(instrumentationScopeName);
        }

        /** Closes the SDK provider used by this test wrapper. */
        private void close() {
            ((SdkTracerProvider) delegate).close();
        }
    }

    /** Exposes the counting tracer provider through the OpenTelemetry routing seam. */
    private static final class CountingOpenTelemetry implements OpenTelemetry {
        private final CountingTracerProvider tracerProvider;
        private final OpenTelemetry noop = OpenTelemetry.noop();

        /** Creates a telemetry view backed by the counting tracer provider. */
        private CountingOpenTelemetry(CountingTracerProvider tracerProvider) {
            this.tracerProvider = tracerProvider;
        }

        /** {@inheritDoc} */
        @Override
        public TracerProvider getTracerProvider() {
            return tracerProvider;
        }

        /** {@inheritDoc} */
        @Override
        public LoggerProvider getLogsBridge() {
            return noop.getLogsBridge();
        }

        /** {@inheritDoc} */
        @Override
        public MeterProvider getMeterProvider() {
            return noop.getMeterProvider();
        }

        /** {@inheritDoc} */
        @Override
        public ContextPropagators getPropagators() {
            return noop.getPropagators();
        }
    }
}

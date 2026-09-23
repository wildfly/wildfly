/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.opentelemetry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import org.junit.Test;

/**
 * Unit tests for AggregatingMetricExporter
 */
public class AggregatingMetricExporterTestCase {

    @Test
    public void testExportAggregatesFromMultipleReaders() {
        // Create mock downstream exporter
        MetricExporter mockDownstream = mock(MetricExporter.class);
        when(mockDownstream.export(any())).thenReturn(CompletableResultCode.ofSuccess());

        // Create deployment readers with test data
        Map<String, AggregatingMetricExporter.DeploymentMetricReaderHandle> readers = new HashMap<>();

        DeploymentMetricReader reader1 = new DeploymentMetricReader(AggregationTemporality.DELTA);
        DeploymentMetricReader reader2 = new DeploymentMetricReader(AggregationTemporality.DELTA);

        readers.put("deployment1", new AggregatingMetricExporter.DeploymentMetricReaderHandle(reader1, "deployment1"));
        readers.put("deployment2", new AggregatingMetricExporter.DeploymentMetricReaderHandle(reader2, "deployment2"));

        // Create aggregating exporter
        AggregatingMetricExporter exporter = new AggregatingMetricExporter(mockDownstream, () -> readers);

        // Create test metric data for server
        MetricData serverMetric = createTestMetric("server.metric", "server-deployment");

        // Export server metrics
        CompletableResultCode result = exporter.export(List.of(serverMetric));

        // Verify export succeeded
        assertTrue(result.isSuccess());

        // Verify downstream was called
        verify(mockDownstream).export(any());
    }

    @Test
    public void testExportWithEmptyDeploymentReaders() {
        MetricExporter mockDownstream = mock(MetricExporter.class);
        when(mockDownstream.export(any())).thenReturn(CompletableResultCode.ofSuccess());

        Map<String, AggregatingMetricExporter.DeploymentMetricReaderHandle> readers = new HashMap<>();
        AggregatingMetricExporter exporter = new AggregatingMetricExporter(mockDownstream, () -> readers);

        MetricData serverMetric = createTestMetric("server.metric", "server");
        CompletableResultCode result = exporter.export(List.of(serverMetric));

        assertTrue("Export should succeed with no deployment readers", result.isSuccess());
        verify(mockDownstream).export(any());
    }

    @Test
    public void testGetAggregationTemporality() {
        MetricExporter mockDownstream = mock(MetricExporter.class);
        when(mockDownstream.getAggregationTemporality(any()))
            .thenReturn(AggregationTemporality.CUMULATIVE);

        AggregatingMetricExporter exporter = new AggregatingMetricExporter(
            mockDownstream, HashMap::new);

        AggregationTemporality temporality = exporter.getAggregationTemporality(InstrumentType.COUNTER);
        assertEquals("Should delegate to downstream", AggregationTemporality.CUMULATIVE, temporality);
    }

    @Test
    public void testFlushDelegatesToDownstream() {
        MetricExporter mockDownstream = mock(MetricExporter.class);
        when(mockDownstream.flush()).thenReturn(CompletableResultCode.ofSuccess());

        AggregatingMetricExporter exporter = new AggregatingMetricExporter(
            mockDownstream, HashMap::new);

        CompletableResultCode result = exporter.flush();
        assertTrue(result.isSuccess());
        verify(mockDownstream).flush();
    }

    @Test
    public void testShutdownDelegatesToDownstream() {
        MetricExporter mockDownstream = mock(MetricExporter.class);
        when(mockDownstream.shutdown()).thenReturn(CompletableResultCode.ofSuccess());

        AggregatingMetricExporter exporter = new AggregatingMetricExporter(
            mockDownstream, HashMap::new);

        CompletableResultCode result = exporter.shutdown();
        assertTrue(result.isSuccess());
        verify(mockDownstream).shutdown();
    }

    @Test
    public void testExportHandlesReaderException() {
        MetricExporter mockDownstream = mock(MetricExporter.class);
        when(mockDownstream.export(any())).thenReturn(CompletableResultCode.ofSuccess());

        // Create a reader that throws on collect
        DeploymentMetricReader failingReader = new DeploymentMetricReader(AggregationTemporality.DELTA) {
            @Override
            Collection<MetricData> collectAllMetrics() {
                throw new RuntimeException("Test exception");
            }
        };

        Map<String, AggregatingMetricExporter.DeploymentMetricReaderHandle> readers = new HashMap<>();
        readers.put("failing", new AggregatingMetricExporter.DeploymentMetricReaderHandle(
            failingReader, "failing-deployment"));

        AggregatingMetricExporter exporter = new AggregatingMetricExporter(mockDownstream, () -> readers);

        MetricData serverMetric = createTestMetric("server.metric", "server");
        CompletableResultCode result = exporter.export(List.of(serverMetric));

        // Export should still succeed even if one reader fails
        assertTrue("Export should succeed despite reader exception", result.isSuccess());
        verify(mockDownstream).export(any());
    }

    private MetricData createTestMetric(String name, String deployment) {
        return mock(MetricData.class);
    }
}

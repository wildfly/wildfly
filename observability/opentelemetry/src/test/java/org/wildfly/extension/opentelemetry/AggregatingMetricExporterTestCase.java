/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.opentelemetry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
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
 * Unit tests for {@link AggregatingMetricExporter}.
 */
public class AggregatingMetricExporterTestCase {

    /** Verifies one downstream export contains server and deployment metrics. */
    @Test
    public void testExportAggregatesFromMultipleReaders() {
        MetricExporter mockDownstream = mock(MetricExporter.class);
        when(mockDownstream.export(any())).thenReturn(CompletableResultCode.ofSuccess());
        MetricData serverMetric = mock(MetricData.class);
        MetricData deploymentMetric1 = mock(MetricData.class);
        MetricData deploymentMetric2 = mock(MetricData.class);
        DeploymentMetricReader reader1 = mock(DeploymentMetricReader.class);
        DeploymentMetricReader reader2 = mock(DeploymentMetricReader.class);
        when(reader1.collectAllMetrics()).thenReturn(List.of(deploymentMetric1));
        when(reader2.collectAllMetrics()).thenReturn(List.of(deploymentMetric2));
        Map<String, AggregatingMetricExporter.DeploymentMetricReaderHandle> readers = new HashMap<>();
        readers.put("deployment1", new AggregatingMetricExporter.DeploymentMetricReaderHandle(reader1, "deployment1"));
        readers.put("deployment2", new AggregatingMetricExporter.DeploymentMetricReaderHandle(reader2, "deployment2"));
        AggregatingMetricExporter exporter = new AggregatingMetricExporter(mockDownstream, () -> readers);

        CompletableResultCode result = exporter.export(List.of(serverMetric));

        assertTrue("Aggregated export of server and deployment metrics should succeed", result.isSuccess());
        verify(mockDownstream).export(argThat(metrics -> metrics.size() == 3
                && metrics.containsAll(List.of(serverMetric, deploymentMetric1, deploymentMetric2))));
    }

    /** Verifies server metrics are exported unchanged when no deployment readers exist. */
    @Test
    public void testExportWithEmptyDeploymentReaders() {
        MetricExporter mockDownstream = mock(MetricExporter.class);
        when(mockDownstream.export(any())).thenReturn(CompletableResultCode.ofSuccess());

        Map<String, AggregatingMetricExporter.DeploymentMetricReaderHandle> readers = new HashMap<>();
        AggregatingMetricExporter exporter = new AggregatingMetricExporter(mockDownstream, () -> readers);

        MetricData serverMetric = mock(MetricData.class);
        CompletableResultCode result = exporter.export(List.of(serverMetric));

        assertTrue("Export should succeed with no deployment readers", result.isSuccess());
        verify(mockDownstream).export(argThat(metrics -> metrics.equals(List.of(serverMetric))));
    }

    /** Verifies aggregation temporality selection is delegated to the downstream exporter. */
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

    /** Verifies flush requests are delegated to the downstream exporter. */
    @Test
    public void testFlushDelegatesToDownstream() {
        MetricExporter mockDownstream = mock(MetricExporter.class);
        when(mockDownstream.flush()).thenReturn(CompletableResultCode.ofSuccess());

        AggregatingMetricExporter exporter = new AggregatingMetricExporter(
            mockDownstream, HashMap::new);

        CompletableResultCode result = exporter.flush();
        assertTrue("Flush should succeed when delegated to the downstream exporter", result.isSuccess());
        verify(mockDownstream).flush();
    }

    /** Verifies shutdown requests are delegated to the downstream exporter. */
    @Test
    public void testShutdownDelegatesToDownstream() {
        MetricExporter mockDownstream = mock(MetricExporter.class);
        when(mockDownstream.shutdown()).thenReturn(CompletableResultCode.ofSuccess());

        AggregatingMetricExporter exporter = new AggregatingMetricExporter(
            mockDownstream, HashMap::new);

        CompletableResultCode result = exporter.shutdown();
        assertTrue("Shutdown should succeed when delegated to the downstream exporter", result.isSuccess());
        verify(mockDownstream).shutdown();
    }

    /** Verifies one failing deployment reader does not prevent exporting other metrics. */
    @Test
    public void testExportHandlesReaderException() {
        MetricExporter mockDownstream = mock(MetricExporter.class);
        when(mockDownstream.export(any())).thenReturn(CompletableResultCode.ofSuccess());

        // Create a reader that throws on collect
        DeploymentMetricReader failingReader = new DeploymentMetricReader(AggregationTemporality.DELTA) {
            /** {@inheritDoc} */
            @Override
            Collection<MetricData> collectAllMetrics() {
                throw new RuntimeException("Test exception");
            }
        };

        Map<String, AggregatingMetricExporter.DeploymentMetricReaderHandle> readers = new HashMap<>();
        readers.put("failing", new AggregatingMetricExporter.DeploymentMetricReaderHandle(
            failingReader, "failing-deployment"));

        AggregatingMetricExporter exporter = new AggregatingMetricExporter(mockDownstream, () -> readers);

        MetricData serverMetric = mock(MetricData.class);
        CompletableResultCode result = exporter.export(List.of(serverMetric));

        // Export should still succeed even if one reader fails
        assertTrue("Export should succeed despite reader exception", result.isSuccess());
        verify(mockDownstream).export(any());
    }
}

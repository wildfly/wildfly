/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.opentelemetry;

import static org.wildfly.extension.opentelemetry.OpenTelemetryExtensionLogger.OTEL_LOGGER;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.common.export.MemoryMode;
import io.opentelemetry.sdk.metrics.Aggregation;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.export.AggregationTemporalitySelector;
import io.opentelemetry.sdk.metrics.export.DefaultAggregationSelector;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.metrics.export.MetricReader;

/**
 * Aggregates metrics from server and all deployment-specific MetricReaders,
 * then exports them via a downstream exporter (typically OTLP).
 */
class AggregatingMetricExporter implements MetricExporter {
    private final MetricExporter downstream;
    private final Supplier<Map<String, DeploymentMetricReaderHandle>> deploymentReadersSupplier;

    AggregatingMetricExporter(MetricExporter downstream,
                             Supplier<Map<String, DeploymentMetricReaderHandle>> deploymentReadersSupplier) {
        this.downstream = downstream;
        this.deploymentReadersSupplier = deploymentReadersSupplier;
    }

    @Override
    public CompletableResultCode export(Collection<MetricData> serverMetrics) {
        List<MetricData> allMetrics = new ArrayList<>(serverMetrics);

        // Collect from all deployment readers
        for (DeploymentMetricReaderHandle handle : deploymentReadersSupplier.get().values()) {
            try {
                if (handle.reader instanceof DeploymentMetricReader) {
                    allMetrics.addAll(((DeploymentMetricReader) handle.reader).collectAllMetrics());
                }
            } catch (Exception e) {
                OTEL_LOGGER.warnf("Failed to collect metrics from deployment %s: %s",
                    handle.deploymentName, e.getMessage());
            }
        }

        return downstream.export(allMetrics);
    }

    @Override
    public AggregationTemporality getAggregationTemporality(InstrumentType instrumentType) {
        if (downstream instanceof AggregationTemporalitySelector) {
            return ((AggregationTemporalitySelector) downstream).getAggregationTemporality(instrumentType);
        }
        return AggregationTemporality.CUMULATIVE;
    }

    @Override
    public MemoryMode getMemoryMode() {
        return downstream.getMemoryMode();
    }

    @Override
    public Aggregation getDefaultAggregation(InstrumentType instrumentType) {
        if (downstream instanceof DefaultAggregationSelector) {
            return ((DefaultAggregationSelector) downstream).getDefaultAggregation(instrumentType);
        }
        return Aggregation.defaultAggregation();
    }

    @Override
    public CompletableResultCode flush() {
        return downstream.flush();
    }

    @Override
    public CompletableResultCode shutdown() {
        return downstream.shutdown();
    }

    static class DeploymentMetricReaderHandle {
        final MetricReader reader;
        final String deploymentName;

        DeploymentMetricReaderHandle(MetricReader reader, String deploymentName) {
            this.reader = reader;
            this.deploymentName = deploymentName;
        }
    }
}

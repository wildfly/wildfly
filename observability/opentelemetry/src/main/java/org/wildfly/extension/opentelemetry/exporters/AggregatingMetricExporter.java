/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.opentelemetry.exporters;

import static org.wildfly.extension.opentelemetry.OpenTelemetryExtensionLogger.OTEL_LOGGER;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.common.export.MemoryMode;
import io.opentelemetry.sdk.metrics.Aggregation;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import org.wildfly.extension.opentelemetry.DeploymentMetricReader;

/**
 * Aggregates metrics from server and all deployment-specific MetricReaders,
 * then exports them via a downstream exporter (typically OTLP).
 */
public class AggregatingMetricExporter implements MetricExporter {
    private static final long EXPORT_TIMEOUT_SECONDS = 30;

    private final MetricExporter downstream;
    private final Supplier<Map<String, DeploymentMetricReaderHandle>> deploymentReadersSupplier;
    private CompletableResultCode lastExport = CompletableResultCode.ofSuccess();

    /**
     * Creates an exporter that adds deployment metrics to every server export.
     *
     * @param downstream the configured server exporter
     * @param deploymentReadersSupplier supplies the currently active deployment readers
     */
    public AggregatingMetricExporter(MetricExporter downstream,
                             Supplier<Map<String, DeploymentMetricReaderHandle>> deploymentReadersSupplier) {
        this.downstream = downstream;
        this.deploymentReadersSupplier = deploymentReadersSupplier;
    }

    /** {@inheritDoc} */
    @Override
    public synchronized CompletableResultCode export(Collection<MetricData> serverMetrics) {
        List<MetricData> allMetrics = new ArrayList<>(serverMetrics);

        // Collect from all deployment readers
        for (DeploymentMetricReaderHandle handle : deploymentReadersSupplier.get().values()) {
            try {
                allMetrics.addAll(handle.reader.collectAllMetrics());
            } catch (Exception e) {
                OTEL_LOGGER.failedToCollectMetrics(handle.deploymentName, e.getMessage());
            }
        }

        lastExport = downstream.export(allMetrics);
        return lastExport;
    }

    /**
     * Collects and exports the remaining points for one deployment before its reader is discarded.
     *
     * @param handle the deployment reader to collect
     * @return the completed export result, or a failed result when collection or export times out
     */
    public synchronized CompletableResultCode exportDeployment(DeploymentMetricReaderHandle handle) {
        lastExport.join(EXPORT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (!lastExport.isDone()) {
            OTEL_LOGGER.timedOutWaitingForMetricExport(handle.deploymentName);
            return CompletableResultCode.ofFailure();
        }

        try {
            lastExport = downstream.export(handle.reader.collectAllMetrics());
            lastExport.join(EXPORT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!lastExport.isDone()) {
                OTEL_LOGGER.timedOutWaitingForMetricExport(handle.deploymentName);
                return CompletableResultCode.ofFailure();
            }
            if (!lastExport.isSuccess()) {
                OTEL_LOGGER.timedOutWaitingForMetricExport(handle.deploymentName);
            }
            return lastExport;
        } catch (Exception e) {
            OTEL_LOGGER.failedToCollectMetrics(handle.deploymentName, e.getMessage());
            return CompletableResultCode.ofFailure();
        }
    }

    /** {@inheritDoc} */
    @Override
    public AggregationTemporality getAggregationTemporality(InstrumentType instrumentType) {
        return downstream.getAggregationTemporality(instrumentType);
    }

    /** {@inheritDoc} */
    @Override
    public MemoryMode getMemoryMode() {
        return downstream.getMemoryMode();
    }

    /** {@inheritDoc} */
    @Override
    public Aggregation getDefaultAggregation(InstrumentType instrumentType) {
        return downstream.getDefaultAggregation(instrumentType);
    }

    /** {@inheritDoc} */
    @Override
    public CompletableResultCode flush() {
        return downstream.flush();
    }

    /** {@inheritDoc} */
    @Override
    public CompletableResultCode shutdown() {
        return downstream.shutdown();
    }

    /**
     * Associates a deployment name with its on-demand metric reader.
     */
    public static class DeploymentMetricReaderHandle {
        final DeploymentMetricReader reader;
        final String deploymentName;

        /**
         * Creates a deployment reader handle.
         *
         * @param reader the deployment reader
         * @param deploymentName the canonical deployment name
         */
        public DeploymentMetricReaderHandle(DeploymentMetricReader reader,
                                         String deploymentName) {
            this.reader = reader;
            this.deploymentName = deploymentName;
        }
    }
}

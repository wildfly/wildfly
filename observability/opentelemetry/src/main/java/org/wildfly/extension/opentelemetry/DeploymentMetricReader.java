/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.opentelemetry;

import java.util.Collection;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.export.CollectionRegistration;
import io.opentelemetry.sdk.metrics.export.MetricReader;

/**
 * A MetricReader for per-deployment metrics that is called on-demand by the
 * server's aggregating exporter. Does not run its own export schedule.
 */
class DeploymentMetricReader implements MetricReader {
    private volatile CollectionRegistration collectionRegistration = CollectionRegistration.noop();
    private final AggregationTemporality temporality;

    DeploymentMetricReader(AggregationTemporality temporality) {
        this.temporality = temporality;
    }

    @Override
    public void register(CollectionRegistration collectionRegistration) {
        this.collectionRegistration = collectionRegistration;
    }

    @Override
    public CompletableResultCode forceFlush() {
        return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode shutdown() {
        return CompletableResultCode.ofSuccess();
    }

    @Override
    public AggregationTemporality getAggregationTemporality(io.opentelemetry.sdk.metrics.InstrumentType instrumentType) {
        return temporality;
    }

    /**
     * Called by the AggregatingMetricExporter to collect this deployment's metrics.
     */
    Collection<MetricData> collectAllMetrics() {
        return collectionRegistration.collectAllMetrics();
    }
}

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

    /**
     * Creates an on-demand reader using the deployment export temporality.
     *
     * @param temporality the aggregation temporality reported to the meter provider
     */
    DeploymentMetricReader(AggregationTemporality temporality) {
        this.temporality = temporality;
    }

    /** {@inheritDoc} */
    @Override
    public void register(CollectionRegistration collectionRegistration) {
        this.collectionRegistration = collectionRegistration;
    }

    /** {@inheritDoc} */
    @Override
    public CompletableResultCode forceFlush() {
        return CompletableResultCode.ofSuccess();
    }

    /**
     * Performs no shutdown work because the owning deployment meter provider controls collection lifecycle.
     *
     * @return a successful result
     */
    @Override
    public CompletableResultCode shutdown() {
        return CompletableResultCode.ofSuccess();
    }

    /** {@inheritDoc} */
    @Override
    public AggregationTemporality getAggregationTemporality(io.opentelemetry.sdk.metrics.InstrumentType instrumentType) {
        return temporality;
    }

    /**
     * Collects all metrics currently pending for this deployment.
     *
     * @return the pending metric data
     */
    Collection<MetricData> collectAllMetrics() {
        return collectionRegistration.collectAllMetrics();
    }
}

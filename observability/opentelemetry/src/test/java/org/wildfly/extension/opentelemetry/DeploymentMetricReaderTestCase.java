/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.opentelemetry;

import static org.junit.Assert.assertEquals;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicLong;

import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.LongUpDownCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.ObservableLongUpDownCounter;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.LongPointData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.data.SumData;
import org.junit.Test;

/** Verifies that deployment metric collection preserves temporality required by each instrument. */
public class DeploymentMetricReaderTestCase {
    /** Verifies up/down sums remain cumulative while monotonic counters continue to export deltas. */
    @Test
    public void upDownCountersRemainCumulativeAcrossCollections() {
        DeploymentMetricReader reader = new DeploymentMetricReader(AggregationTemporality.DELTA);
        try (SdkMeterProvider provider = SdkMeterProvider.builder().registerMetricReader(reader).build()) {
            Meter meter = provider.get("deployment-metrics");
            LongUpDownCounter synchronous = meter.upDownCounterBuilder("synchronous-balance").build();
            LongCounter counter = meter.counterBuilder("requests").build();
            AtomicLong observedValue = new AtomicLong(5);
            try (ObservableLongUpDownCounter observable = meter.upDownCounterBuilder("observable-balance")
                    .buildWithCallback(measurement -> measurement.record(observedValue.get()))) {
                synchronous.add(5);
                counter.add(4);
                Collection<MetricData> first = reader.collectAllMetrics();
                assertMetric(first, "synchronous-balance", AggregationTemporality.CUMULATIVE, 5);
                assertMetric(first, "observable-balance", AggregationTemporality.CUMULATIVE, 5);
                assertMetric(first, "requests", AggregationTemporality.DELTA, 4);

                synchronous.add(-2);
                observedValue.set(3);
                counter.add(2);
                Collection<MetricData> second = reader.collectAllMetrics();
                assertMetric(second, "synchronous-balance", AggregationTemporality.CUMULATIVE, 3);
                assertMetric(second, "observable-balance", AggregationTemporality.CUMULATIVE, 3);
                assertMetric(second, "requests", AggregationTemporality.DELTA, 2);
            }
        }
    }

    /**
     * Verifies one collected sum's temporality and value.
     *
     * @param metrics collected deployment metrics
     * @param name expected metric name
     * @param temporality expected aggregation temporality
     * @param value expected sum
     */
    private static void assertMetric(Collection<MetricData> metrics, String name,
                                     AggregationTemporality temporality, long value) {
        MetricData metric = metrics.stream().filter(candidate -> name.equals(candidate.getName()))
                .findFirst().orElseThrow();
        SumData<LongPointData> sum = metric.getLongSumData();
        assertEquals(name, temporality, sum.getAggregationTemporality());
        assertEquals(name, value, sum.getPoints().iterator().next().getValue());
    }
}

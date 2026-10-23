/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.opentelemetry.api;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.Data;
import io.opentelemetry.sdk.metrics.data.DoublePointData;
import io.opentelemetry.sdk.metrics.data.ExponentialHistogramData;
import io.opentelemetry.sdk.metrics.data.GaugeData;
import io.opentelemetry.sdk.metrics.data.HistogramData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.data.PointData;
import io.opentelemetry.sdk.metrics.data.SumData;
import io.opentelemetry.sdk.metrics.data.SummaryData;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import org.wildfly.extension.observability.shared.FilterModel;

/**
 * Metric exporter that filters metrics based on configured filter rules before delegating
 * to the underlying exporter. Filters can accept or reject metrics based on meter name,
 * tag name, or tag value.
 */
public class FilteringMetricExporter implements MetricExporter {
    private final MetricExporter delegate;
    private final List<FilterModel> acceptFilters;
    private final List<FilterModel> rejectFilters;

    /**
     * Creates a filtering metric exporter.
     *
     * @param delegate the underlying exporter to delegate to after filtering
     * @param filters the list of filter rules to apply
     */
    public FilteringMetricExporter(MetricExporter delegate, List<FilterModel> filters) {
        this.delegate = delegate;

        acceptFilters = filters.stream().filter(filter -> filter.outcome() == FilterModel.Outcome.ACCEPT).toList();
        rejectFilters = filters.stream().filter(filter -> filter.outcome() == FilterModel.Outcome.REJECT).toList();
    }

    @Override
    public CompletableResultCode export(Collection<MetricData> metrics) {
        return delegate.export(applyFilters(metrics));
    }

    @Override
    public CompletableResultCode flush() {
        return delegate.flush();
    }

    @Override
    public CompletableResultCode shutdown() {
        return delegate.shutdown();
    }

    @Override
    public AggregationTemporality getAggregationTemporality(InstrumentType instrumentType) {
        return delegate.getAggregationTemporality(instrumentType);
    }

    /**
     * Applies configured filters to the metric collection.
     *
     * @param metrics the metrics to filter
     * @return the filtered metrics
     */
    private Collection<MetricData> applyFilters(Collection<MetricData> metrics) {
        return metrics.stream()
                .map(this::filterMetric)
                .filter(metric -> metric != null)
                .toList();
    }

    /**
     * Filters a single metric by removing points that don't match the filter criteria.
     *
     * @param metric the metric to filter
     * @return the filtered metric, or null if all points were filtered out
     */
    private MetricData filterMetric(MetricData metric) {
        Predicate<PointData> filter = point -> isAccepted(metric.getName(), point);
        if (metric.getData().getPoints().stream().noneMatch(filter)) {
            return null;
        }

        Data<?> filteredData = filterDataByType(metric.getData(), filter);
        return createFilteredMetricData(metric, filteredData);
    }

    /**
     * Filters metric data based on its type.
     *
     * @param data the metric data to filter
     * @param filter the point filter predicate
     * @return the filtered data
     */
    private Data<?> filterDataByType(Data<?> data, Predicate<PointData> filter) {
        if (data instanceof GaugeData<?> gaugeData) {
            return filterGaugeData(gaugeData, filter);
        } else if (data instanceof SumData<?> sumData) {
            return filterSumData(sumData, filter);
        } else if (data instanceof HistogramData histogramData) {
            return filterHistogramData(histogramData, filter);
        } else if (data instanceof ExponentialHistogramData exponentialHistogramData) {
            return filterExponentialHistogramData(exponentialHistogramData, filter);
        } else if (data instanceof SummaryData summaryData) {
            return filterSummaryData(summaryData, filter);
        } else {
            throw new IllegalArgumentException("Unsupported metric data type: " + data.getClass());
        }
    }

    /**
     * Creates a MetricData wrapper that delegates to the original metric but returns filtered data.
     *
     * @param original the original metric data
     * @param filteredData the filtered data points
     * @return a new MetricData instance with filtered points
     */
    private static MetricData createFilteredMetricData(MetricData original, Data<?> filteredData) {
        return new MetricData() {
            @Override
            public io.opentelemetry.sdk.resources.Resource getResource() {
                return original.getResource();
            }

            @Override
            public io.opentelemetry.sdk.common.InstrumentationScopeInfo getInstrumentationScopeInfo() {
                return original.getInstrumentationScopeInfo();
            }

            @Override
            public String getName() {
                return original.getName();
            }

            @Override
            public String getDescription() {
                return original.getDescription();
            }

            @Override
            public String getUnit() {
                return original.getUnit();
            }

            @Override
            public io.opentelemetry.sdk.metrics.data.MetricDataType getType() {
                return original.getType();
            }

            @Override
            public Data<?> getData() {
                return filteredData;
            }

            /**
             * Returns filtered double-sum data directly because the default accessor expects the SDK's internal
             * sum-data implementation.
             *
             * @return the filtered data for a double sum, or the delegate's data for any other metric type
             */
            @Override
            @SuppressWarnings("unchecked")
            public SumData<DoublePointData> getDoubleSumData() {
                return getType() == io.opentelemetry.sdk.metrics.data.MetricDataType.DOUBLE_SUM
                        ? (SumData<DoublePointData>) filteredData
                        : original.getDoubleSumData();
            }
        };
    }

    /**
     * Checks if a metric point is accepted by the configured filters.
     * Accept filters take precedence - if any accept filter matches, the point is accepted.
     * Otherwise, the point is rejected if any reject filter matches.
     *
     * @param name the metric name
     * @param point the metric point data
     * @return true if the point should be included in the export
     */
    private boolean isAccepted(String name, PointData point) {
        // Convert OpenTelemetry attributes to simple map for filter matching
        Map<String, String> attributes = point.getAttributes().asMap().entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey().getKey(), e -> String.valueOf(e.getValue())));
        if (acceptFilters.stream().anyMatch(f -> f.matches(name, attributes))) {
            return true;
        }
        return rejectFilters.stream().noneMatch(f -> f.matches(name, attributes));
    }

    private static <T extends PointData> GaugeData<T> filterGaugeData(GaugeData<T> data,
                                                                       Predicate<PointData> filter) {
        return () -> data.getPoints().stream().filter(filter).toList();
    }

    private static <T extends PointData> SumData<T> filterSumData(SumData<T> data, Predicate<PointData> filter) {
        return new SumData<>() {
            @Override
            public boolean isMonotonic() {
                return data.isMonotonic();
            }

            @Override
            public AggregationTemporality getAggregationTemporality() {
                return data.getAggregationTemporality();
            }

            @Override
            public Collection<T> getPoints() {
                return data.getPoints().stream().filter(filter).toList();
            }
        };
    }

    private static HistogramData filterHistogramData(HistogramData data, Predicate<PointData> filter) {
        return new HistogramData() {
            @Override
            public AggregationTemporality getAggregationTemporality() {
                return data.getAggregationTemporality();
            }

            @Override
            public Collection<io.opentelemetry.sdk.metrics.data.HistogramPointData> getPoints() {
                return data.getPoints().stream().filter(filter).toList();
            }
        };
    }

    private static ExponentialHistogramData filterExponentialHistogramData(ExponentialHistogramData data,
                                                                            Predicate<PointData> filter) {
        return new ExponentialHistogramData() {
            @Override
            public AggregationTemporality getAggregationTemporality() {
                return data.getAggregationTemporality();
            }

            @Override
            public Collection<io.opentelemetry.sdk.metrics.data.ExponentialHistogramPointData> getPoints() {
                return data.getPoints().stream().filter(filter).toList();
            }
        };
    }

    private static SummaryData filterSummaryData(SummaryData data, Predicate<PointData> filter) {
        return () -> data.getPoints().stream().filter(filter).toList();
    }
}

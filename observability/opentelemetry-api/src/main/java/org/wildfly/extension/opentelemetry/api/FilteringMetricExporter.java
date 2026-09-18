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

public class FilteringMetricExporter implements MetricExporter {
    private final MetricExporter delegate;
    private final List<FilterModel> acceptFilters;
    private final List<FilterModel> rejectFilters;

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

    private Collection<MetricData> applyFilters(Collection<MetricData> metrics) {
        return metrics.stream()
                .map(this::filterMetric)
                .filter(metric -> metric != null)
                .toList();
    }

    private MetricData filterMetric(MetricData metric) {
        Predicate<PointData> filter = point -> isAccepted(metric.getName(), point);
        if (metric.getData().getPoints().stream().noneMatch(filter)) {
            return null;
        }

        Data<?> data = metric.getData();
        Data<?> filteredData;
        if (data instanceof GaugeData<?> gaugeData) {
            filteredData = filterGaugeData(gaugeData, filter);
        } else if (data instanceof SumData<?> sumData) {
            filteredData = filterSumData(sumData, filter);
        } else if (data instanceof HistogramData histogramData) {
            filteredData = filterHistogramData(histogramData, filter);
        } else if (data instanceof ExponentialHistogramData exponentialHistogramData) {
            filteredData = filterExponentialHistogramData(exponentialHistogramData, filter);
        } else if (data instanceof SummaryData summaryData) {
            filteredData = filterSummaryData(summaryData, filter);
        } else {
            throw new IllegalArgumentException("Unsupported metric data type: " + data.getClass());
        }

        return new MetricData() {
            @Override
            public io.opentelemetry.sdk.resources.Resource getResource() {
                return metric.getResource();
            }

            @Override
            public io.opentelemetry.sdk.common.InstrumentationScopeInfo getInstrumentationScopeInfo() {
                return metric.getInstrumentationScopeInfo();
            }

            @Override
            public String getName() {
                return metric.getName();
            }

            @Override
            public String getDescription() {
                return metric.getDescription();
            }

            @Override
            public String getUnit() {
                return metric.getUnit();
            }

            @Override
            public io.opentelemetry.sdk.metrics.data.MetricDataType getType() {
                return metric.getType();
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
                        : metric.getDoubleSumData();
            }
        };
    }

    private boolean isAccepted(String name, PointData point) {
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

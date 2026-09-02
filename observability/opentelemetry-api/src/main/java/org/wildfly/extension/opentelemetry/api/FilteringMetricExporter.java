/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.opentelemetry.api;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.MetricData;
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
                .filter(metric -> {
                    Map<String, String> attributes = getAttributes(metric);
                    if (acceptFilters.stream().anyMatch(f -> f.matches(metric.getName(), attributes))) {
                        return true;
                    }
                    return rejectFilters.stream().noneMatch(f -> f.matches(metric.getName(), attributes));
                })
                .toList();
    }

    private Map<String, String> getAttributes(MetricData metricData) {
        return metricData.getData().getPoints().stream()
                .findFirst()
                .map(point -> point.getAttributes().asMap().entrySet().stream()
                        .collect(Collectors.toMap(e -> e.getKey().getKey(),
                                e -> String.valueOf(e.getValue()))))
                .orElse(Map.of());
    }
}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.opentelemetry.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.List;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.Data;
import io.opentelemetry.sdk.metrics.data.ExemplarData;
import io.opentelemetry.sdk.metrics.data.GaugeData;
import io.opentelemetry.sdk.metrics.data.HistogramData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.data.MetricDataType;
import io.opentelemetry.sdk.metrics.data.PointData;
import io.opentelemetry.sdk.metrics.data.ExponentialHistogramData;
import io.opentelemetry.sdk.metrics.data.SummaryData;
import io.opentelemetry.sdk.metrics.data.SumData;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import org.junit.jupiter.api.Test;
import org.wildfly.extension.observability.shared.FilterModel;
import org.wildfly.extension.observability.shared.FilterModel.Condition;
import org.wildfly.extension.observability.shared.FilterModel.Field;
import org.wildfly.extension.observability.shared.FilterModel.Outcome;

public class FilteringMetricExporterTest {

    @Test
    public void testFiltersEachPointIndependently() {
        CapturingExporter delegate = new CapturingExporter();
        FilterModel filter = new FilterModel(Outcome.REJECT, Field.TAG_VALUE, Condition.EQUALS, false, "blocked");
        FilteringMetricExporter exporter = new FilteringMetricExporter(delegate, List.of(filter));

        PointData allowed = point("allowed");
        PointData blocked = point("blocked");
        exporter.export(List.of(metric(allowed, blocked)));

        assertEquals(1, delegate.metrics.size());
        assertEquals(List.of(allowed), delegate.metrics.iterator().next().getData().getPoints());
    }

    @Test
    public void testRetainsAllowedPointWhenRejectedPointIsFirst() {
        CapturingExporter delegate = new CapturingExporter();
        FilterModel filter = new FilterModel(Outcome.REJECT, Field.TAG_VALUE, Condition.EQUALS, false, "blocked");
        FilteringMetricExporter exporter = new FilteringMetricExporter(delegate, List.of(filter));

        PointData blocked = point("blocked");
        PointData allowed = point("allowed");
        exporter.export(List.of(metric(blocked, allowed)));

        assertEquals(1, delegate.metrics.size());
        assertEquals(List.of(allowed), delegate.metrics.iterator().next().getData().getPoints());
    }

    @Test
    public void testFiltersAllSupportedMetricDataTypes() {
        List<DataCase> cases = List.of(
                new DataCase(GaugeData.class, MetricDataType.DOUBLE_GAUGE),
                new DataCase(SumData.class, MetricDataType.LONG_SUM),
                new DataCase(HistogramData.class, MetricDataType.HISTOGRAM),
                new DataCase(ExponentialHistogramData.class, MetricDataType.EXPONENTIAL_HISTOGRAM),
                new DataCase(SummaryData.class, MetricDataType.SUMMARY));

        for (DataCase dataCase : cases) {
            CapturingExporter delegate = new CapturingExporter();
            FilterModel filter = new FilterModel(Outcome.REJECT, Field.TAG_VALUE, Condition.EQUALS, false, "blocked");
            new FilteringMetricExporter(delegate, List.of(filter)).export(
                    List.of(metric(data(dataCase.dataType(), dataCase.metricType()), dataCase.metricType())));

            assertEquals(1, delegate.metrics.size());
            PointData retained = (PointData) delegate.metrics.iterator().next().getData().getPoints().iterator().next();
            assertEquals("allowed", retained.getAttributes().asMap().values().iterator().next());
        }
    }

    @Test
    public void testDoubleSumDataRemainsAccessibleAfterFiltering() {
        CapturingExporter delegate = new CapturingExporter();
        FilterModel filter = new FilterModel(Outcome.REJECT, Field.TAG_VALUE, Condition.EQUALS, false, "blocked");
        FilteringMetricExporter exporter = new FilteringMetricExporter(delegate, List.of(filter));

        exporter.export(List.of(metric(data(SumData.class, MetricDataType.DOUBLE_SUM), MetricDataType.DOUBLE_SUM)));

        assertEquals(1, delegate.metrics.iterator().next().getDoubleSumData().getPoints().size());
    }

    private static Data<?> data(Class<?> dataType, MetricDataType metricType) {
        PointData allowed = point("allowed");
        PointData blocked = point("blocked");
        return (Data<?>) Proxy.newProxyInstance(
                dataType.getClassLoader(), new Class<?>[]{dataType}, (proxy, method, args) -> {
                    if (method.getName().equals("getPoints")) {
                        return List.of(allowed, blocked);
                    }
                    if (method.getName().equals("getAggregationTemporality")) {
                        return AggregationTemporality.CUMULATIVE;
                    }
                    if (method.getName().equals("isMonotonic")) {
                        return false;
                    }
                    throw new UnsupportedOperationException(method.getName() + " for " + metricType);
                });
    }

    private static MetricData metric(Data<?> data, MetricDataType type) {
        return new MetricData() {
            @Override
            public String getName() {
                return "test.metric";
            }

            @Override
            public Data<?> getData() {
                return data;
            }

            @Override
            public io.opentelemetry.sdk.resources.Resource getResource() {
                return null;
            }

            @Override
            public io.opentelemetry.sdk.common.InstrumentationScopeInfo getInstrumentationScopeInfo() {
                return null;
            }

            @Override
            public String getDescription() {
                return "";
            }

            @Override
            public String getUnit() {
                return "";
            }

            @Override
            public MetricDataType getType() {
                return type;
            }
        };
    }

    private record DataCase(Class<?> dataType, MetricDataType metricType) {
    }

    private static PointData point(String value) {
        return new PointData() {
            @Override
            public long getStartEpochNanos() {
                return 0;
            }

            @Override
            public long getEpochNanos() {
                return 0;
            }

            @Override
            public Attributes getAttributes() {
                return Attributes.builder().put("label", value).build();
            }

            @Override
            public List<? extends ExemplarData> getExemplars() {
                return List.of();
            }
        };
    }

    private static MetricData metric(PointData... points) {
        Data<PointData> data = () -> List.of(points);
        GaugeData<PointData> gauge = data::getPoints;
        return new MetricData() {
            @Override
            public String getName() {
                return "test.metric";
            }

            @Override
            public Data<PointData> getData() {
                return gauge;
            }

            @Override
            public io.opentelemetry.sdk.resources.Resource getResource() {
                return null;
            }

            @Override
            public io.opentelemetry.sdk.common.InstrumentationScopeInfo getInstrumentationScopeInfo() {
                return null;
            }

            @Override
            public String getDescription() {
                return "";
            }

            @Override
            public String getUnit() {
                return "";
            }

            @Override
            public io.opentelemetry.sdk.metrics.data.MetricDataType getType() {
                return io.opentelemetry.sdk.metrics.data.MetricDataType.DOUBLE_GAUGE;
            }
        };
    }

    private static class CapturingExporter implements MetricExporter {
        private Collection<MetricData> metrics;

        @Override
        public CompletableResultCode export(Collection<MetricData> metrics) {
            this.metrics = metrics;
            return CompletableResultCode.ofSuccess();
        }

        @Override
        public CompletableResultCode flush() {
            return CompletableResultCode.ofSuccess();
        }

        @Override
        public CompletableResultCode shutdown() {
            return CompletableResultCode.ofSuccess();
        }

        @Override
        public AggregationTemporality getAggregationTemporality(InstrumentType instrumentType) {
            return AggregationTemporality.CUMULATIVE;
        }
    }
}

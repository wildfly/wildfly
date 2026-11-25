/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.micrometer.prometheus;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tag;
import io.micrometer.prometheusmetrics.PrometheusNamingConvention;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import io.prometheus.metrics.expositionformats.ExpositionFormats;
import io.prometheus.metrics.expositionformats.PrometheusTextFormatWriter;
import io.prometheus.metrics.model.snapshots.CounterSnapshot;
import io.prometheus.metrics.model.snapshots.CounterSnapshot.CounterDataPointSnapshot;
import io.prometheus.metrics.model.snapshots.GaugeSnapshot;
import io.prometheus.metrics.model.snapshots.GaugeSnapshot.GaugeDataPointSnapshot;
import io.prometheus.metrics.model.snapshots.Label;
import io.prometheus.metrics.model.snapshots.Labels;
import io.prometheus.metrics.model.snapshots.MetricSnapshot;
import io.prometheus.metrics.model.snapshots.MetricSnapshots;
import org.wildfly.extension.micrometer.metrics.MetricScrapeContext;
import org.wildfly.extension.micrometer.registry.WildFlyRegistry;

public class WildFlyPrometheusRegistry extends PrometheusMeterRegistry implements WildFlyRegistry {
    private static final PrometheusNamingConvention NAMING_CONVENTION = new PrometheusNamingConvention();
    private static final ExpositionFormats EXPOSITION_FORMATS = ExpositionFormats.init();

    public WildFlyPrometheusRegistry() {
        super(PrometheusConfig.DEFAULT);
    }

    /** Scrapes metrics in the default format while omitting model series denied to this caller. */
    @Override
    public String scrape() {
        return scrape(PrometheusTextFormatWriter.CONTENT_TYPE);
    }

    /** Returns a caller-filtered scrape in the requested exposition format. */
    @Override
    public String scrape(String contentType) {
        return scrapeAsString(contentType, null);
    }

    /** Returns a caller-filtered scrape limited to the requested metric names. */
    @Override
    public String scrape(String contentType, Set<String> includedNames) {
        return scrapeAsString(contentType, includedNames);
    }

    /** Writes a caller-filtered scrape in the default exposition format. */
    @Override
    public void scrape(OutputStream outputStream) throws IOException {
        scrape(outputStream, PrometheusTextFormatWriter.CONTENT_TYPE);
    }

    /** Writes a caller-filtered scrape in the requested exposition format. */
    @Override
    public void scrape(OutputStream outputStream, String contentType) throws IOException {
        scrape(outputStream, contentType, null);
    }

    /** Writes a caller-filtered scrape limited to the requested metric names. */
    @Override
    public void scrape(OutputStream outputStream, String contentType, Set<String> includedNames) throws IOException {
        try (MetricScrapeContext context = MetricScrapeContext.begin()) {
            MetricSnapshots snapshots = includedNames == null
                    ? getPrometheusRegistry().scrape()
                    : getPrometheusRegistry().scrape(includedNames::contains);
            MetricSnapshots filteredSnapshots = filterDeniedSeries(snapshots, context.deniedMeters());
            EXPOSITION_FORMATS.findWriter(contentType).write(outputStream, filteredSnapshots);
        }
    }

    /** Encodes a caller-filtered scrape as a UTF-8 string. */
    private String scrapeAsString(String contentType, Set<String> includedNames) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            scrape(outputStream, contentType, includedNames);
        } catch (IOException e) {
            // A ByteArrayOutputStream never throws, but the writer API is declared to; surface it faithfully.
            throw new UncheckedIOException(e);
        }
        return outputStream.toString(StandardCharsets.UTF_8);
    }

    /** Removes denied model datapoints while preserving all other metric families. */
    private static MetricSnapshots filterDeniedSeries(MetricSnapshots snapshots, Set<Meter.Id> deniedMeters) {
        if (deniedMeters.isEmpty()) {
            return snapshots;
        }
        Set<MetricSeries> deniedSeries = deniedMeters.stream()
                .map(MetricSeries::from)
                .collect(Collectors.toSet());
        List<MetricSnapshot> filtered = new ArrayList<>(snapshots.size());
        for (MetricSnapshot snapshot : snapshots) {
            if (snapshot instanceof GaugeSnapshot gauge) {
                List<GaugeDataPointSnapshot> datapoints = gauge.getDataPoints().stream()
                        .filter(datapoint -> !deniedSeries.contains(
                                MetricSeries.from(snapshot.getMetadata().getName(), datapoint.getLabels())))
                        .toList();
                if (!datapoints.isEmpty()) {
                    filtered.add(new GaugeSnapshot(gauge.getMetadata(), datapoints));
                }
            } else if (snapshot instanceof CounterSnapshot counter) {
                List<CounterDataPointSnapshot> datapoints = counter.getDataPoints().stream()
                        .filter(datapoint -> !deniedSeries.contains(
                                MetricSeries.from(snapshot.getMetadata().getName(), datapoint.getLabels())))
                        .toList();
                if (!datapoints.isEmpty()) {
                    filtered.add(new CounterSnapshot(counter.getMetadata(), datapoints));
                }
            } else {
                filtered.add(snapshot);
            }
        }
        return new MetricSnapshots(filtered);
    }

    /** Identifies one Prometheus series by its exported name and labels. */
    private record MetricSeries(String name, Map<String, String> labels) {
        /** Builds an exported series identity from the meter's Prometheus name and labels. */
        private static MetricSeries from(Meter.Id id) {
            Map<String, String> labels = new HashMap<>();
            for (Tag tag : id.getConventionTags(NAMING_CONVENTION)) {
                labels.put(tag.getKey(), tag.getValue());
            }
            return new MetricSeries(id.getConventionName(NAMING_CONVENTION), Map.copyOf(labels));
        }

        /** Builds an exported series identity from a Prometheus datapoint. */
        private static MetricSeries from(String name, Labels labels) {
            Map<String, String> values = new HashMap<>();
            for (Label label : labels) {
                values.put(label.getName(), label.getValue());
            }
            return new MetricSeries(name, Map.copyOf(values));
        }
    }
}

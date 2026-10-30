/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.shared.observability.signals;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/** Verifies parsing of Prometheus exposition metadata, samples, and labels. */
class PrometheusMetricTest {

    /** Verifies that absent response bodies produce no metrics. */
    @Test
    void returnsEmptyListForNullOrEmptyBody() {
        assertTrue(PrometheusMetric.buildPrometheusMetrics(null).isEmpty(), "Null body should produce no metrics");
        assertTrue(PrometheusMetric.buildPrometheusMetrics("").isEmpty(), "Empty body should produce no metrics");
    }

    /** Verifies metadata and an unlabeled sample. */
    @Test
    void parsesMetadataAndMetricWithoutLabels() {
        List<PrometheusMetric> metrics = PrometheusMetric.buildPrometheusMetrics("""
             # HELP requests_total Total requests
             # TYPE requests_total counter
             requests_total 42
             """);

        assertEquals(1, metrics.size(), "One unlabeled metric should be parsed");
        PrometheusMetric metric = metrics.get(0);
        assertEquals("requests_total", metric.getKey(), "Metric name should be parsed");
        assertEquals("42", metric.getValue(), "Metric value should be parsed");
        assertEquals(" counter", metric.getType(), "Metric type metadata should be parsed");
        assertEquals(" Total requests", metric.getHelp(), "Metric help metadata should be parsed");
        assertTrue(metric.getTags().isEmpty(), "Unlabeled metric should have no tags");
    }

    /** Verifies that blank and ordinary comment lines are ignored. */
    @Test
    void ignoresBlankAndCommentLines() {
        List<PrometheusMetric> metrics = PrometheusMetric.buildPrometheusMetrics("""
                # An ordinary comment
                requests_total 1

                # Another comment
                """);

        assertEquals(1, metrics.size(), "Only the sample line should be parsed");
        assertEquals("requests_total", metrics.get(0).getKey(), "Sample name should be parsed");
    }

    /** Verifies that commas inside quoted label values are preserved. */
    @Test
    void parsesLabelValuesContainingCommas() {
        List<PrometheusMetric> metrics = PrometheusMetric.buildPrometheusMetrics("""
                queue_messages{queue="orders,priority",state="ready"} 1
                """);

        assertEquals(1, metrics.size(), "One labeled metric should be parsed");
        assertEquals("orders,priority", metrics.get(0).getTags().get("queue"), "Comma should remain in label value");
        assertEquals("ready", metrics.get(0).getTags().get("state"), "Second label should be parsed");
    }

    /** Verifies that equals signs inside quoted label values are preserved. */
    @Test
    void parsesEqualsSignsInsideLabelValues() {
        List<PrometheusMetric> metrics = PrometheusMetric.buildPrometheusMetrics("""
                http_requests{query="a=b,c=d"} 2
                """);

        assertEquals(1, metrics.size(), "One labeled metric should be parsed");
        assertEquals("a=b,c=d", metrics.get(0).getTags().get("query"), "Equals signs should remain in label value");
    }

    /** Verifies that non-sample lines without a value are ignored. */
    @Test
    void skipsNonSampleLinesWithoutSpaces() {
        List<PrometheusMetric> metrics = PrometheusMetric.buildPrometheusMetrics("""
                not-a-prometheus-sample
                requests_total 1
                """);

        assertEquals(1, metrics.size(), "Only the valid sample should be parsed");
        assertEquals("requests_total", metrics.get(0).getKey(), "Valid sample name should be parsed");
    }

    /** Verifies that malformed labels are skipped without discarding valid samples. */
    @Test
    void skipsMalformedLabels() {
        List<PrometheusMetric> metrics = PrometheusMetric.buildPrometheusMetrics("""
                malformed{bar} 1
                also_malformed{} 1
                requests_total{state="ready"} 2
                """);

        assertEquals(1, metrics.size(), "Malformed samples should be skipped");
        assertEquals("requests_total", metrics.get(0).getKey(), "Valid sample should remain");
    }

    /** Verifies that incomplete metadata comments do not prevent sample parsing. */
    @Test
    void skipsTruncatedMetadataLines() {
        List<PrometheusMetric> metrics = PrometheusMetric.buildPrometheusMetrics("""
                # HELP
                # TYPE
                requests_total 1
                """);

        assertEquals(1, metrics.size(), "Sample should be parsed despite truncated metadata");
        assertEquals("requests_total", metrics.get(0).getKey(), "Sample name should be parsed");
    }
}

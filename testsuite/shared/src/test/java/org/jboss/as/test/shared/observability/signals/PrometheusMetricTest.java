/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.shared.observability.signals;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class PrometheusMetricTest {

    @Test
    void returnsEmptyListForNullOrEmptyBody() {
        assertTrue(PrometheusMetric.buildPrometheusMetrics(null).isEmpty());
        assertTrue(PrometheusMetric.buildPrometheusMetrics("").isEmpty());
    }

    @Test
    void parsesMetadataAndMetricWithoutLabels() {
        List<PrometheusMetric> metrics = PrometheusMetric.buildPrometheusMetrics("""
             # HELP requests_total Total requests
             # TYPE requests_total counter
             requests_total 42
             """);

        assertEquals(1, metrics.size());
        PrometheusMetric metric = metrics.get(0);
        assertEquals("requests_total", metric.getKey());
        assertEquals("42", metric.getValue());
        assertEquals(" counter", metric.getType());
        assertEquals(" Total requests", metric.getHelp());
        assertTrue(metric.getTags().isEmpty());
    }

    @Test
    void ignoresBlankAndCommentLines() {
        List<PrometheusMetric> metrics = PrometheusMetric.buildPrometheusMetrics("""
                # An ordinary comment
                requests_total 1

                # Another comment
                """);

        assertEquals(1, metrics.size());
        assertEquals("requests_total", metrics.get(0).getKey());
    }

    @Test
    void parsesLabelValuesContainingCommas() {
        List<PrometheusMetric> metrics = PrometheusMetric.buildPrometheusMetrics("""
                queue_messages{queue="orders,priority",state="ready"} 1
                """);

        assertEquals(1, metrics.size());
        assertEquals("orders,priority", metrics.get(0).getTags().get("queue"));
        assertEquals("ready", metrics.get(0).getTags().get("state"));
    }

    @Test
    void parsesEqualsSignsInsideLabelValues() {
        List<PrometheusMetric> metrics = PrometheusMetric.buildPrometheusMetrics("""
                http_requests{query="a=b,c=d"} 2
                """);

        assertEquals(1, metrics.size());
        assertEquals("a=b,c=d", metrics.get(0).getTags().get("query"));
    }

    @Test
    void skipsNonSampleLinesWithoutSpaces() {
        List<PrometheusMetric> metrics = PrometheusMetric.buildPrometheusMetrics("""
                not-a-prometheus-sample
                requests_total 1
                """);

        assertEquals(1, metrics.size());
        assertEquals("requests_total", metrics.get(0).getKey());
    }
}

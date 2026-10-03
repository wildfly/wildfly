/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.shared.observability.signals;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PrometheusMetric {
    private final String key;
    private final Map<String, String> tags;
    private final String value;
    private final String type;
    private final String help;

    public PrometheusMetric(String key,
                            Map<String, String> tags,
                            String value,
                            String type,
                            String help) {
        this.key = key;
        this.tags = Collections.unmodifiableMap(tags);
        this.value = value;
        this.type = type;
        this.help = help;
    }

    public String getKey() {
        return key;
    }

    public Map<String, String> getTags() {
        return tags;
    }

    public String getValue() {
        return value;
    }

    public String getType() {
        return type;
    }

    public String getHelp() {
        return help;
    }

    @Override
    public String toString() {
        return "PrometheusMetric{" +
                "key='" + key + '\'' +
                ", tags=" + tags +
                ", value='" + value + '\'' +
                ", type='" + type + '\'' +
                ", help='" + help + '\'' +
                '}';
    }

    /**
     * Parses the text exposition returned from a Prometheus endpoint into a list of {@link PrometheusMetric} instances.
     * {@code # HELP} and {@code # TYPE} lines are consumed as metadata, and other comment or blank lines are ignored.
     * Lines that are not well-formed samples (for example an authentication error page returned in place of metrics)
     * are skipped rather than causing a failure.
     *
     * @param body the raw response body from a Prometheus endpoint
     * @return the parsed metrics, or an empty list if {@code body} is null or empty
     */
    public static List<PrometheusMetric> buildPrometheusMetrics(String body) {
        List<PrometheusMetric> metrics = new LinkedList<>();
        if (body == null || body.isEmpty()) {
            return metrics;
        }

        Map<String, String> help = new HashMap<>();
        Map<String, String> type = new HashMap<>();
        for (String e : body.split("\\R")) {
            if (e.startsWith("# HELP")) {
                extractMetadata(help, e);
            } else if (e.startsWith("# TYPE")) {
                extractMetadata(type, e);
            } else if (e.isBlank() || e.startsWith("#")) {
                // Other comment or blank line - nothing to collect.
            } else {
                String[] parts = e.split("[{}]");
                String key;
                String value;
                Map<String, String> tags;
                if (parts.length >= 3) {
                    // key{tag="value",...} value
                    key = parts[0];
                    tags = Arrays.stream(parts[1].split(","))
                            .map(t -> t.split("="))
                            .collect(Collectors.toMap(i -> i[0],
                                    i -> i[1]
                                            .replaceAll("^\"", "")
                                            .replaceAll("\"$", "")
                            ));
                    value = parts[2].trim();
                } else {
                    // key value (metric without tags)
                    int idx = e.lastIndexOf(' ');
                    if (idx < 0) {
                        // Not a sample line (e.g. an error page returned in place of metrics).
                        continue;
                    }
                    key = e.substring(0, idx).trim();
                    value = e.substring(idx + 1).trim();
                    tags = Collections.emptyMap();
                }
                metrics.add(new PrometheusMetric(key, tags, value, type.get(key), help.get(key)));
            }
        }

        return metrics;
    }

    private static void extractMetadata(Map<String, String> target, String source) {
        String[] parts = source.split(" ");
        target.put(parts[2],
                Arrays.stream(Arrays.copyOfRange(parts, 3, parts.length))
                        .reduce("", (total, element) -> total + " " + element));
    }
}

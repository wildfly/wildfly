/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.shared.observability.signals;

import java.util.Collections;
import java.util.Map;

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
}

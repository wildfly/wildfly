/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.shared.observability.signals;

import java.util.Map;

public record PrometheusMetric(String key,
                               Map<String, String> tags,
                               String value,
                               String type,
                               String help) {
//    public PrometheusMetric(String key,
//                            Map<String, String> tags,
//                            String value,
//                            String type,
//                            String help) {
//        this.key = key;
//        this.tags = Collections.unmodifiableMap(tags);
//        this.value = value;
//        this.type = type;
//        this.help = help;
//    }

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

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
}

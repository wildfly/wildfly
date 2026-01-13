/*
 *
 *  * Copyright The WildFly Authors
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.jboss.as.test.shared.observability.signals;

import java.util.Map;
import java.util.Objects;

public record SimpleMetric(String name,
                           String description,
                           String value,
                           String type,
                           String unit,
                           Map<String, String> resourceAttributes,
                           String scopeName,
                           Map<String, String> tags) {
    // Determine a meter's uniqueness based on its name and tags
    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        SimpleMetric that = (SimpleMetric) o;
        return Objects.equals(name, that.name) && Objects.equals(tags, that.tags);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, tags);
    }
}

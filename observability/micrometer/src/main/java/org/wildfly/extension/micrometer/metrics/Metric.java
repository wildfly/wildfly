/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.micrometer.metrics;

import java.util.OptionalDouble;

/**
 * A representation of a metric.
 * It is possibe that the value can not be computed (e.g. if it is backed by a WildFly
 * management attribute value which is undefined).
 */
public interface Metric {
    OptionalDouble getValue();
}

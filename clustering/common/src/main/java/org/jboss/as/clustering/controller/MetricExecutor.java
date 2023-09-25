/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

/**
 * Encapsulates the execution of a runtime metric.
 * @author Paul Ferraro
 * @param <C> the metric execution context.
 */
public interface MetricExecutor<C> extends Executor<C, Metric<C>> {
}

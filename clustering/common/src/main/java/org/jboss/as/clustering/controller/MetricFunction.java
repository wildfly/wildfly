/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import java.util.function.Function;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.wildfly.common.function.ExceptionFunction;

/**
 * A functional view of a runtime metric.
 * @author Paul Ferraro
 * @param <T> the type of value provided by the service on which the given runtime metric operates
 * @param <V> the type of the value of which the given runtime metric operates
 */
public class MetricFunction<T, V> implements ExceptionFunction<T, ModelNode, OperationFailedException> {

    private final Function<T, V> mapper;
    private final Metric<V> metric;

    /**
     * Creates a functional view of the specified metric using the specified mapper.
     * @param mapper maps the value of a service to the value on which the given metric operates
     * @param metric a runtime metric
     */
    public MetricFunction(Function<T, V> mapper, Metric<V> metric) {
        this.mapper = mapper;
        this.metric = metric;
    }

    @Override
    public ModelNode apply(T value) throws OperationFailedException {
        V mapped = this.mapper.apply(value);
        return (mapped != null) ? this.metric.execute(mapped) : null;
    }
}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import java.util.function.Function;

import org.jboss.as.controller.ExpressionResolver;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.wildfly.common.function.ExceptionFunction;

/**
 * A functional view of a runtime operation.
 * @author Paul Ferraro
 * @param <T> the type of value provided by the service on which the given runtime operation operates
 * @param <V> the type of the value of which the given runtime operation operates
 */
public class OperationFunction<T, V> implements ExceptionFunction<T, ModelNode, OperationFailedException> {

    private final ExpressionResolver resolver;
    private final ModelNode operation;
    private final Function<T, V> mapper;
    private final Operation<V> executable;

    /**
     * Creates a functional view of the specified metric using the specified mapping function.
     * @param resolver an expression resolver
     * @param operation the management operation
     * @param mapper maps the value of a service to the value on which the given metric operates
     * @param executable a runtime operation
     */
    public OperationFunction(ExpressionResolver resolver, ModelNode operation, Function<T, V> mapper, Operation<V> executable) {
        this.resolver = resolver;
        this.operation = operation;
        this.mapper = mapper;
        this.executable = executable;
    }

    @Override
    public ModelNode apply(T value) throws OperationFailedException {
        V mapped = this.mapper.apply(value);
        return (mapped != null) ? this.executable.execute(this.resolver, this.operation, mapped) : null;
    }
}

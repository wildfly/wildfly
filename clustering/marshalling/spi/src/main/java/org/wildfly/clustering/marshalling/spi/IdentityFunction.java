/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.spi;

import java.util.function.Function;

/**
 * Behaves the same as {@link Function#identity()}, where the return type is a superclass of the function parameter.
 * @author Paul Ferraro
 */
public enum IdentityFunction implements Function<Object, Object> {
    INSTANCE;

    @Override
    public Object apply(Object value) {
        return value;
    }

    /**
     * Returns a function that returns its parameter.
     * @param <T> the parameter type
     * @param <R> the return type
     * @return a function that return its parameter
     */
    @SuppressWarnings("unchecked")
    public static <T extends R, R> Function<T, R> getInstance() {
        return (Function<T, R>) INSTANCE;
    }
}

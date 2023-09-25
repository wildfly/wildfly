/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.spi;

import java.util.function.DoubleFunction;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.LongFunction;

/**
 * A function that always results a constant value, ignoring its parameter.
 * @author Paul Ferraro
 */
public class ValueFunction<T, R> implements Function<T, R>, IntFunction<R>, LongFunction<R>, DoubleFunction<R> {
    private static final ValueFunction<Object, Void> VOID = new ValueFunction<>(null);

    /**
     * Returns a function that always returns a null result, regardless of input.
     * @param <T> the function parameter type
     * @return a function that always returns null
     */
    @SuppressWarnings("unchecked")
    public static <T> ValueFunction<T, Void> voidFunction() {
        return (ValueFunction<T, Void>) VOID;
    }

    private final R result;

    public ValueFunction(R result) {
        this.result = result;
    }

    @Override
    public R apply(T ignored) {
        return this.result;
    }

    @Override
    public R apply(double value) {
        return this.result;
    }

    @Override
    public R apply(long value) {
        return this.result;
    }

    @Override
    public R apply(int value) {
        return this.result;
    }
}

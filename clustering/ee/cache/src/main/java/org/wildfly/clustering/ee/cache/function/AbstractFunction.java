/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee.cache.function;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Function that operates on an operable object.
 * @author Paul Ferraro
 * @param <T> the operand type
 * @param <O> the operable object type
 */
public abstract class AbstractFunction<T, O> implements BiFunction<Object, O, O>, BiConsumer<O, T> {
    private final T operand;
    private final UnaryOperator<O> copier;
    private final Supplier<O> factory;
    private final Predicate<O> empty;

    public AbstractFunction(T operand, UnaryOperator<O> copier, Supplier<O> factory, Predicate<O> empty) {
        this.operand = operand;
        this.copier = copier;
        this.factory = factory;
        this.empty = empty;
    }

    @Override
    public O apply(Object key, O operable) {
        O result = (operable != null) ? this.copier.apply(operable) : this.factory.get();
        this.accept(result, this.operand);
        return !this.empty.test(result) ? result : null;
    }

    public T getOperand() {
        return this.operand;
    }

    @Override
    public int hashCode() {
        return this.operand.hashCode();
    }

    @Override
    public boolean equals(Object object) {
        if (!this.getClass().isInstance(object)) return false;
        return this.operand.equals(((AbstractFunction<?, ?>) object).operand);
    }

    @Override
    public String toString() {
        return String.format("%s[%s]", this.getClass().getSimpleName(), this.operand);
    }
}

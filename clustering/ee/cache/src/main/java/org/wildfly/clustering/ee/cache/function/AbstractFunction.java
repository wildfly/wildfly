/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee.cache.function;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
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
    private final Function<O, Boolean> empty;

    public AbstractFunction(T operand, UnaryOperator<O> copier, Supplier<O> factory, Function<O, Boolean> empty) {
        this.operand = operand;
        this.copier = copier;
        this.factory = factory;
        this.empty = empty;
    }

    @Override
    public O apply(Object key, O operable) {
        // Transactional caches must operate on a copy of the operable object
        O result = (operable != null) ? this.copier.apply(operable) : this.factory.get();
        this.accept(result, this.operand);
        return !this.empty.apply(result) ? result : null;
    }

    public T getOperand() {
        return this.operand;
    }
}

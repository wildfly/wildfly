/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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

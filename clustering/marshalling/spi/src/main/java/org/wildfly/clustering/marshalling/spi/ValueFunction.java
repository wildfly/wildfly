/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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

    @SuppressWarnings("unchecked")
    public static <T> ValueFunction<T, Void> nullFunction() {
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

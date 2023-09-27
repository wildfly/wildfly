/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.spi;

import java.util.function.DoubleFunction;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.LongFunction;
import java.util.function.Supplier;

/**
 * Adapts a Supplier to a Function ignoring it's parameter.
 * @author Paul Ferraro
 */
public class SupplierFunction<R> implements Function<Void, R>, IntFunction<R>, LongFunction<R>, DoubleFunction<R> {

    private final Supplier<R> supplier;

    public SupplierFunction(Supplier<R> supplier) {
        this.supplier = supplier;
    }

    @Override
    public R apply(Void ignored) {
        return this.supplier.get();
    }

    @Override
    public R apply(int value) {
        return this.supplier.get();
    }

    @Override
    public R apply(long value) {
        return this.supplier.get();
    }

    @Override
    public R apply(double value) {
        return this.supplier.get();
    }
}

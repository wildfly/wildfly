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

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.service;

import java.util.function.Function;

import org.jboss.msc.service.ServiceBuilder;

/**
 * A {@link SupplierDependency} that applies a mapping to the source dependency value.
 * @author Paul Ferraro
 */
public class FunctionSupplierDependency<T, R> implements SupplierDependency<R> {

    private final SupplierDependency<T> dependency;
    private final Function<T, R> mapper;

    public FunctionSupplierDependency(SupplierDependency<T> dependency, Function<T, R> mapper) {
        this.dependency = dependency;
        this.mapper = mapper;
    }

    @Override
    public R get() {
        return this.mapper.apply(this.dependency.get());
    }

    @Override
    public <V> ServiceBuilder<V> register(ServiceBuilder<V> builder) {
        return this.dependency.register(builder);
    }
}

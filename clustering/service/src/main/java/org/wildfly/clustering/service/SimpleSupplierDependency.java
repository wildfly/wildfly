/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.service;

import org.jboss.msc.service.ServiceBuilder;

/**
 * A {@link Dependency} that supplies a static value
 * @author Paul Ferraro
 */
public class SimpleSupplierDependency<V> implements SupplierDependency<V> {

    private final V value;

    public SimpleSupplierDependency(V value) {
        this.value = value;
    }

    @Override
    public V get() {
        return this.value;
    }

    @Override
    public <T> ServiceBuilder<T> register(ServiceBuilder<T> builder) {
        // Nothing to register - value is already known.
        return builder;
    }
}

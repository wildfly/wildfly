/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.service;

import java.util.function.Supplier;

import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;

/**
 * Encapsulates a {@link Dependency} on a {@link org.jboss.msc.Service} that supplies a value.
 * @author Paul Ferraro
 */
public class ServiceSupplierDependency<V> extends SimpleServiceNameProvider implements SupplierDependency<V> {

    private volatile Supplier<V> supplier;

    public ServiceSupplierDependency(ServiceName name) {
        super(name);
    }

    public ServiceSupplierDependency(ServiceNameProvider provider) {
        super(provider.getServiceName());
    }

    @Override
    public V get() {
        return this.supplier.get();
    }

    @Override
    public <T> ServiceBuilder<T> register(ServiceBuilder<T> builder) {
        this.supplier = builder.requires(this.getServiceName());
        return builder;
    }
}

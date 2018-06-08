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

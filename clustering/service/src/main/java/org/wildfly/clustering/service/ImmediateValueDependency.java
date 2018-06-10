/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

import org.jboss.msc.service.ServiceBuilder;

/**
 * A trivial {@link ValueDependency} whose value is immediately available.
 * @author Paul Ferraro
 * @deprecated Replaced by {@link SimpleSupplierDependency}.
 */
@Deprecated
public class ImmediateValueDependency<V> implements ValueDependency<V> {

    private final V value;

    public ImmediateValueDependency(V value) {
        this.value = value;
    }

    @Override
    public V getValue() {
        return this.value;
    }

    @Override
    public <T> ServiceBuilder<T> register(ServiceBuilder<T> builder) {
        // Nothing to register
        return builder;
    }
}

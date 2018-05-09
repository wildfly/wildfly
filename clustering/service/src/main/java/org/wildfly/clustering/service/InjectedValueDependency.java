/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

import org.jboss.msc.service.ServiceName;
import org.jboss.msc.value.InjectedValue;

/**
 * Service dependency whose provided value is made available via injection.
 * @author Paul Ferraro
 * @deprecated Replaced by {@link ServiceSupplierDependency}.
 */
@Deprecated
public class InjectedValueDependency<T> extends InjectorDependency<T> implements ValueDependency<T> {

    private final InjectedValue<T> value;

    public InjectedValueDependency(ServiceNameProvider provider, Class<T> targetClass) {
        this(provider.getServiceName(), targetClass, new InjectedValue<T>());
    }

    public InjectedValueDependency(ServiceName name, Class<T> targetClass) {
        this(name, targetClass, new InjectedValue<T>());
    }

    private InjectedValueDependency(ServiceName name, Class<T> targetClass, InjectedValue<T> value) {
        super(name, targetClass, value);
        this.value = value;
    }

    @Override
    public T getValue() {
        return this.value.getValue();
    }
}

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

import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;

/**
 * Service dependency requiring an injector.
 * @author Paul Ferraro
 * @deprecated Replaced by {@link ServiceSupplierDependency}.
 */
@Deprecated
public class InjectorDependency<T> implements Dependency {
    private final ServiceName name;
    private final Class<T> targetClass;
    private final Injector<T> injector;

    public InjectorDependency(ServiceNameProvider provider, Class<T> targetClass, Injector<T> injector) {
        this(provider.getServiceName(), targetClass, injector);
    }

    public InjectorDependency(ServiceName name, Class<T> targetClass, Injector<T> injector) {
        this.name = name;
        this.targetClass = targetClass;
        this.injector = injector;
    }

    @Override
    public <X> ServiceBuilder<X> register(ServiceBuilder<X> builder) {
        return builder.addDependency(this.name, this.targetClass, this.injector);
    }
}

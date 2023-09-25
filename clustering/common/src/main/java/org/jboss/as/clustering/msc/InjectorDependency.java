/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.msc;

import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.service.Dependency;
import org.wildfly.clustering.service.ServiceNameProvider;
import org.wildfly.clustering.service.ServiceSupplierDependency;

/**
 * Service dependency requiring an injector.
 * @author Paul Ferraro
 * @deprecated Replaced by {@link ServiceSupplierDependency}.
 */
@Deprecated(forRemoval = true)
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

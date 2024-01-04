/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.msc;

import org.jboss.msc.service.ServiceName;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.clustering.service.ServiceNameProvider;
import org.wildfly.clustering.service.ServiceSupplierDependency;

/**
 * Service dependency whose provided value is made available via injection.
 * @author Paul Ferraro
 * @deprecated Replaced by {@link ServiceSupplierDependency}.
 */
@Deprecated(forRemoval = true)
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

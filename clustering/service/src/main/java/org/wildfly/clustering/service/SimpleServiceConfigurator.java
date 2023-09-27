/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.service;

import java.util.function.Consumer;

import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;

/**
 * Configures a simple {@link Service} that provides a static value.
 * @author Paul Ferraro
 */
public class SimpleServiceConfigurator<T> extends SimpleServiceNameProvider implements ServiceConfigurator {

    private final T value;

    public SimpleServiceConfigurator(ServiceName name, T value) {
        super(name);
        this.value = value;
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceBuilder<?> builder = target.addService(this.getServiceName());
        Consumer<T> injector = builder.provides(this.getServiceName());
        return builder.setInstance(Service.newInstance(injector, this.value));
    }
}

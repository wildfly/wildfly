/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jboss.as.clustering.controller.ResourceServiceConfigurator;
import org.jboss.as.clustering.controller.ResourceServiceNameFactory;
import org.jboss.as.controller.PathAddress;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.service.Dependency;
import org.wildfly.clustering.service.FunctionalService;

/**
 * Configures a service supplying a cache component.
 * @author Paul Ferraro
 */
public abstract class ComponentServiceConfigurator<C> implements ResourceServiceConfigurator, Supplier<C>, Dependency {

    private final ServiceName name;
    private final ServiceController.Mode initialMode;

    protected ComponentServiceConfigurator(ResourceServiceNameFactory factory, PathAddress address) {
        this(factory, address, ServiceController.Mode.ON_DEMAND);
    }

    protected ComponentServiceConfigurator(ResourceServiceNameFactory factory, PathAddress address, ServiceController.Mode initialMode) {
        this.name = factory.getServiceName(address.getParent());
        this.initialMode = initialMode;
    }

    @Override
    public ServiceName getServiceName() {
        return this.name;
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceBuilder<?> builder = target.addService(this.getServiceName());
        Consumer<C> component = this.register(builder).provides(this.getServiceName());
        Service service = new FunctionalService<>(component, Function.identity(), this);
        return builder.setInstance(service).setInitialMode(this.initialMode);
    }

    @Override
    public <T> ServiceBuilder<T> register(ServiceBuilder<T> builder) {
        return builder;
    }
}

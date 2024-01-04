/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.service.FunctionalService;
import org.wildfly.clustering.service.SimpleServiceNameProvider;

/**
 * Configures a service that provides a value created from a generic factory and mapper.
 * @author Paul Ferraro
 * @param <T> the source type of the mapped value provided by the installed service
 * @param <V> the type of the value provided by the installed service
 */
public class FunctionalCapabilityServiceConfigurator<T, V> extends SimpleServiceNameProvider implements CapabilityServiceConfigurator {

    private final Function<T, V> mapper;
    private final Supplier<T> factory;

    public FunctionalCapabilityServiceConfigurator(ServiceName name, Function<T, V> mapper, Supplier<T> factory) {
        super(name);
        this.mapper = mapper;
        this.factory = factory;
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceName name = this.getServiceName();
        ServiceBuilder<?> builder = target.addService(name);
        Consumer<V> injector = builder.provides(name);
        return builder.setInstance(new FunctionalService<>(injector, this.mapper, this.factory));
    }
}

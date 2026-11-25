/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.singleton.server;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.wildfly.clustering.server.service.Service;
import org.wildfly.clustering.singleton.Singleton;

/**
 * Logic common to current and legacy {@link SingletonService} implementations.
 * @author Paul Ferraro
 */
public abstract class AbstractSingletonService<C extends SingletonContext, S extends Service> implements org.jboss.msc.Service, Supplier<SingletonContextRegistration<C>> {
    private final SingletonServiceContext context;
    private final Function<ServiceTarget, S> serviceFactory;
    private final BiFunction<SingletonServiceContext, S, SingletonContextRegistration<C>> contextFactory;
    private final Consumer<Singleton> singleton;

    private volatile SingletonContextRegistration<C> registration;

    public AbstractSingletonService(SingletonServiceContext context, Function<ServiceTarget, S> serviceFactory, BiFunction<SingletonServiceContext, S, SingletonContextRegistration<C>> contextFactory, Consumer<Singleton> singleton) {
        this.context = context;
        this.serviceFactory = serviceFactory;
        this.contextFactory = contextFactory;
        this.singleton = singleton;
    }

    @Override
    public void start(StartContext context) throws StartException {
        this.registration = this.contextFactory.apply(this.context, this.serviceFactory.apply(context.getChildTarget()));
        this.singleton.accept(this.registration);
    }

    @Override
    public void stop(StopContext context) {
        this.singleton.accept(null);
        this.registration.close();
    }

    @Override
    public SingletonContextRegistration<C> get() {
        return this.registration;
    }
}

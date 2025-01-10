/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.singleton.server;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.singleton.Singleton;
import org.wildfly.clustering.singleton.service.SingletonServiceBuilder;
import org.wildfly.clustering.singleton.service.SingletonServiceController;
import org.wildfly.common.function.Functions;
import org.wildfly.service.AsyncServiceBuilder;

/**
 * A service builder that installs a distributed singleton service.
 * @author Paul Ferraro
 */
public class DistributedSingletonServiceBuilder<T> extends AbstractSingletonServiceBuilder<T> {

    private final ServiceName id = ServiceName.parse(UUID.randomUUID().toString());
    private final Function<ServiceTarget, ServiceBuilder<?>> builderFactory;
    private final List<Map.Entry<ServiceName[], Consumer<Consumer<?>>>> injectors = new LinkedList<>();
    private final SingletonServiceBuilderContext context;
    private final Consumer<Singleton> singleton;
    private final SingletonReference reference = new SingletonReference();

    public DistributedSingletonServiceBuilder(ServiceBuilder<T> builder, Function<ServiceTarget, ServiceBuilder<?>> builderFactory, SingletonServiceBuilderContext context, Function<ServiceBuilder<?>, Consumer<Singleton>> singletonFactory) {
        super(new AsyncServiceBuilder<>(builder, builder.requires(ServiceName.parse("org.wildfly.management.executor"))), context);
        this.builderFactory = builderFactory;
        this.context = context;
        Consumer<Singleton> injector = builder.provides(this.id);
        this.singleton = singletonFactory.apply(builder).andThen(this.reference).andThen(injector);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <V> Consumer<V> provides(ServiceName... names) {
        if (names.length == 0) return Functions.discardingConsumer();
        this.context.setServiceName(names[0]);
        AtomicReference<Consumer<V>> injector = new AtomicReference<>();
        this.injectors.add(Map.entry(names, ((AtomicReference<Consumer<?>>) (AtomicReference<?>) injector)::set));
        return new Consumer<>() {
            @Override
            public void accept(V value) {
                injector.get().accept(value);
            }
        };
    }

    @Override
    public SingletonServiceBuilder<T> setInstance(Service service) {
        this.context.setService(service);
        return this;
    }

    @Override
    public SingletonServiceController<T> install() {
        // Identify singleton service via its id if caller did not provide one
        this.context.setServiceName(this.id);
        ServiceController<T> controller = this.getDelegate().setInstance(new DistributedSingletonService(this.context, this.builderFactory, this.injectors, this.singleton)).install();
        return new DistributedSingletonServiceController<>(controller, this.reference);
    }
}

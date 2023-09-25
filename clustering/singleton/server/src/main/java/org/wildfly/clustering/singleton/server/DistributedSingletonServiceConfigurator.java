/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.singleton.server;

import java.util.AbstractMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jboss.msc.Service;
import org.jboss.msc.service.DelegatingServiceBuilder;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.provider.ServiceProviderRegistry;
import org.wildfly.clustering.server.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.service.AsyncServiceConfigurator;
import org.wildfly.clustering.service.CompositeDependency;
import org.wildfly.clustering.service.SimpleServiceNameProvider;
import org.wildfly.clustering.service.SupplierDependency;
import org.wildfly.clustering.singleton.Singleton;
import org.wildfly.clustering.singleton.SingletonElectionListener;
import org.wildfly.clustering.singleton.SingletonElectionPolicy;
import org.wildfly.clustering.singleton.election.SimpleSingletonElectionPolicy;
import org.wildfly.clustering.singleton.service.SingletonServiceBuilder;
import org.wildfly.clustering.singleton.service.SingletonServiceConfigurator;

/**
 * Distributed {@link SingletonServiceConfigurator} implementation that uses JBoss MSC 1.4.x service installation.
 * @author Paul Ferraro
 */
public class DistributedSingletonServiceConfigurator extends SimpleServiceNameProvider implements SingletonServiceConfigurator, DistributedSingletonServiceContext, Supplier<Group> {

    private final SupplierDependency<ServiceProviderRegistry<ServiceName>> registry;
    private final SupplierDependency<CommandDispatcherFactory> dispatcherFactory;

    private volatile SingletonElectionPolicy electionPolicy = new SimpleSingletonElectionPolicy();
    private volatile SingletonElectionListener electionListener;
    private volatile int quorum = 1;

    public DistributedSingletonServiceConfigurator(ServiceName name, DistributedSingletonServiceConfiguratorContext context) {
        super(name);
        this.registry = context.getServiceProviderRegistryDependency();
        this.dispatcherFactory = context.getCommandDispatcherFactoryDependency();
        this.electionListener = new DefaultSingletonElectionListener(name, this);
    }

    @Override
    public Group get() {
        return this.registry.get().getGroup();
    }

    @Override
    public SingletonServiceBuilder<?> build(ServiceTarget target) {
        ServiceName name = this.getServiceName().append("singleton");
        ServiceBuilder<?> builder = new AsyncServiceConfigurator(name).build(target);
        Consumer<Singleton> singleton = builder.provides(name);
        return new DistributedSingletonServiceBuilder<>(this, new CompositeDependency(this.registry, this.dispatcherFactory).register(builder), singleton);
    }

    @Override
    public SingletonServiceConfigurator requireQuorum(int quorum) {
        this.quorum = quorum;
        return this;
    }

    @Override
    public SingletonServiceConfigurator electionPolicy(SingletonElectionPolicy policy) {
        this.electionPolicy = policy;
        return this;
    }

    @Override
    public SingletonServiceConfigurator electionListener(SingletonElectionListener listener) {
        this.electionListener = listener;
        return this;
    }

    @Override
    public Supplier<ServiceProviderRegistry<ServiceName>> getServiceProviderRegistry() {
        return this.registry;
    }

    @Override
    public Supplier<CommandDispatcherFactory> getCommandDispatcherFactory() {
        return this.dispatcherFactory;
    }

    @Override
    public SingletonElectionPolicy getElectionPolicy() {
        return this.electionPolicy;
    }

    @Override
    public SingletonElectionListener getElectionListener() {
        return this.electionListener;
    }

    @Override
    public int getQuorum() {
        return this.quorum;
    }

    private static class DistributedSingletonServiceBuilder<T> extends DelegatingServiceBuilder<T> implements SingletonServiceBuilder<T> {

        private final DistributedSingletonServiceContext context;
        private final Consumer<Singleton> singleton;
        private final List<Map.Entry<ServiceName[], DeferredInjector<?>>> injectors = new LinkedList<>();
        private Service service = Service.NULL;

        DistributedSingletonServiceBuilder(DistributedSingletonServiceContext context, ServiceBuilder<T> builder, Consumer<Singleton> singleton) {
            super(builder);
            this.context = context;
            this.singleton = singleton;
        }

        @Override
        public <V> Consumer<V> provides(ServiceName... names) {
            DeferredInjector<V> injector = new DeferredInjector<>();
            this.injectors.add(new AbstractMap.SimpleImmutableEntry<>(names, injector));
            return injector;
        }

        @Override
        public ServiceBuilder<T> setInstance(Service service) {
            this.service = service;
            return this;
        }

        @Override
        public ServiceController<T> install() {
            return this.getDelegate().setInstance(new DistributedSingletonService(this.context, this.service, this.singleton, this.injectors)).install();
        }
    }
}

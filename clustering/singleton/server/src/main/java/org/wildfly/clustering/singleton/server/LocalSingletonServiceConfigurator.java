/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.singleton.server;

import java.util.function.Supplier;

import org.jboss.msc.Service;
import org.jboss.msc.service.DelegatingServiceBuilder;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.service.SimpleServiceNameProvider;
import org.wildfly.clustering.service.SupplierDependency;
import org.wildfly.clustering.singleton.SingletonElectionListener;
import org.wildfly.clustering.singleton.SingletonElectionPolicy;
import org.wildfly.clustering.singleton.service.SingletonServiceBuilder;
import org.wildfly.clustering.singleton.service.SingletonServiceConfigurator;

/**
 * Local {@link SingletonServiceConfigurator} implementation that uses JBoss MSC 1.4.x service installation.
 * @author Paul Ferraro
 */
public class LocalSingletonServiceConfigurator extends SimpleServiceNameProvider implements SingletonServiceConfigurator, LocalSingletonServiceContext {

    private final SupplierDependency<Group> group;
    private volatile SingletonElectionListener listener;

    public LocalSingletonServiceConfigurator(ServiceName name, LocalSingletonServiceConfiguratorContext context) {
        super(name);
        this.group = context.getGroupDependency();
        this.listener = new DefaultSingletonElectionListener(name, this.group);
    }

    @Override
    public SingletonServiceBuilder<?> build(ServiceTarget target) {
        ServiceBuilder<?> builder = target.addService(this.getServiceName());
        return new LocalSingletonServiceBuilder<>(this.group.register(builder), this);
    }

    @Override
    public SingletonServiceConfigurator requireQuorum(int quorum) {
        // Quorum requirements are inconsequential to a local singleton
        return this;
    }

    @Override
    public SingletonServiceConfigurator electionPolicy(SingletonElectionPolicy policy) {
        // Election policies are inconsequential to a local singleton
        return this;
    }

    @Override
    public SingletonServiceConfigurator electionListener(SingletonElectionListener listener) {
        this.listener = listener;
        return this;
    }

    @Override
    public Supplier<Group> getGroup() {
        return this.group;
    }

    @Override
    public SingletonElectionListener getElectionListener() {
        return this.listener;
    }

    private static class LocalSingletonServiceBuilder<T> extends DelegatingServiceBuilder<T> implements SingletonServiceBuilder<T> {

        private final LocalSingletonServiceContext context;
        private Service service = Service.NULL;

        LocalSingletonServiceBuilder(ServiceBuilder<T> builder, LocalSingletonServiceContext context) {
            super(builder);
            this.context = context;
        }

        @Override
        public ServiceBuilder<T> setInstance(Service service) {
            this.service = service;
            return this;
        }

        @Override
        public ServiceController<T> install() {
            return this.getDelegate().setInstance(new LocalSingletonService(this.service, this.context)).install();
        }
    }
}

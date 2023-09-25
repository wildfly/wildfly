/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.singleton.server;

import java.util.function.Supplier;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.service.SimpleServiceNameProvider;
import org.wildfly.clustering.service.SupplierDependency;
import org.wildfly.clustering.singleton.SingletonElectionListener;
import org.wildfly.clustering.singleton.SingletonElectionPolicy;
import org.wildfly.clustering.singleton.SingletonService;
import org.wildfly.clustering.singleton.SingletonServiceBuilder;
import org.wildfly.clustering.singleton.service.SingletonServiceConfigurator;

/**
 * Local {@link SingletonServiceConfigurator} implementation that uses JBoss MSC 1.3.x service installation.
 * @author Paul Ferraro
 */
@Deprecated
public class LocalSingletonServiceBuilder<T> extends SimpleServiceNameProvider implements SingletonServiceBuilder<T>, LocalSingletonServiceContext {

    private final Service<T> service;
    private final SupplierDependency<Group> group;
    private volatile SingletonElectionListener listener;

    public LocalSingletonServiceBuilder(ServiceName name, Service<T> service, LocalSingletonServiceConfiguratorContext context) {
        super(name);
        this.service = service;
        this.group = context.getGroupDependency();
        this.listener = new DefaultSingletonElectionListener(name, this.group);
    }

    @Override
    public SingletonServiceBuilder<T> requireQuorum(int quorum) {
        // Quorum requirements are inconsequential to a local singleton
        return this;
    }

    @Override
    public SingletonServiceBuilder<T> electionPolicy(SingletonElectionPolicy policy) {
        // Election policies are inconsequential to a local singleton
        return this;
    }

    @Override
    public SingletonServiceBuilder<T> electionListener(SingletonElectionListener listener) {
        this.listener = listener;
        return this;
    }

    @Override
    public ServiceBuilder<T> build(ServiceTarget target) {
        SingletonService<T> service = new LocalLegacySingletonService<>(this.service, this);
        return this.group.register(target.addService(this.getServiceName(), service));
    }

    @Override
    public Supplier<Group> getGroup() {
        return this.group;
    }

    @Override
    public SingletonElectionListener getElectionListener() {
        return this.listener;
    }
}

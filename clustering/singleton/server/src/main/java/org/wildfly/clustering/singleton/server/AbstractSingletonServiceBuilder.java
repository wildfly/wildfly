/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.singleton.server;

import org.jboss.msc.Service;
import org.jboss.msc.service.DelegatingServiceBuilder;
import org.jboss.msc.service.LifecycleListener;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.wildfly.clustering.singleton.election.SingletonElectionListener;
import org.wildfly.clustering.singleton.election.SingletonElectionPolicy;
import org.wildfly.clustering.singleton.service.SingletonServiceBuilder;
import org.wildfly.clustering.singleton.service.SingletonServiceController;

/**
 * @author Paul Ferraro
 */
public abstract class AbstractSingletonServiceBuilder<T> extends DelegatingServiceBuilder<T> implements SingletonServiceBuilder<T> {

    private final SingletonServiceBuilderContext context;

    public AbstractSingletonServiceBuilder(ServiceBuilder<T> builder, SingletonServiceBuilderContext context) {
        super(builder);
        context.getCommandDispatcherFactoryDependency().accept(builder);
        context.getServiceProviderRegistrarDependency().accept(builder);
        this.context = context;
    }

    @Override
    public abstract SingletonServiceBuilder<T> setInstance(Service service);

    @Override
    public abstract SingletonServiceController<T> install();

    @Override
    public SingletonServiceBuilder<T> requireQuorum(int quorum) {
        this.context.setQuorum(quorum);
        return this;
    }

    @Override
    public SingletonServiceBuilder<T> withElectionPolicy(SingletonElectionPolicy policy) {
        this.context.setElectionPolicy(policy);
        return this;
    }

    @Override
    public SingletonServiceBuilder<T> withElectionListener(SingletonElectionListener listener) {
        this.context.setElectionListener(listener);
        return this;
    }

    @Override
    public SingletonServiceBuilder<T> addListener(LifecycleListener listener) {
        this.getDelegate().addListener(listener);
        return this;
    }

    @Override
    public SingletonServiceBuilder<T> setInitialMode(ServiceController.Mode mode) {
        this.getDelegate().setInitialMode(mode);
        return this;
    }
}

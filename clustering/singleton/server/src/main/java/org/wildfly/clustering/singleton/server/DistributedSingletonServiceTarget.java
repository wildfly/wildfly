/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.singleton.server;

import java.util.function.Consumer;
import java.util.function.Function;

import org.jboss.msc.service.DelegatingServiceTarget;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.singleton.Singleton;
import org.wildfly.clustering.singleton.service.SingletonServiceBuilder;
import org.wildfly.clustering.singleton.service.SingletonServiceTarget;

/**
 * A service target for installing distributed singleton services.
 * @author Paul Ferraro
 */
public class DistributedSingletonServiceTarget extends DelegatingServiceTarget implements SingletonServiceTarget {

    private final SingletonServiceTargetContext context;
    private final Function<ServiceBuilder<?>, Consumer<Singleton>> singletonFactory;

    public DistributedSingletonServiceTarget(ServiceTarget target, SingletonServiceTargetContext context, Function<ServiceBuilder<?>, Consumer<Singleton>> singletonFactory) {
        super(target);
        this.context = context;
        this.singletonFactory = singletonFactory;
    }

    @Override
    public SingletonServiceBuilder<?> addService() {
        return new DistributedSingletonServiceBuilder<>(this.getDelegate().addService(), ServiceTarget::addService, new DefaultSingletonServiceBuilderContext(this.context), this.singletonFactory);
    }

    @Deprecated
    @Override
    public SingletonServiceBuilder<?> addService(ServiceName name) {
        return new DistributedSingletonServiceBuilder<>(this.getDelegate().addService(), target -> target.addService(name), new DefaultSingletonServiceBuilderContext(name, this.context), this.singletonFactory);
    }

    @Deprecated
    @Override
    public <T> SingletonServiceBuilder<T> addService(ServiceName name, org.jboss.msc.service.Service<T> service) {
        SingletonReference reference = new SingletonReference();
        SingletonServiceBuilderContext context = new DefaultSingletonServiceBuilderContext(name, this.context);
        org.jboss.msc.service.Service<T> singletonService = new LegacyDistributedSingletonService<>(context, service, null, reference);
        return new LegacySingletonServiceBuilder<>(reference, context, this.getDelegate().addService(name, singletonService));
    }

    @Override
    public SingletonServiceTarget subTarget() {
        return new DistributedSingletonServiceTarget(super.subTarget(), this.context, this.singletonFactory);
    }
}

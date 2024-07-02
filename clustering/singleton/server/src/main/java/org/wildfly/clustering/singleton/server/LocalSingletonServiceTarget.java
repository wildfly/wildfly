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
import org.wildfly.clustering.server.GroupMember;
import org.wildfly.clustering.singleton.Singleton;
import org.wildfly.clustering.singleton.service.SingletonServiceBuilder;
import org.wildfly.clustering.singleton.service.SingletonServiceTarget;
import org.wildfly.service.ServiceDependency;

/**
 * A service target for installing local singleton services.
 * @author Paul Ferraro
 */
public class LocalSingletonServiceTarget extends DelegatingServiceTarget implements SingletonServiceTarget {

    private final ServiceDependency<GroupMember> member;
    private final Function<ServiceBuilder<?>, Consumer<Singleton>> singletonFactory;

    public LocalSingletonServiceTarget(ServiceTarget target, ServiceDependency<GroupMember> member, Function<ServiceBuilder<?>, Consumer<Singleton>> singletonFactory) {
        super(target);
        this.member = member;
        this.singletonFactory = singletonFactory;
    }

    @Override
    public SingletonServiceBuilder<?> addService() {
        return new LocalSingletonServiceBuilder<>(this.getDelegate().addService(), this.member, this.singletonFactory);
    }

    @Deprecated
    @Override
    public SingletonServiceBuilder<?> addService(ServiceName name) {
        return new LocalSingletonServiceBuilder<>(this.getDelegate().addService(name), this.member, this.singletonFactory);
    }

    @Deprecated
    @Override
    public <T> SingletonServiceBuilder<T> addService(ServiceName name, org.jboss.msc.service.Service<T> service) {
        return new LocalSingletonServiceBuilder<>(this.getDelegate().addService(name, service), this.member, this.singletonFactory);
    }

    @Override
    public SingletonServiceTarget subTarget() {
        return new LocalSingletonServiceTarget(this.getDelegate().subTarget(), this.member, this.singletonFactory);
    }
}

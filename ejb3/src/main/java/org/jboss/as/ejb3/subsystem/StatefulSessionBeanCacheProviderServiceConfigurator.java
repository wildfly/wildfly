/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.subsystem;

import org.jboss.as.clustering.controller.CapabilityServiceNameProvider;
import org.jboss.as.clustering.controller.ResourceServiceConfigurator;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.ejb3.component.stateful.StatefulSessionComponentInstance;
import org.jboss.as.ejb3.component.stateful.cache.StatefulSessionBeanCacheProvider;
import org.jboss.ejb.client.SessionID;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.service.FunctionalService;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Base class which provides configure(), build() methods used to allow SimpleResourceServiceHandler to install
 * a CacheFactoryBuilder service.
 *
 * The class installs the service using:
 * - a service name obtained from a capability and a path address, and
 * - a service value obtained from this class, via a Supplier<CacheFactoryBuilder>
 *
 * Subclasses should implement the get() method to return the CacheFactoryBuilder.
 */
public abstract class StatefulSessionBeanCacheProviderServiceConfigurator extends CapabilityServiceNameProvider implements ResourceServiceConfigurator, Supplier<StatefulSessionBeanCacheProvider<SessionID, StatefulSessionComponentInstance>> {

    public StatefulSessionBeanCacheProviderServiceConfigurator(PathAddress address) {
        super(StatefulSessionBeanCacheProviderResourceDefinition.Capability.CACHE_FACTORY, address);
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceName name = this.getServiceName();
        ServiceBuilder<?> builder = target.addService(name);
        Consumer<StatefulSessionBeanCacheProvider<SessionID, StatefulSessionComponentInstance>> provider = builder.provides(name);
        Service service = new FunctionalService<>(provider, Function.identity(), this);
        return builder.setInstance(service).setInitialMode(ServiceController.Mode.ON_DEMAND);
    }
}

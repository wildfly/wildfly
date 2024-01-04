/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.singleton.server;

import java.io.Serializable;
import java.util.function.Consumer;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.server.service.ClusteringCacheRequirement;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.SimpleServiceNameProvider;
import org.wildfly.clustering.singleton.service.SingletonServiceConfiguratorFactory;

/**
 * Configures a service providing a local {@link org.wildfly.clustering.singleton.service.SingletonServiceConfiguratorFactory}.
 * @author Paul Ferraro
 */
public class LocalSingletonServiceConfiguratorFactoryServiceConfigurator<T extends Serializable> extends SimpleServiceNameProvider implements CapabilityServiceConfigurator, LocalSingletonServiceConfiguratorFactoryContext {

    private final String containerName;
    private final String cacheName;

    private volatile ServiceName groupServiceName;

    public LocalSingletonServiceConfiguratorFactoryServiceConfigurator(ServiceName name, String containerName, String cacheName) {
        super(name);
        this.containerName = containerName;
        this.cacheName = cacheName;
    }

    @Override
    public ServiceConfigurator configure(CapabilityServiceSupport support) {
        this.groupServiceName = ClusteringCacheRequirement.GROUP.getServiceName(support, this.containerName, this.cacheName);
        return this;
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceBuilder<?> builder = target.addService(this.getServiceName());
        Consumer<SingletonServiceConfiguratorFactory> factory = builder.provides(this.getServiceName());
        Service service = Service.newInstance(factory, new LocalSingletonServiceBuilderFactory(this));
        return builder.setInstance(service);
    }

    @Override
    public ServiceName getGroupServiceName() {
        return this.groupServiceName;
    }
}

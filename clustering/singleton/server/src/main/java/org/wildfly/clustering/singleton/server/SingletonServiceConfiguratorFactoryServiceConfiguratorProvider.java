/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.singleton.server;

import java.util.List;

import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.server.service.CacheCapabilityServiceConfiguratorFactory;
import org.wildfly.clustering.server.service.CacheServiceConfiguratorProvider;
import org.wildfly.clustering.service.IdentityServiceConfigurator;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.singleton.service.SingletonCacheRequirement;
import org.wildfly.clustering.singleton.service.SingletonServiceConfiguratorFactory;

/**
 * @author Paul Ferraro
 */
public class SingletonServiceConfiguratorFactoryServiceConfiguratorProvider implements CacheServiceConfiguratorProvider {

    private final CacheCapabilityServiceConfiguratorFactory<SingletonServiceConfiguratorFactory> factory;

    protected SingletonServiceConfiguratorFactoryServiceConfiguratorProvider(CacheCapabilityServiceConfiguratorFactory<SingletonServiceConfiguratorFactory> factory) {
        this.factory = factory;
    }

    @Override
    public Iterable<ServiceConfigurator> getServiceConfigurators(CapabilityServiceSupport support, String containerName, String cacheName) {
        ServiceName name = SingletonCacheRequirement.SINGLETON_SERVICE_CONFIGURATOR_FACTORY.getServiceName(support, containerName, cacheName);
        ServiceConfigurator configurator = this.factory.createServiceConfigurator(name, containerName, cacheName).configure(support);
        @SuppressWarnings("removal")
        ServiceName legacyName = SingletonCacheRequirement.SINGLETON_SERVICE_BUILDER_FACTORY.getServiceName(support, containerName, cacheName);
        ServiceConfigurator legacyConfigurator = new IdentityServiceConfigurator<>(legacyName, name);
        return List.of(configurator, legacyConfigurator);
    }
}

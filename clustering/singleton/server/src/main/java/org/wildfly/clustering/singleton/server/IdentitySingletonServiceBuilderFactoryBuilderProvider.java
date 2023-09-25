/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.singleton.server;

import java.util.List;

import org.jboss.as.clustering.controller.IdentityLegacyCapabilityServiceConfigurator;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.msc.service.ServiceName;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.server.service.IdentityCacheServiceConfiguratorProvider;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.singleton.SingletonServiceBuilderFactory;
import org.wildfly.clustering.singleton.service.SingletonCacheRequirement;

/**
 * @author Paul Ferraro
 */
@Deprecated
@MetaInfServices(IdentityCacheServiceConfiguratorProvider.class)
public class IdentitySingletonServiceBuilderFactoryBuilderProvider implements IdentityCacheServiceConfiguratorProvider {

    @Override
    public Iterable<ServiceConfigurator> getServiceConfigurators(CapabilityServiceSupport support, String containerName, String cacheName, String targetCacheName) {
        ServiceName name = SingletonCacheRequirement.SINGLETON_SERVICE_BUILDER_FACTORY.getServiceName(support, containerName, cacheName);
        return List.of(new IdentityLegacyCapabilityServiceConfigurator<>(name, SingletonServiceBuilderFactory.class, SingletonCacheRequirement.SINGLETON_SERVICE_BUILDER_FACTORY, containerName, targetCacheName).configure(support));
    }

    @Override
    public String toString() {
        return this.getClass().getName();
    }
}

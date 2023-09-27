/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.singleton.server;

import java.util.List;

import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.msc.service.ServiceName;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.server.service.IdentityCacheServiceConfiguratorProvider;
import org.wildfly.clustering.service.IdentityServiceConfigurator;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.singleton.service.SingletonCacheRequirement;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(IdentityCacheServiceConfiguratorProvider.class)
public class IdentitySingletonServiceConfiguratorFactoryServiceConfiguratorProvider implements IdentityCacheServiceConfiguratorProvider {

    @Override
    public Iterable<ServiceConfigurator> getServiceConfigurators(CapabilityServiceSupport support, String containerName, String cacheName, String targetCacheName) {
        ServiceName name = SingletonCacheRequirement.SINGLETON_SERVICE_CONFIGURATOR_FACTORY.getServiceName(support, containerName, cacheName);
        ServiceName targetName = SingletonCacheRequirement.SINGLETON_SERVICE_CONFIGURATOR_FACTORY.getServiceName(support, containerName, targetCacheName);
        ServiceConfigurator configurator = new IdentityServiceConfigurator<>(name, targetName);
        return List.of(configurator);
    }
}

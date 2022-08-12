/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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

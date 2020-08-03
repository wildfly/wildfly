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

package org.wildfly.clustering.server.singleton;

import java.util.Collections;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.clustering.controller.IdentityCapabilityServiceConfigurator;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.ee.CompositeIterable;
import org.wildfly.clustering.server.CacheCapabilityServiceConfiguratorFactory;
import org.wildfly.clustering.server.CacheRequirementServiceConfiguratorProvider;
import org.wildfly.clustering.service.ServiceNameRegistry;
import org.wildfly.clustering.singleton.service.SingletonServiceConfiguratorFactory;
import org.wildfly.clustering.spi.ClusteringCacheRequirement;

/**
 * @author Paul Ferraro
 */
public class SingletonServiceConfiguratorFactoryServiceConfiguratorProvider extends CacheRequirementServiceConfiguratorProvider<SingletonServiceConfiguratorFactory> {

    protected SingletonServiceConfiguratorFactoryServiceConfiguratorProvider(CacheCapabilityServiceConfiguratorFactory<SingletonServiceConfiguratorFactory> factory) {
        super(ClusteringCacheRequirement.SINGLETON_SERVICE_CONFIGURATOR_FACTORY, factory);
    }

    @Override
    public Iterable<CapabilityServiceConfigurator> getServiceConfigurators(ServiceNameRegistry<ClusteringCacheRequirement> registry, String containerName, String cacheName) {
        Iterable<CapabilityServiceConfigurator> configurators = super.getServiceConfigurators(registry, containerName, cacheName);
        @SuppressWarnings("deprecation")
        ServiceName name = registry.getServiceName(ClusteringCacheRequirement.SINGLETON_SERVICE_BUILDER_FACTORY);
        if (name == null) return configurators;

        // Add configurator for deprecated capability
        CapabilityServiceConfigurator deprecatedConfigurator = new IdentityCapabilityServiceConfigurator<>(name, ClusteringCacheRequirement.SINGLETON_SERVICE_CONFIGURATOR_FACTORY, containerName, cacheName);
        return new CompositeIterable<>(configurators, Collections.singleton(deprecatedConfigurator));
    }
}

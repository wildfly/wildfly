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

import java.util.List;

import org.jboss.as.clustering.controller.IdentityCapabilityServiceConfigurator;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.ee.CompositeIterable;
import org.wildfly.clustering.server.CacheCapabilityServiceConfiguratorFactory;
import org.wildfly.clustering.server.CacheRequirementServiceConfiguratorProvider;
import org.wildfly.clustering.service.ServiceConfigurator;
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
    public Iterable<ServiceConfigurator> getServiceConfigurators(CapabilityServiceSupport support, String containerName, String cacheName) {
        Iterable<ServiceConfigurator> configurators = super.getServiceConfigurators(support, containerName, cacheName);
        @SuppressWarnings("deprecation")
        ServiceName name = ClusteringCacheRequirement.SINGLETON_SERVICE_BUILDER_FACTORY.getServiceName(support, containerName, cacheName);
        ServiceConfigurator deprecatedConfigurator = new IdentityCapabilityServiceConfigurator<>(name, ClusteringCacheRequirement.SINGLETON_SERVICE_CONFIGURATOR_FACTORY, containerName, cacheName).configure(support);
        return new CompositeIterable<>(configurators, List.of(deprecatedConfigurator));
    }
}

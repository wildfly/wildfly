/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.web.infinispan.session;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.function.Supplier;

import org.infinispan.configuration.cache.CacheMode;
import org.jboss.as.clustering.controller.CapabilityServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.infinispan.spi.InfinispanCacheRequirement;
import org.wildfly.clustering.infinispan.spi.service.CacheBuilder;
import org.wildfly.clustering.infinispan.spi.service.TemplateConfigurationBuilder;
import org.wildfly.clustering.service.ValueDependency;
import org.wildfly.clustering.spi.CacheBuilderProvider;
import org.wildfly.clustering.spi.ClusteringCacheRequirement;
import org.wildfly.clustering.spi.DistributedCacheBuilderProvider;
import org.wildfly.clustering.spi.ServiceNameRegistry;
import org.wildfly.clustering.web.session.RouteLocator;
import org.wildfly.clustering.web.session.RouteLocatorBuilderProvider;

/**
 * Provides a builder for a {@link RouteLocator} service.
 * @author Paul Ferraro
 */
@MetaInfServices(RouteLocatorBuilderProvider.class)
public class InfinispanRouteLocatorBuilderProvider implements RouteLocatorBuilderProvider {

    @Override
    public CapabilityServiceBuilder<RouteLocator> getRouteLocatorBuilder(String serverName, String deploymentName) {
        return new InfinispanRouteLocatorBuilder(serverName, deploymentName);
    }

    @Override
    public Collection<CapabilityServiceBuilder<?>> getRouteLocatorConfigurationBuilders(String serverName, Supplier<ValueDependency<String>> routeDependencyProvider) {
        String containerName = InfinispanSessionManagerFactoryBuilder.DEFAULT_CACHE_CONTAINER;

        List<CapabilityServiceBuilder<?>> builders = new LinkedList<>();

        builders.add(new RouteRegistryEntryProviderBuilder(serverName, routeDependencyProvider.get()));
        builders.add(new TemplateConfigurationBuilder(ServiceName.parse(InfinispanCacheRequirement.CONFIGURATION.resolve(containerName, serverName)), containerName, serverName, null, builder -> {
            CacheMode mode = builder.clustering().cacheMode();
            builder.clustering().cacheMode(mode.isClustered() ? CacheMode.REPL_SYNC : CacheMode.LOCAL);
            // don't use DefaultConsistentHashFactory for REPL caches (WFLY-9276)
            builder.clustering().hash().consistentHashFactory(null);
            builder.clustering().l1().disable();
            builder.persistence().clearStores();
        }));
        builders.add(new CacheBuilder<>(ServiceName.parse(InfinispanCacheRequirement.CACHE.resolve(containerName, serverName)), containerName, serverName));
        ServiceNameRegistry<ClusteringCacheRequirement> registry = requirement -> ServiceName.parse(requirement.resolve(containerName, serverName));
        for (CacheBuilderProvider provider : ServiceLoader.load(DistributedCacheBuilderProvider.class, DistributedCacheBuilderProvider.class.getClassLoader())) {
            builders.addAll(provider.getBuilders(registry, containerName, serverName));
        }

        return builders;
    }
}

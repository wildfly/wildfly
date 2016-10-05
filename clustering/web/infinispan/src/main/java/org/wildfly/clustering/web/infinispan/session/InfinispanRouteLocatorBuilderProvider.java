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
import java.util.function.Consumer;

import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.jboss.as.clustering.controller.CapabilityServiceBuilder;
import org.jboss.as.clustering.controller.SimpleCapabilityServiceBuilder;
import org.wildfly.clustering.infinispan.spi.service.CacheBuilder;
import org.wildfly.clustering.infinispan.spi.service.CacheServiceName;
import org.wildfly.clustering.infinispan.spi.service.TemplateConfigurationBuilder;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.spi.CacheGroupBuilderProvider;
import org.wildfly.clustering.spi.DistributedCacheGroupBuilderProvider;
import org.wildfly.clustering.web.session.RouteLocator;
import org.wildfly.clustering.web.session.RouteLocatorBuilderProvider;

/**
 * Builds a {@link RouteLocator} service.
 * @author Paul Ferraro
 */
public class InfinispanRouteLocatorBuilderProvider implements RouteLocatorBuilderProvider {

    @Override
    public Builder<RouteLocator> getRouteLocatorBuilder(String deploymentName) {
        return new InfinispanRouteLocatorBuilder(deploymentName);
    }

    @Override
    public Collection<CapabilityServiceBuilder<?>> getRouteLocatorConfigurationBuilders(String serverName) {
        String containerName = InfinispanSessionManagerFactoryBuilder.DEFAULT_CACHE_CONTAINER;

        List<CapabilityServiceBuilder<?>> builders = new LinkedList<>();

        builders.add(new RouteRegistryEntryProviderBuilder(serverName));

        if (!serverName.equals(CacheServiceName.DEFAULT_CACHE)) {
            Consumer<ConfigurationBuilder> consumer = builder -> {
                CacheMode mode = builder.clustering().cacheMode();
                builder.clustering().cacheMode(mode.isClustered() ? CacheMode.REPL_SYNC : CacheMode.LOCAL);
                builder.persistence().clearStores();
            };
            builders.add(new SimpleCapabilityServiceBuilder<>(new TemplateConfigurationBuilder(containerName, serverName, CacheServiceName.DEFAULT_CACHE, consumer)));
            builders.add(new SimpleCapabilityServiceBuilder<>(new CacheBuilder<>(containerName, serverName)));

            for (CacheGroupBuilderProvider provider : ServiceLoader.load(DistributedCacheGroupBuilderProvider.class, DistributedCacheGroupBuilderProvider.class.getClassLoader())) {
                provider.getBuilders(containerName, serverName).stream().map(builder -> new SimpleCapabilityServiceBuilder<>(builder)).forEach(builder -> builders.add(builder));
            }
        }

        return builders;
    }
}

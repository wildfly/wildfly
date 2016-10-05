/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

import org.infinispan.configuration.cache.CacheMode;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.wildfly.clustering.infinispan.spi.InfinispanCacheRequirement;
import org.wildfly.clustering.infinispan.spi.service.CacheBuilder;
import org.wildfly.clustering.infinispan.spi.service.TemplateConfigurationBuilder;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.spi.CacheGroupAliasBuilderProvider;
import org.wildfly.clustering.spi.CacheGroupBuilderProvider;

/**
 * Creates routing services.
 * @author Paul Ferraro
 */
public class RouteCacheGroupBuilderProvider implements CacheGroupBuilderProvider, CacheGroupAliasBuilderProvider {

    static final String CACHE_NAME = "routing";

    private final Class<? extends CacheGroupBuilderProvider> providerClass;

    RouteCacheGroupBuilderProvider(Class<? extends CacheGroupBuilderProvider> providerClass) {
        this.providerClass = providerClass;
    }

    @Override
    public Collection<Builder<?>> getBuilders(CapabilityServiceSupport support, String containerName, String cacheName) {
        List<Builder<?>> builders = new LinkedList<>();
        if (cacheName == null) {
            builders.add(new TemplateConfigurationBuilder(InfinispanCacheRequirement.CONFIGURATION.getServiceName(support, containerName, CACHE_NAME), containerName, CACHE_NAME, cacheName, builder -> {
                CacheMode mode = builder.clustering().cacheMode();
                builder.clustering().cacheMode(mode.isClustered() ? CacheMode.REPL_SYNC : CacheMode.LOCAL);
                builder.persistence().clearStores();
            }).configure(support));
            builders.add(new CacheBuilder<>(InfinispanCacheRequirement.CACHE.getServiceName(support, containerName, CACHE_NAME), containerName, CACHE_NAME).configure(support));
            for (CacheGroupBuilderProvider provider : ServiceLoader.load(this.providerClass, this.providerClass.getClassLoader())) {
                builders.addAll(provider.getBuilders(support, containerName, CACHE_NAME));
            }
        }
        return builders;
    }

    @Override
    public Collection<Builder<?>> getBuilders(CapabilityServiceSupport support, String containerName, String aliasCacheName, String targetCacheName) {
        return this.getBuilders(support, containerName, aliasCacheName);
    }
}

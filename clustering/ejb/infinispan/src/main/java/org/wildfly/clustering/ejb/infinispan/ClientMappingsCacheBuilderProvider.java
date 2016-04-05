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

package org.wildfly.clustering.ejb.infinispan;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.ServiceLoader;

import org.infinispan.configuration.cache.CacheMode;
import org.wildfly.clustering.ejb.BeanManagerFactoryBuilderConfiguration;
import org.wildfly.clustering.infinispan.spi.service.CacheBuilder;
import org.wildfly.clustering.infinispan.spi.service.TemplateConfigurationBuilder;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.service.SubGroupServiceNameFactory;
import org.wildfly.clustering.spi.CacheGroupAliasBuilderProvider;
import org.wildfly.clustering.spi.CacheGroupBuilderProvider;

/**
 * Creates routing services.
 * @author Paul Ferraro
 */
public class ClientMappingsCacheBuilderProvider implements CacheGroupBuilderProvider, CacheGroupAliasBuilderProvider {

    private final Class<? extends CacheGroupBuilderProvider> providerClass;

    ClientMappingsCacheBuilderProvider(Class<? extends CacheGroupBuilderProvider> providerClass) {
        this.providerClass = providerClass;
    }

    @Override
    public Collection<Builder<?>> getBuilders(String containerName, String cacheName) {
        List<Builder<?>> builders = new LinkedList<>();
        if (cacheName.equals(SubGroupServiceNameFactory.DEFAULT_SUB_GROUP)) {
            builders.add(new TemplateConfigurationBuilder(containerName, BeanManagerFactoryBuilderConfiguration.CLIENT_MAPPINGS_CACHE_NAME, cacheName, builder -> {
                CacheMode mode = builder.clustering().cacheMode();
                builder.clustering().cacheMode(mode.isClustered() ? CacheMode.REPL_SYNC : CacheMode.LOCAL);
                builder.persistence().clearStores();
            }));
            builders.add(new CacheBuilder<>(containerName, BeanManagerFactoryBuilderConfiguration.CLIENT_MAPPINGS_CACHE_NAME));
            for (CacheGroupBuilderProvider provider : ServiceLoader.load(this.providerClass, this.providerClass.getClassLoader())) {
                builders.addAll(provider.getBuilders(containerName, BeanManagerFactoryBuilderConfiguration.CLIENT_MAPPINGS_CACHE_NAME));
            }
        }
        return builders;
    }

    @Override
    public Collection<Builder<?>> getBuilders(String containerName, String aliasCacheName, String targetCacheName) {
        return this.getBuilders(containerName, aliasCacheName);
    }
}

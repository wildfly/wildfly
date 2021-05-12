/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.infinispan.client.near;

import org.infinispan.client.hotrod.configuration.NearCacheConfiguration;
import org.infinispan.client.hotrod.configuration.NearCacheMode;
import org.infinispan.client.hotrod.configuration.RemoteCacheConfiguration;
import org.infinispan.client.hotrod.event.impl.ClientListenerNotifier;
import org.infinispan.client.hotrod.near.NearCacheService;
import org.wildfly.clustering.infinispan.client.NearCacheFactory;

/**
 * A factory for creating near cache services based on a simple configuration.
 * @author Paul Ferraro
 */
public class SimpleNearCacheFactory<K, V> implements NearCacheFactory<K, V> {

    private final NearCacheConfiguration config;

    public SimpleNearCacheFactory(NearCacheMode mode) {
        this(new NearCacheConfiguration(mode, RemoteCacheConfiguration.NEAR_CACHE_MAX_ENTRIES.getDefaultValue().intValue(), RemoteCacheConfiguration.NEAR_CACHE_BLOOM_FILTER.getDefaultValue().booleanValue()));
    }

    public SimpleNearCacheFactory(NearCacheConfiguration config) {
        this.config = config;
    }

    @Override
    public NearCacheService<K, V> createService(ClientListenerNotifier notifier) {
        return NearCacheService.create(this.config, notifier);
    }

    @Override
    public NearCacheMode getMode() {
        return this.config.mode();
    }
}

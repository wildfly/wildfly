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

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.configuration.NearCacheConfiguration;
import org.infinispan.client.hotrod.event.impl.ClientListenerNotifier;
import org.infinispan.client.hotrod.near.NearCache;
import org.infinispan.client.hotrod.near.NearCacheService;
import org.wildfly.clustering.Registrar;
import org.wildfly.clustering.Registration;
import org.wildfly.clustering.infinispan.client.RegisteredRemoteCache;

/**
 * An empty near cache service.
 * @author Paul Ferraro
 */
public class EmptyNearCacheService<K, V> extends NearCacheService<K, V> implements Registrar<String>, Registration {

    public EmptyNearCacheService(ClientListenerNotifier notifier) {
        super(null, notifier);
    }

    @Override
    public void start(RemoteCache<K, V> cache) {
        super.start(new DeafRemoteCache<>(cache, this));
    }

    @Override
    public void stop(RemoteCache<K, V> cache) {
        super.stop(new DeafRemoteCache<>(cache, this));
    }

    @Override
    protected NearCache<K, V> createNearCache(NearCacheConfiguration config) {
        return new EmptyNearCache<>();
    }

    @Override
    public Registration register(String object) {
        return this;
    }

    @Override
    public void close() {
        // Do nothing
    }

    // Remote cache decorator with disabled listener registration
    private static class DeafRemoteCache<K, V> extends RegisteredRemoteCache<K, V> {

        DeafRemoteCache(RemoteCache<K, V> cache, Registrar<String> registrar) {
            super(cache.getRemoteCacheManager(), cache, registrar);
        }

        @Override
        public void addClientListener(Object listener) {
            // Disable listener registration
        }

        @Override
        public void addClientListener(Object listener, Object[] filterFactoryParams, Object[] converterFactoryParams) {
            // Disable listener registration
        }

        @Override
        public void removeClientListener(Object listener) {
            // Disable listener registration
        }
    }
}

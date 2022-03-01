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

package org.wildfly.clustering.infinispan.client;

import org.infinispan.client.hotrod.RemoteCacheManagerAdmin;
import org.infinispan.client.hotrod.jmx.RemoteCacheManagerMXBean;

/**
 * Extends Infinispan's {@link org.wildfly.clustering.infinispan.client.client.hotrod.RemoteCacheContainer} additionally exposing the name of the
 * remote cache container, an administration utility, and a mechanism for configuring near caching per remote cache.
 *
 * @author Radoslav Husar
 * @author Paul Ferraro
 */
public interface RemoteCacheContainer extends org.infinispan.client.hotrod.RemoteCacheContainer, RemoteCacheManagerMXBean {

    interface NearCacheRegistration extends AutoCloseable {
        @Override
        void close();
    }

    /**
     * Returns the name of this remote cache container.
     *
     * @return the remote cache container name
     */
    String getName();

    /**
     * Returns administration utility to administer (create, remove or reindex) caches.
     *
     * @return administration utility
     */
    RemoteCacheManagerAdmin administration();

    /**
     * Registers a factory for creating a near cache for a given cache.
     * The returned registration can be closed once the associated cache is created.
     * @param <K> the cache key
     * @param <V> the cache value
     * @param cacheName the name of a remote cache
     * @param factory a factory for creating a near cache
     * @return A near cache registration, which, when closed, unregisters the registered factory.
     */
    <K, V> NearCacheRegistration registerNearCacheFactory(String cacheName, NearCacheFactory<K, V> factory);
}

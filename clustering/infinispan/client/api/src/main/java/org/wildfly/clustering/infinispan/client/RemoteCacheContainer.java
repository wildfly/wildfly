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

import java.util.concurrent.CompletionStage;

import jakarta.transaction.TransactionManager;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManagerAdmin;
import org.infinispan.client.hotrod.configuration.TransactionMode;
import org.infinispan.client.hotrod.jmx.RemoteCacheManagerMXBean;

/**
 * Extends Infinispan's {@link org.wildfly.clustering.infinispan.client.client.hotrod.RemoteCacheContainer} additionally exposing the name of the
 * remote cache container, an administration utility, and a mechanism for configuring near caching per remote cache.
 *
 * @author Radoslav Husar
 * @author Paul Ferraro
 */
public interface RemoteCacheContainer extends org.infinispan.client.hotrod.RemoteCacheContainer, RemoteCacheManagerMXBean {

    /**
     * Returns the name of this remote cache container.
     *
     * @return the remote cache container name
     */
    String getName();

    @Deprecated
    @Override
    default <K, V> RemoteCache<K, V> getCache(boolean forceReturnValue) {
        return this.getCache();
    }

    @Deprecated
    @Override
    default <K, V> RemoteCache<K, V> getCache(String cacheName, boolean forceReturnValue) {
        return this.getCache(cacheName);
    }

    @Deprecated
    @Override
    default <K, V> RemoteCache<K, V> getCache(String cacheName, TransactionMode transactionMode) {
        return this.getCache(cacheName);
    }

    @Deprecated
    @Override
    default <K, V> RemoteCache<K, V> getCache(String cacheName, TransactionManager transactionManager) {
        return this.getCache(cacheName);
    }

    @Deprecated
    @Override
    default <K, V> RemoteCache<K, V> getCache(String cacheName, boolean forceReturnValue, TransactionMode transactionMode) {
        return this.getCache(cacheName);
    }

    @Deprecated
    @Override
    default <K, V> RemoteCache<K, V> getCache(String cacheName, boolean forceReturnValue, TransactionManager transactionManager) {
        return this.getCache(cacheName);
    }

    @Deprecated
    @Override
    default <K, V> RemoteCache<K, V> getCache(String cacheName, TransactionMode transactionMode, TransactionManager transactionManager) {
        return this.getCache(cacheName);
    }

    @Deprecated
    @Override
    default <K, V> RemoteCache<K, V> getCache(String cacheName, boolean forceReturnValue, TransactionMode transactionMode, TransactionManager transactionManager) {
        return this.getCache(cacheName);
    }

    /**
     * Returns administration utility to administer (create, remove or reindex) caches.
     *
     * @return administration utility
     */
    RemoteCacheManagerAdmin administration();

    CompletionStage<Boolean> isAvailable();
}

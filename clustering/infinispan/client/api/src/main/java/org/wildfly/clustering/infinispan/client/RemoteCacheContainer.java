/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.infinispan.client;

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
}

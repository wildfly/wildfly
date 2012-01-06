/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.ejb3.cache.impl.factory;

import java.io.Serializable;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.as.ejb3.cache.Cache;
import org.jboss.as.ejb3.cache.CacheFactory;
import org.jboss.as.ejb3.cache.Cacheable;
import org.jboss.as.ejb3.cache.PassivationManager;
import org.jboss.as.ejb3.cache.StatefulObjectFactory;
import org.jboss.as.ejb3.cache.impl.GroupAwareCache;
import org.jboss.as.ejb3.cache.impl.backing.GroupAwareBackingCacheImpl;
import org.jboss.as.ejb3.cache.impl.backing.PassivatingBackingCacheImpl;
import org.jboss.as.ejb3.cache.impl.backing.SerializationGroupContainer;
import org.jboss.as.ejb3.cache.impl.backing.SerializationGroupMemberContainer;
import org.jboss.as.ejb3.cache.spi.BackingCacheEntryStore;
import org.jboss.as.ejb3.cache.spi.BackingCacheEntryStoreSource;
import org.jboss.as.ejb3.cache.spi.BackingCacheLifecycleListener;
import org.jboss.as.ejb3.cache.spi.GroupAwareBackingCache;
import org.jboss.as.ejb3.cache.spi.PassivatingBackingCache;
import org.jboss.as.ejb3.cache.spi.SerializationGroup;
import org.jboss.as.ejb3.cache.spi.SerializationGroupMember;
import org.jboss.as.ejb3.component.stateful.StatefulTimeoutInfo;

/**
 * {@link CacheFactory} implementation that can return a group-aware cache. How the cache functions depends on the
 * behavior of the {@link BackingCacheEntryStore} implementations returned by the injected {@link BackingCacheEntryStoreSource}.
 *
 * @author Brian Stansberry
 * @author Paul Ferraro
 */
public class GroupAwareCacheFactory<K extends Serializable, V extends Cacheable<K>> implements CacheFactory<K, V>, BackingCacheLifecycleListener {

    private final AtomicReference<PassivatingBackingCache<UUID, Cacheable<UUID>, SerializationGroup<K, V, UUID>>> groupCacheRef = new AtomicReference<PassivatingBackingCache<UUID, Cacheable<UUID>, SerializationGroup<K, V, UUID>>>();
    private final AtomicInteger memberCounter = new AtomicInteger();
    private final BackingCacheEntryStoreSource<K, V, UUID> storeSource;

    public GroupAwareCacheFactory(BackingCacheEntryStoreSource<K, V, UUID> storeSource) {
        this.storeSource = storeSource;
    }

    // --------------------------------------------------- StatefulCacheFactory

    @Override
    public Cache<K, V> createCache(String beanName, StatefulObjectFactory<V> factory, PassivationManager<K, V> passivationManager, StatefulTimeoutInfo timeout) {

        // Create/find the cache for SerializationGroup that the container
        // may be associated with
        PassivatingBackingCache<UUID, Cacheable<UUID>, SerializationGroup<K, V, UUID>> groupCache = this.groupCacheRef.get();
        if (groupCache == null) {
            groupCache = this.createGroupCache(passivationManager, timeout);
            if (!groupCacheRef.compareAndSet(null, groupCache)) {
                groupCache = this.groupCacheRef.get();
            }
        }

        SerializationGroupMemberContainer<K, V, UUID> container = new SerializationGroupMemberContainer<K, V, UUID>(passivationManager, groupCache, this.storeSource);

        // Create the store for SerializationGroupMembers from the container
        BackingCacheEntryStore<K, V, SerializationGroupMember<K, V, UUID>> store = storeSource.createIntegratedObjectStore(beanName, container, timeout);
        container.setBackingCacheEntryStore(store);

        // Set up the backing cache with the store and group cache
        GroupAwareBackingCache<K, V, UUID, SerializationGroupMember<K, V, UUID>> backingCache = new GroupAwareBackingCacheImpl<K, V, UUID>(factory, container, groupCache, Executors.newScheduledThreadPool(1, Executors.defaultThreadFactory()));

        // Listen for backing cache lifecycle changes so we know when to start/stop groupCache
        backingCache.addLifecycleListener(this);

        // Finally, the front-end cache
        return new GroupAwareCache<K, V, UUID, SerializationGroupMember<K, V, UUID>>(this.storeSource, backingCache, true);
    }

    private PassivatingBackingCache<UUID, Cacheable<UUID>, SerializationGroup<K, V, UUID>> createGroupCache(PassivationManager<K, V> passivationManager, StatefulTimeoutInfo timeout) {
        SerializationGroupContainer<K, V> container = new SerializationGroupContainer<K, V>(passivationManager);

        BackingCacheEntryStore<UUID, Cacheable<UUID>, SerializationGroup<K, V, UUID>> store = storeSource.createGroupIntegratedObjectStore(container, timeout);

        PassivatingBackingCache<UUID, Cacheable<UUID>, SerializationGroup<K, V, UUID>> groupCache = new PassivatingBackingCacheImpl<UUID, Cacheable<UUID>, SerializationGroup<K, V, UUID>>(container, container, container, store);

        container.setGroupCache(groupCache);

        return groupCache;
    }

    @Override
    public void lifecycleChange(LifecycleState newState) {
        switch (newState) {
            case STARTING: {
                if (this.memberCounter.incrementAndGet() == 1) {
                    PassivatingBackingCache<UUID, Cacheable<UUID>, SerializationGroup<K, V, UUID>> groupCache = this.groupCacheRef.get();
                    synchronized (groupCache) {
                        groupCache.start();
                    }
                }
                break;
            }
            case STOPPED: {
                if (this.memberCounter.decrementAndGet() == 0) {
                    PassivatingBackingCache<UUID, Cacheable<UUID>, SerializationGroup<K, V, UUID>> groupCache = this.groupCacheRef.get();
                    synchronized (groupCache) {
                        groupCache.stop();
                    }
                }
                break;
            }
        }
    }
}

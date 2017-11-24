/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.infinispan.spi;

import java.lang.reflect.Field;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Collections;
import java.util.function.Predicate;

import org.infinispan.container.DefaultDataContainer;
import org.infinispan.container.InternalEntryFactory;
import org.infinispan.container.entries.InternalCacheEntry;
import org.infinispan.eviction.ActivationManager;
import org.infinispan.eviction.EvictionManager;
import org.infinispan.eviction.PassivationManager;
import org.infinispan.expiration.ExpirationManager;
import org.infinispan.notifications.cachelistener.CacheNotifier;
import org.infinispan.persistence.manager.PersistenceManager;
import org.infinispan.util.TimeService;
import org.infinispan.util.concurrent.WithinThreadExecutor;
import org.wildfly.security.manager.WildFlySecurityManager;

import com.github.benmanes.caffeine.cache.CacheWriter;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.RemovalListener;
import com.github.benmanes.caffeine.cache.Weigher;

/**
 * Custom {@link DataContainer} considers only specific cache entries for eviction.
 * @author Paul Ferraro
 */
public class EvictableDataContainer<K, V> extends DefaultDataContainer<K, V> implements RemovalListener<K, InternalCacheEntry<K, V>>, CacheWriter<K, InternalCacheEntry<K, V>> {

    private EvictionManager<K, V> evictionManager;
    private PassivationManager passivator;
    private ActivationManager activator;

    public EvictableDataContainer(long size, Predicate<K> evictable) {
        super(0);

        Weigher<K, InternalCacheEntry<K, V>> weigher = (key, value) -> evictable.test(key) ? 1 : 0;
        com.github.benmanes.caffeine.cache.Cache<K, InternalCacheEntry<K, V>> evictionCache = Caffeine.newBuilder()
                .weigher(weigher)
                .maximumWeight(size)
                .executor(new WithinThreadExecutor())
                .removalListener(this)
                .writer(this)
                .build();

        // Workaround for ISPN-8319
        this.setField("evictionCache", evictionCache);
        this.setField("entries", evictionCache.asMap());
    }

    private void setField(String name, Object value) {
        PrivilegedExceptionAction<Void> action = () -> {
            Field field = DefaultDataContainer.class.getDeclaredField(name);
            field.setAccessible(true);
            try {
                field.set(this, value);
                return null;
            } finally {
                field.setAccessible(false);
            }
        };
        try {
            WildFlySecurityManager.doUnchecked(action);
        } catch (PrivilegedActionException e) {
            throw new IllegalStateException(e.getCause());
        }
    }

    @Override
    public void initialize(EvictionManager evictionManager, PassivationManager passivator, InternalEntryFactory entryFactory,
            ActivationManager activator, PersistenceManager persistenceManager, TimeService timeService,
            CacheNotifier cacheNotifier, ExpirationManager<K, V> expirationManager) {
        super.initialize(evictionManager, passivator, entryFactory, activator, persistenceManager, timeService, cacheNotifier, expirationManager);
        this.evictionManager = evictionManager;
        this.passivator = passivator;
        this.activator = activator;
    }

    @Override
    public void write(K key, InternalCacheEntry<K, V> value) {
    }

    @Override
    public void delete(K key, InternalCacheEntry<K, V> value, RemovalCause cause) {
        if (cause == RemovalCause.SIZE) {
            this.passivator.passivate(value);
        }
    }

    @Override
    public void onRemoval(K key, InternalCacheEntry<K, V> value, RemovalCause cause) {
        switch (cause) {
            case SIZE: {
                this.evictionManager.onEntryEviction(Collections.singletonMap(key, value));
                break;
            }
            case REPLACED: {
                this.activator.onUpdate(key, true);
                break;
            }
            case COLLECTED:
            case EXPIRED:
            case EXPLICIT:
        }
    }
}

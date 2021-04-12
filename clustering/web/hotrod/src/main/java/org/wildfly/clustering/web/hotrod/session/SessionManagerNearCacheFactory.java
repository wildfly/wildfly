/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.web.hotrod.session;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.infinispan.client.hotrod.MetadataValue;
import org.infinispan.client.hotrod.configuration.NearCacheMode;
import org.infinispan.client.hotrod.event.impl.ClientListenerNotifier;
import org.infinispan.client.hotrod.near.NearCacheService;
import org.wildfly.clustering.infinispan.client.NearCacheFactory;
import org.wildfly.clustering.infinispan.client.near.CaffeineNearCacheService;
import org.wildfly.clustering.infinispan.client.near.SimpleKeyWeigher;
import org.wildfly.clustering.web.hotrod.session.coarse.SessionAttributesKey;
import org.wildfly.clustering.web.hotrod.session.fine.SessionAttributeKey;
import org.wildfly.clustering.web.hotrod.session.fine.SessionAttributeNamesKey;
import org.wildfly.clustering.web.session.SessionAttributePersistenceStrategy;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.RemovalListener;

/**
 * A near-cache factory based on max-active-sessions.
 * @author Paul Ferraro
 */
public class SessionManagerNearCacheFactory<K, V> implements NearCacheFactory<K, V>, Supplier<Cache<K, MetadataValue<V>>>, RemovalListener<Object, Object> {

    private final Integer maxActiveSessions;
    private final SessionAttributePersistenceStrategy strategy;
    private final AtomicReference<Cache<K, MetadataValue<V>>> cache = new AtomicReference<>();

    public SessionManagerNearCacheFactory(Integer maxActiveSessions, SessionAttributePersistenceStrategy strategy) {
        this.maxActiveSessions = maxActiveSessions;
        this.strategy = strategy;
    }

    @Override
    public NearCacheService<K, V> createService(ClientListenerNotifier notifier) {
        return new CaffeineNearCacheService<>(this, notifier);
    }

    @Override
    public NearCacheMode getMode() {
        return (this.maxActiveSessions == null) || (this.maxActiveSessions.intValue() == 0) ? NearCacheMode.DISABLED : NearCacheMode.INVALIDATED;
    }

    @Override
    public Cache<K, MetadataValue<V>> get() {
        Caffeine<Object, Object> builder = Caffeine.newBuilder();
        if (this.maxActiveSessions != null) {
            builder.executor(Runnable::run)
                    .maximumWeight(this.maxActiveSessions.longValue())
                    .weigher(new SimpleKeyWeigher(SessionCreationMetaDataKey.class::isInstance))
                    .removalListener(this);
        }
        Cache<K, MetadataValue<V>> cache = builder.build();
        // Set reference for use by removal listener
        this.cache.set(cache);
        return cache;
    }

    @Override
    public void onRemoval(Object key, Object value, RemovalCause cause) {
        // Cascade invalidation to dependent entries
        if ((cause == RemovalCause.SIZE) && (key instanceof SessionCreationMetaDataKey)) {
            String id = ((SessionCreationMetaDataKey) key).getId();
            Cache<K, MetadataValue<V>> cache = this.cache.get();
            List<Object> keys = new LinkedList<>();
            keys.add(new SessionAccessMetaDataKey(id));
            switch (this.strategy) {
                case COARSE: {
                    keys.add(new SessionAttributesKey(id));
                    break;
                }
                case FINE: {
                    SessionAttributeNamesKey namesKey = new SessionAttributeNamesKey(id);
                    keys.add(namesKey);
                    MetadataValue<V> namesValue = cache.getIfPresent(namesKey);
                    if (namesValue != null) {
                        @SuppressWarnings("unchecked")
                        Map<String, UUID> names = (Map<String, UUID>) namesValue.getValue();
                        for (UUID attributeId : names.values()) {
                            keys.add(new SessionAttributeKey(id, attributeId));
                        }
                    }
                    break;
                }
            }
            cache.invalidateAll(keys);
        }
    }
}

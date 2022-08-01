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
import java.util.function.BiConsumer;

import org.infinispan.client.hotrod.MetadataValue;
import org.infinispan.client.hotrod.configuration.NearCacheConfiguration;
import org.infinispan.client.hotrod.near.NearCache;
import org.infinispan.client.hotrod.near.NearCacheFactory;
import org.wildfly.clustering.infinispan.client.near.CaffeineNearCache;
import org.wildfly.clustering.infinispan.client.near.EvictionListener;
import org.wildfly.clustering.infinispan.client.near.SimpleKeyWeigher;
import org.wildfly.clustering.web.hotrod.session.coarse.SessionAttributesKey;
import org.wildfly.clustering.web.hotrod.session.fine.SessionAttributeKey;
import org.wildfly.clustering.web.hotrod.session.fine.SessionAttributeNamesKey;
import org.wildfly.clustering.web.session.SessionAttributePersistenceStrategy;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * A near-cache factory based on max-active-sessions.
 * @author Paul Ferraro
 */
public class SessionManagerNearCacheFactory implements NearCacheFactory {

    private final Integer maxActiveSessions;
    private final SessionAttributePersistenceStrategy strategy;

    public SessionManagerNearCacheFactory(Integer maxActiveSessions, SessionAttributePersistenceStrategy strategy) {
        this.maxActiveSessions = maxActiveSessions;
        this.strategy = strategy;
    }

    @Override
    public <K, V> NearCache<K, V> createNearCache(NearCacheConfiguration config, BiConsumer<K, MetadataValue<V>> removedConsumer) {
        EvictionListener<K, V> listener = (this.maxActiveSessions != null) ? new EvictionListener<>(removedConsumer, new InvalidationListener(this.strategy)) : null;
        Caffeine<Object, Object> builder = Caffeine.newBuilder();
        if (listener != null) {
            builder.executor(Runnable::run)
                    .maximumWeight(this.maxActiveSessions.longValue())
                    .weigher(new SimpleKeyWeigher(SessionCreationMetaDataKey.class::isInstance))
                    .removalListener(listener);
        }
        Cache<K, MetadataValue<V>> cache = builder.build();
        if (listener != null) {
            listener.accept(cache);
        }
        return new CaffeineNearCache<>(cache);
    }

    private static class InvalidationListener implements BiConsumer<Cache<Object, MetadataValue<Object>>, Map.Entry<Object, Object>> {
        private final SessionAttributePersistenceStrategy strategy;

        InvalidationListener(SessionAttributePersistenceStrategy strategy) {
            this.strategy = strategy;
        }

        @Override
        public void accept(Cache<Object, MetadataValue<Object>> cache, Map.Entry<Object, Object> entry) {
            Object key = entry.getKey();
            if (key instanceof SessionCreationMetaDataKey) {
                String id = ((SessionCreationMetaDataKey) key).getId();
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
                        MetadataValue<Object> namesValue = cache.getIfPresent(namesKey);
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
}

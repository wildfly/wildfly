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

package org.wildfly.clustering.ejb.hotrod.bean;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import org.infinispan.client.hotrod.MetadataValue;
import org.infinispan.client.hotrod.configuration.NearCacheConfiguration;
import org.infinispan.client.hotrod.near.NearCache;
import org.infinispan.client.hotrod.near.NearCacheFactory;
import org.jboss.logging.Logger;
import org.wildfly.clustering.ejb.bean.BeanInstance;
import org.wildfly.clustering.ejb.cache.bean.BeanGroupKey;
import org.wildfly.clustering.infinispan.client.near.CaffeineNearCache;
import org.wildfly.clustering.infinispan.client.near.EvictionListener;
import org.wildfly.clustering.infinispan.client.near.SimpleKeyWeigher;
import org.wildfly.clustering.marshalling.spi.MarshalledValue;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * A near-cache factory based on max-active-sessions.
 * @author Paul Ferraro
 * @param <K> the bean identifier type
 * @param <V> the bean instance type
 */
public class BeanManagerNearCacheFactory<K, V extends BeanInstance<K>, C> implements NearCacheFactory {
    private static final Logger LOGGER = Logger.getLogger(BeanManagerNearCacheFactory.class);

    private final Integer maxActiveSessions;
    private final Supplier<C> context;

    public BeanManagerNearCacheFactory(Integer maxActiveSessions, Supplier<C> context) {
        this.maxActiveSessions = maxActiveSessions;
        this.context = context;
    }

    @Override
    public <KK, VV> NearCache<KK, VV> createNearCache(NearCacheConfiguration config, BiConsumer<KK, MetadataValue<VV>> removedConsumer) {
        EvictionListener<KK, VV> listener = (this.maxActiveSessions != null) ? new EvictionListener<>(removedConsumer, new InvalidationListener<>(this.context)) : null;
        Caffeine<Object, Object> builder = Caffeine.newBuilder();
        if (listener != null) {
            builder.executor(Runnable::run)
                    .maximumWeight(this.maxActiveSessions.longValue())
                    .weigher(new SimpleKeyWeigher(BeanGroupKey.class::isInstance))
                    .removalListener(listener);
        }
        Cache<KK, MetadataValue<VV>> cache = builder.build();
        if (listener != null) {
            listener.accept(cache);
        }
        return new CaffeineNearCache<>(cache);
    }

    private static class InvalidationListener<K, V extends BeanInstance<K>, C> implements BiConsumer<Cache<Object, MetadataValue<Object>>, Map.Entry<Object, Object>> {
        private final Supplier<C> context;

        InvalidationListener(Supplier<C> context) {
            this.context = context;
        }

        @Override
        public void accept(Cache<Object, MetadataValue<Object>> cache, Map.Entry<Object, Object> entry) {
            Object key = entry.getKey();
            if (key instanceof BeanGroupKey) {
                @SuppressWarnings("unchecked")
                MarshalledValue<Map<K, V>, C> value = (MarshalledValue<Map<K, V>, C>) entry.getValue();
                try {
                    // Passivation listeners will already have been triggered
                    Map<K, V> instances = value.get(this.context.get());
                    List<Object> keys = new ArrayList<>(instances.size() * 2);
                    for (V instance : instances.values()) {
                        K id = instance.getId();
                        keys.add(new HotRodBeanCreationMetaDataKey<>(id));
                        keys.add(new HotRodBeanAccessMetaDataKey<>(id));
                    }
                    cache.invalidateAll(keys);
                } catch (IOException e) {
                    LOGGER.warn(e.getLocalizedMessage(), e);
                }
            }
        }
    }
}

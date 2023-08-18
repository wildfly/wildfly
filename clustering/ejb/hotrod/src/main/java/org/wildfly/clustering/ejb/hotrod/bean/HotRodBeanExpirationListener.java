/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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

import java.util.Map;
import java.util.function.BiConsumer;

import org.infinispan.client.hotrod.Flag;
import org.infinispan.client.hotrod.RemoteCache;
import org.wildfly.clustering.ejb.bean.Bean;
import org.wildfly.clustering.ejb.bean.BeanExpirationConfiguration;
import org.wildfly.clustering.ejb.bean.BeanInstance;
import org.wildfly.clustering.ejb.cache.bean.BeanAccessMetaData;
import org.wildfly.clustering.ejb.cache.bean.BeanAccessMetaDataKey;
import org.wildfly.clustering.ejb.cache.bean.BeanCreationMetaData;
import org.wildfly.clustering.ejb.cache.bean.BeanCreationMetaDataKey;
import org.wildfly.clustering.ejb.cache.bean.BeanFactory;
import org.wildfly.clustering.infinispan.client.listener.ClientCacheEntryExpiredEventListener;
import org.wildfly.clustering.infinispan.client.listener.ListenerRegistrar;
import org.wildfly.clustering.infinispan.client.listener.ListenerRegistration;

/**
 * Client listener for remote expiration events.
 * @author Paul Ferraro
 * @param <K> the bean identifier type
 * @param <V> the bean instance type
 */
public class HotRodBeanExpirationListener<K, V extends BeanInstance<K>> implements BiConsumer<BeanAccessMetaDataKey<K>, BeanAccessMetaData>, ListenerRegistrar {

    private final BeanExpirationConfiguration<K, V> expiration;
    private final BeanFactory<K, V, Map.Entry<BeanCreationMetaData<K>, BeanAccessMetaData>> factory;
    private final RemoteCache<BeanCreationMetaDataKey<K>, BeanCreationMetaData<K>> creationMetaDataCache;
    private final RemoteCache<BeanAccessMetaDataKey<K>, BeanAccessMetaData> accessMetaDataCache;

    public HotRodBeanExpirationListener(HotRodBeanManagerConfiguration<K, V, Map.Entry<BeanCreationMetaData<K>, BeanAccessMetaData>> configuration) {
        this.expiration = configuration.getExpiration();
        this.factory = configuration.getBeanFactory();
        this.creationMetaDataCache = configuration.getCache();
        this.accessMetaDataCache = configuration.getCache();
    }

    @Override
    public ListenerRegistration register() {
        return new ClientCacheEntryExpiredEventListener<>(this.accessMetaDataCache, this).register();
    }

    @Override
    public void accept(BeanAccessMetaDataKey<K> accessMetaDataKey, BeanAccessMetaData accessMetaData) {
        K id = accessMetaDataKey.getId();
        BeanCreationMetaData<K> creationMetaData = this.creationMetaDataCache.withFlags(Flag.FORCE_RETURN_VALUE).remove(new HotRodBeanCreationMetaDataKey<>(id));
        if (creationMetaData != null) {
            try (Bean<K, V> bean = this.factory.createBean(id, Map.entry(creationMetaData, accessMetaData))) {
                bean.remove(this.expiration.getExpirationListener());
            }
        }
    }
}

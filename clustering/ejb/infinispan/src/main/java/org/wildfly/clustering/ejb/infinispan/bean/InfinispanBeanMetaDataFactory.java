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

package org.wildfly.clustering.ejb.infinispan.bean;

import java.util.Map;

import org.infinispan.Cache;
import org.wildfly.clustering.ee.Key;
import org.wildfly.clustering.ee.Mutator;
import org.wildfly.clustering.ee.MutatorFactory;
import org.wildfly.clustering.ee.infinispan.InfinispanMutatorFactory;
import org.wildfly.clustering.ejb.bean.BeanExpiration;
import org.wildfly.clustering.ejb.bean.BeanInstance;
import org.wildfly.clustering.ejb.bean.BeanMetaData;
import org.wildfly.clustering.ejb.bean.ImmutableBeanMetaData;
import org.wildfly.clustering.ejb.cache.bean.BeanAccessMetaData;
import org.wildfly.clustering.ejb.cache.bean.BeanAccessMetaDataKey;
import org.wildfly.clustering.ejb.cache.bean.BeanCreationMetaData;
import org.wildfly.clustering.ejb.cache.bean.BeanCreationMetaDataKey;
import org.wildfly.clustering.ejb.cache.bean.BeanMetaDataFactory;
import org.wildfly.clustering.ejb.cache.bean.CompositeBeanMetaData;
import org.wildfly.clustering.ejb.cache.bean.CompositeImmutableBeanMetaData;
import org.wildfly.clustering.ejb.cache.bean.ImmortalBeanAccessMetaData;
import org.wildfly.clustering.ejb.cache.bean.MutableBeanAccessMetaData;
import org.wildfly.clustering.ejb.cache.bean.SimpleBeanAccessMetaData;
import org.wildfly.clustering.ejb.cache.bean.SimpleBeanCreationMetaData;

/**
 * A {@link BeanMetaDataFactory} whose metadata entries are stored in an embedded Infinispan cache.
 * @author Paul Ferraro
 * @param <K> the bean identifier type
 */
public class InfinispanBeanMetaDataFactory<K> implements BeanMetaDataFactory<K, Map.Entry<BeanCreationMetaData<K>, BeanAccessMetaData>> {

    private final Cache<Key<K>, Object> writeOnlyCache;
    private final Cache<BeanCreationMetaDataKey<K>, BeanCreationMetaData<K>> creationMetaDataReadForUpdateCache;
    private final Cache<BeanCreationMetaDataKey<K>, BeanCreationMetaData<K>> creationMetaDataTryReadForUpdateCache;
    private final Cache<BeanAccessMetaDataKey<K>, BeanAccessMetaData> accessMetaDataCache;
    private final MutatorFactory<BeanAccessMetaDataKey<K>, BeanAccessMetaData> mutatorFactory;
    private final BeanExpiration expiration;
    private final String beanName;

    public InfinispanBeanMetaDataFactory(InfinispanBeanMetaDataFactoryConfiguration configuration) {
        this.writeOnlyCache = configuration.getWriteOnlyCache();
        this.creationMetaDataReadForUpdateCache = configuration.getReadForUpdateCache();
        this.creationMetaDataTryReadForUpdateCache = configuration.getTryReadForUpdateCache();
        this.expiration = configuration.getExpiration();
        boolean scheduledExpiration = (this.expiration != null) && !this.expiration.getTimeout().isZero();
        this.accessMetaDataCache = scheduledExpiration ? configuration.getCache() : null;
        this.mutatorFactory = (this.accessMetaDataCache != null) ? new InfinispanMutatorFactory<>(this.accessMetaDataCache) : null;
        this.beanName = configuration.getBeanName();
    }

    @Override
    public Map.Entry<BeanCreationMetaData<K>, BeanAccessMetaData> createValue(BeanInstance<K> instance, K groupId) {
        K id = instance.getId();
        BeanCreationMetaDataKey<K> creationMetaDataKey = new InfinispanBeanCreationMetaDataKey<>(id);
        BeanCreationMetaData<K> creationMetaData = new SimpleBeanCreationMetaData<>(this.beanName, groupId);
        BeanAccessMetaData accessMetaData = (this.accessMetaDataCache != null) ? new SimpleBeanAccessMetaData() : ImmortalBeanAccessMetaData.INSTANCE;
        if (this.accessMetaDataCache != null) {
            BeanAccessMetaDataKey<K> accessMetaDataKey = new InfinispanBeanAccessMetaDataKey<>(id);
            this.writeOnlyCache.putAll(Map.of(creationMetaDataKey, creationMetaData, accessMetaDataKey, accessMetaData));
        } else {
            this.writeOnlyCache.put(creationMetaDataKey, creationMetaData);
        }
        return Map.entry(creationMetaData, accessMetaData);
    }

    @Override
    public Map.Entry<BeanCreationMetaData<K>, BeanAccessMetaData> findValue(K id) {
        return this.getValue(this.creationMetaDataReadForUpdateCache, id);
    }

    @Override
    public Map.Entry<BeanCreationMetaData<K>, BeanAccessMetaData> tryValue(K id) {
        return this.getValue(this.creationMetaDataTryReadForUpdateCache, id);
    }

    private Map.Entry<BeanCreationMetaData<K>, BeanAccessMetaData> getValue(Cache<BeanCreationMetaDataKey<K>, BeanCreationMetaData<K>> creationMetaDataCache, K id) {
        BeanCreationMetaData<K> creationMetaData = creationMetaDataCache.get(new InfinispanBeanCreationMetaDataKey<>(id));
        if (creationMetaData == null) return null;
        BeanAccessMetaData accessMetaData = (this.accessMetaDataCache != null) ? this.accessMetaDataCache.get(new InfinispanBeanAccessMetaDataKey<>(id)) : null;
        return Map.entry(creationMetaData, (accessMetaData != null) ? accessMetaData : ImmortalBeanAccessMetaData.INSTANCE);
    }

    @Override
    public boolean remove(K id) {
        this.writeOnlyCache.remove(new InfinispanBeanCreationMetaDataKey<>(id));
        if (this.accessMetaDataCache != null) {
            this.writeOnlyCache.remove(new InfinispanBeanAccessMetaDataKey<>(id));
        }
        return true;
    }

    @Override
    public ImmutableBeanMetaData<K> createImmutableBeanMetaData(K id, Map.Entry<BeanCreationMetaData<K>, BeanAccessMetaData> entry) {
        BeanCreationMetaData<K> creationMetaData = entry.getKey();
        BeanAccessMetaData accessMetaData = entry.getValue();
        return new CompositeImmutableBeanMetaData<>(creationMetaData, accessMetaData, this.expiration);
    }

    @Override
    public BeanMetaData<K> createBeanMetaData(K id, Map.Entry<BeanCreationMetaData<K>, BeanAccessMetaData> entry) {
        BeanCreationMetaData<K> creationMetaData = entry.getKey();
        BeanAccessMetaData accessMetaData = entry.getValue();
        Mutator mutator = (this.mutatorFactory != null) ? this.mutatorFactory.createMutator(new InfinispanBeanAccessMetaDataKey<>(id), accessMetaData) : Mutator.PASSIVE;
        return new CompositeBeanMetaData<>(creationMetaData, (mutator != Mutator.PASSIVE) ? new MutableBeanAccessMetaData(accessMetaData, mutator) : accessMetaData, this.expiration);
    }
}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.bean;

import java.time.Instant;

import org.infinispan.Cache;
import org.wildfly.clustering.ee.Mutator;
import org.wildfly.clustering.ee.MutatorFactory;
import org.wildfly.clustering.ee.cache.offset.OffsetValue;
import org.wildfly.clustering.ee.infinispan.CacheComputeMutatorFactory;
import org.wildfly.clustering.ejb.bean.BeanExpiration;
import org.wildfly.clustering.ejb.bean.BeanInstance;
import org.wildfly.clustering.ejb.bean.BeanMetaData;
import org.wildfly.clustering.ejb.bean.ImmutableBeanMetaData;
import org.wildfly.clustering.ejb.cache.bean.RemappableBeanMetaDataEntry;
import org.wildfly.clustering.ejb.cache.bean.BeanMetaDataEntryFunction;
import org.wildfly.clustering.ejb.cache.bean.BeanMetaDataFactory;
import org.wildfly.clustering.ejb.cache.bean.BeanMetaDataKey;
import org.wildfly.clustering.ejb.cache.bean.DefaultBeanMetaData;
import org.wildfly.clustering.ejb.cache.bean.DefaultBeanMetaDataEntry;
import org.wildfly.clustering.ejb.cache.bean.DefaultImmutableBeanMetaData;
import org.wildfly.clustering.ejb.cache.bean.MutableBeanMetaDataEntry;

/**
 * A {@link BeanMetaDataFactory} whose metadata entries are stored in an embedded Infinispan cache.
 * @author Paul Ferraro
 * @param <K> the bean identifier type
 */
public class InfinispanBeanMetaDataFactory<K> implements BeanMetaDataFactory<K, RemappableBeanMetaDataEntry<K>> {

    private final Cache<BeanMetaDataKey<K>, RemappableBeanMetaDataEntry<K>> writeOnlyCache;
    private final Cache<BeanMetaDataKey<K>, RemappableBeanMetaDataEntry<K>> readForUpdateCache;
    private final Cache<BeanMetaDataKey<K>, RemappableBeanMetaDataEntry<K>> tryReadForUpdateCache;
    private final MutatorFactory<BeanMetaDataKey<K>, OffsetValue<Instant>> mutatorFactory;
    private final BeanExpiration expiration;
    private final String beanName;

    public InfinispanBeanMetaDataFactory(InfinispanBeanMetaDataFactoryConfiguration configuration) {
        this.writeOnlyCache = configuration.getWriteOnlyCache();
        this.readForUpdateCache = configuration.getReadForUpdateCache();
        this.tryReadForUpdateCache = configuration.getTryReadForUpdateCache();
        this.expiration = configuration.getExpiration();
        this.mutatorFactory = (this.expiration != null) && !this.expiration.getTimeout().isZero() ? new CacheComputeMutatorFactory<>(this.writeOnlyCache, BeanMetaDataEntryFunction::new) : null;
        this.beanName = configuration.getBeanName();
    }

    @Override
    public RemappableBeanMetaDataEntry<K> createValue(BeanInstance<K> instance, K groupId) {
        RemappableBeanMetaDataEntry<K> entry = new DefaultBeanMetaDataEntry<>(this.beanName, groupId);
        this.writeOnlyCache.put(new InfinispanBeanMetaDataKey<>(instance.getId()), entry);
        return entry;
    }

    @Override
    public RemappableBeanMetaDataEntry<K> findValue(K id) {
        return this.getValue(this.readForUpdateCache, id);
    }

    @Override
    public RemappableBeanMetaDataEntry<K> tryValue(K id) {
        return this.getValue(this.tryReadForUpdateCache, id);
    }

    private RemappableBeanMetaDataEntry<K> getValue(Cache<BeanMetaDataKey<K>, RemappableBeanMetaDataEntry<K>> cache, K id) {
        return cache.get(new InfinispanBeanMetaDataKey<>(id));
    }

    @Override
    public boolean remove(K id) {
        this.writeOnlyCache.remove(new InfinispanBeanMetaDataKey<>(id));
        return true;
    }

    @Override
    public ImmutableBeanMetaData<K> createImmutableBeanMetaData(K id, RemappableBeanMetaDataEntry<K> entry) {
        return new DefaultImmutableBeanMetaData<>(entry, this.expiration);
    }

    @Override
    public BeanMetaData<K> createBeanMetaData(K id, RemappableBeanMetaDataEntry<K> entry) {
        OffsetValue<Instant> lastAccess = (this.mutatorFactory != null) ? entry.getLastAccess().rebase() : entry.getLastAccess();
        Mutator mutator = (this.mutatorFactory != null) ? this.mutatorFactory.createMutator(new InfinispanBeanMetaDataKey<>(id), lastAccess) : Mutator.PASSIVE;
        return new DefaultBeanMetaData<>((this.mutatorFactory != null) ? new MutableBeanMetaDataEntry<>(entry, lastAccess) : entry, this.expiration, mutator);
    }
}

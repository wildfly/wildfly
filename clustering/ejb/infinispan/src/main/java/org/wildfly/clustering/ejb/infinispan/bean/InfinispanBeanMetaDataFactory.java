/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.bean;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

import org.infinispan.Cache;
import org.wildfly.clustering.cache.CacheEntryMutator;
import org.wildfly.clustering.cache.CacheEntryMutatorFactory;
import org.wildfly.clustering.ejb.bean.BeanInstance;
import org.wildfly.clustering.ejb.bean.BeanMetaData;
import org.wildfly.clustering.ejb.bean.ImmutableBeanMetaData;
import org.wildfly.clustering.ejb.cache.bean.BeanMetaDataEntryFunction;
import org.wildfly.clustering.ejb.cache.bean.BeanMetaDataFactory;
import org.wildfly.clustering.ejb.cache.bean.BeanMetaDataKey;
import org.wildfly.clustering.ejb.cache.bean.DefaultBeanMetaData;
import org.wildfly.clustering.ejb.cache.bean.DefaultBeanMetaDataEntry;
import org.wildfly.clustering.ejb.cache.bean.DefaultImmutableBeanMetaData;
import org.wildfly.clustering.ejb.cache.bean.MutableBeanMetaDataEntry;
import org.wildfly.clustering.ejb.cache.bean.RemappableBeanMetaDataEntry;
import org.wildfly.clustering.function.Consumer;
import org.wildfly.clustering.function.Function;
import org.wildfly.clustering.server.offset.OffsetValue;

/**
 * A {@link BeanMetaDataFactory} whose metadata entries are stored in an embedded Infinispan cache.
 * @author Paul Ferraro
 * @param <K> the bean identifier type
 */
public class InfinispanBeanMetaDataFactory<K> implements BeanMetaDataFactory<K, RemappableBeanMetaDataEntry<K>> {

    private final Cache<BeanMetaDataKey<K>, RemappableBeanMetaDataEntry<K>> writeOnlyCache;
    private final Cache<BeanMetaDataKey<K>, RemappableBeanMetaDataEntry<K>> readForUpdateCache;
    private final Cache<BeanMetaDataKey<K>, RemappableBeanMetaDataEntry<K>> tryReadForUpdateCache;
    private final CacheEntryMutatorFactory<BeanMetaDataKey<K>, OffsetValue<Instant>> mutatorFactory;
    private final Optional<Duration> maxIdle;
    private final String beanName;

    public InfinispanBeanMetaDataFactory(InfinispanBeanMetaDataFactoryConfiguration configuration) {
        this.writeOnlyCache = configuration.getWriteOnlyCache();
        this.readForUpdateCache = configuration.getReadForUpdateCache();
        this.tryReadForUpdateCache = configuration.getTryReadForUpdateCache();
        this.maxIdle = configuration.getMaxIdle();
        this.mutatorFactory = this.maxIdle.isPresent() ? configuration.getCacheEntryMutatorFactory(BeanMetaDataEntryFunction::new) : null;
        this.beanName = configuration.getBeanName();
    }

    @Override
    public CompletionStage<RemappableBeanMetaDataEntry<K>> createValueAsync(BeanInstance<K> instance, K groupId) {
        RemappableBeanMetaDataEntry<K> entry = new DefaultBeanMetaDataEntry<>(this.beanName, groupId);
        return this.writeOnlyCache.putAsync(new InfinispanBeanMetaDataKey<>(instance.getId()), entry).thenApply(Function.of(entry));
    }

    @Override
    public CompletionStage<RemappableBeanMetaDataEntry<K>> findValueAsync(K id) {
        return this.readForUpdateCache.getAsync(new InfinispanBeanMetaDataKey<>(id));
    }

    @Override
    public CompletionStage<RemappableBeanMetaDataEntry<K>> tryValueAsync(K id) {
        return this.tryReadForUpdateCache.getAsync(new InfinispanBeanMetaDataKey<>(id));
    }

    @Override
    public CompletionStage<Void> removeAsync(K id) {
        return this.writeOnlyCache.removeAsync(new InfinispanBeanMetaDataKey<>(id)).thenAccept(Consumer.empty());
    }

    @Override
    public void remove(K id) {
        this.writeOnlyCache.remove(new InfinispanBeanMetaDataKey<>(id));
    }

    @Override
    public ImmutableBeanMetaData<K> createImmutableBeanMetaData(K id, RemappableBeanMetaDataEntry<K> entry) {
        return new DefaultImmutableBeanMetaData<>(entry, this.maxIdle);
    }

    @Override
    public BeanMetaData<K> createBeanMetaData(K id, RemappableBeanMetaDataEntry<K> entry) {
        OffsetValue<Instant> lastAccess = (this.mutatorFactory != null) ? entry.getLastAccessTime().rebase() : entry.getLastAccessTime();
        Runnable mutator = (this.mutatorFactory != null) ? this.mutatorFactory.createMutator(new InfinispanBeanMetaDataKey<>(id), lastAccess) : CacheEntryMutator.EMPTY;
        return new DefaultBeanMetaData<>((this.mutatorFactory != null) ? new MutableBeanMetaDataEntry<>(entry, lastAccess) : entry, this.maxIdle, mutator);
    }
}

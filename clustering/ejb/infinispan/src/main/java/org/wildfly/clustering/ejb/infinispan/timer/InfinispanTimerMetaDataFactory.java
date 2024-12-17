/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.timer;

import static org.wildfly.clustering.cache.function.Functions.constantFunction;
import static org.wildfly.common.function.Functions.discardingConsumer;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.infinispan.Cache;
import org.infinispan.context.Flag;
import org.wildfly.clustering.cache.CacheEntryMutator;
import org.wildfly.clustering.cache.infinispan.embedded.EmbeddedCacheEntryComputer;
import org.wildfly.clustering.ejb.cache.timer.DefaultImmutableTimerMetaData;
import org.wildfly.clustering.ejb.cache.timer.DefaultTimerMetaData;
import org.wildfly.clustering.ejb.cache.timer.MutableTimerMetaDataEntry;
import org.wildfly.clustering.ejb.cache.timer.RemappableTimerMetaDataEntry;
import org.wildfly.clustering.ejb.cache.timer.TimerIndex;
import org.wildfly.clustering.ejb.cache.timer.TimerIndexKey;
import org.wildfly.clustering.ejb.cache.timer.TimerMetaDataConfiguration;
import org.wildfly.clustering.ejb.cache.timer.TimerMetaDataEntryFunction;
import org.wildfly.clustering.ejb.cache.timer.TimerMetaDataFactory;
import org.wildfly.clustering.ejb.cache.timer.TimerMetaDataKey;
import org.wildfly.clustering.ejb.timer.ImmutableTimerMetaData;
import org.wildfly.clustering.ejb.timer.TimerMetaData;
import org.wildfly.clustering.server.offset.OffsetValue;

/**
 * @author Paul Ferraro
 */
public class InfinispanTimerMetaDataFactory<I, C> implements TimerMetaDataFactory<I, RemappableTimerMetaDataEntry<C>> {

    private final Cache<TimerIndexKey, I> indexCache;
    private final Cache<TimerMetaDataKey<I>, RemappableTimerMetaDataEntry<C>> readCache;
    private final Cache<TimerMetaDataKey<I>, RemappableTimerMetaDataEntry<C>> writeCache;
    private final Cache<TimerMetaDataKey<I>, RemappableTimerMetaDataEntry<C>> removeCache;
    private final TimerMetaDataConfiguration<C> config;

    public InfinispanTimerMetaDataFactory(InfinispanTimerMetaDataConfiguration<C> config) {
        this.config = config;
        this.indexCache = config.<TimerIndexKey, I>getCache().getAdvancedCache().withFlags(Flag.FORCE_WRITE_LOCK);
        this.readCache = config.getReadForUpdateCache();
        this.writeCache = config.getSilentWriteCache();
        this.removeCache = config.getCache();
    }

    @Override
    public CompletionStage<RemappableTimerMetaDataEntry<C>> createValueAsync(I id, Map.Entry<RemappableTimerMetaDataEntry<C>, TimerIndex> entry) {
        RemappableTimerMetaDataEntry<C> metaData = entry.getKey();
        TimerIndex index = entry.getValue();
        // Create index, if necessary
        CompletionStage<I> existingIndex = (index != null) ? this.indexCache.putIfAbsentAsync(new InfinispanTimerIndexKey(index), id) : CompletableFuture.completedStage(null);
        // If a timer with the same index already exists, return null;
        return existingIndex.thenCompose(indexId -> (indexId == null) ? this.writeCache.putAsync(new InfinispanTimerMetaDataKey<>(id), metaData).thenApply(constantFunction(metaData)) : CompletableFuture.completedStage(null));
    }

    @Override
    public CompletionStage<RemappableTimerMetaDataEntry<C>> findValueAsync(I id) {
        return this.readCache.getAsync(new InfinispanTimerMetaDataKey<>(id));
    }

    @Override
    public CompletionStage<Void> removeAsync(I id) {
        return this.removeCache.removeAsync(new InfinispanTimerMetaDataKey<>(id)).thenAccept(discardingConsumer());
    }

    @Override
    public TimerMetaData createTimerMetaData(I id, RemappableTimerMetaDataEntry<C> entry) {
        Duration lastTimeout = entry.getLastTimeout();
        OffsetValue<Duration> lastTimeoutOffset = OffsetValue.from(Optional.ofNullable(lastTimeout).orElse(Duration.ZERO));
        CacheEntryMutator mutator = new EmbeddedCacheEntryComputer<>(this.writeCache, new InfinispanTimerMetaDataKey<>(id), new TimerMetaDataEntryFunction<>(lastTimeoutOffset));
        return new DefaultTimerMetaData<>(this.config, (lastTimeout != null) ? new MutableTimerMetaDataEntry<>(entry, lastTimeoutOffset) : entry, mutator);
    }

    @Override
    public ImmutableTimerMetaData createImmutableTimerMetaData(RemappableTimerMetaDataEntry<C> entry) {
        return new DefaultImmutableTimerMetaData<>(this.config, entry);
    }
}

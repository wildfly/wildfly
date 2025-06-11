/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.timer;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.infinispan.Cache;
import org.infinispan.context.Flag;
import org.wildfly.clustering.cache.CacheEntryMutatorFactory;
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
import org.wildfly.clustering.function.Consumer;
import org.wildfly.clustering.function.Function;
import org.wildfly.clustering.function.Supplier;
import org.wildfly.clustering.server.offset.OffsetValue;

/**
 * @author Paul Ferraro
 */
public class InfinispanTimerMetaDataFactory<I, C> implements TimerMetaDataFactory<I, RemappableTimerMetaDataEntry<C>> {

    private final Cache<TimerIndexKey, I> indexCache;
    private final Cache<TimerMetaDataKey<I>, RemappableTimerMetaDataEntry<C>> readCache;
    private final Cache<TimerMetaDataKey<I>, RemappableTimerMetaDataEntry<C>> readForUpdateCache;
    private final Cache<TimerMetaDataKey<I>, RemappableTimerMetaDataEntry<C>> writeCache;
    private final Cache<TimerMetaDataKey<I>, RemappableTimerMetaDataEntry<C>> removeCache;
    private final TimerMetaDataConfiguration<C> config;
    private final CacheEntryMutatorFactory<TimerMetaDataKey<I>, OffsetValue<Duration>> mutatorFactory;
    private final Supplier<CompletionStage<RemappableTimerMetaDataEntry<C>>> completed = Supplier.of(CompletableFuture.completedStage(null));

    public InfinispanTimerMetaDataFactory(InfinispanTimerMetaDataConfiguration<C> config) {
        this.config = config;
        this.indexCache = config.<TimerIndexKey, I>getCache().getAdvancedCache().withFlags(Flag.FORCE_WRITE_LOCK);
        this.readCache = config.getCache();
        this.readForUpdateCache = config.getReadForUpdateCache();
        this.writeCache = config.getSilentWriteCache();
        this.removeCache = config.getCache();
        this.mutatorFactory = config.getCacheEntryMutatorFactory(TimerMetaDataEntryFunction::new);
    }

    @Override
    public CompletionStage<RemappableTimerMetaDataEntry<C>> createValueAsync(I id, Map.Entry<RemappableTimerMetaDataEntry<C>, TimerIndex> entry) {
        RemappableTimerMetaDataEntry<C> metaData = entry.getKey();
        TimerIndex index = entry.getValue();
        // Create index, if necessary
        CompletionStage<I> existingIndex = (index != null) ? this.indexCache.putIfAbsentAsync(new InfinispanTimerIndexKey(index), id) : CompletableFuture.completedStage(null);
        Supplier<CompletionStage<RemappableTimerMetaDataEntry<C>>> createTimerMetaData = () -> this.writeCache.putAsync(new InfinispanTimerMetaDataKey<>(id), metaData).thenApply(Function.of(metaData));
        // If a timer with the same index already exists, return null;
        return existingIndex.thenCompose(Function.of(createTimerMetaData).orDefault(Objects::isNull, this.completed));
    }

    @Override
    public CompletionStage<RemappableTimerMetaDataEntry<C>> findValueAsync(I id) {
        return this.readForUpdateCache.getAsync(new InfinispanTimerMetaDataKey<>(id));
    }

    @Override
    public CompletionStage<RemappableTimerMetaDataEntry<C>> tryValueAsync(I id) {
        return this.readCache.getAsync(new InfinispanTimerMetaDataKey<>(id));
    }

    @Override
    public CompletionStage<Void> removeAsync(I id) {
        return this.removeCache.removeAsync(new InfinispanTimerMetaDataKey<>(id)).thenAccept(Consumer.empty());
    }

    @Override
    public TimerMetaData createTimerMetaData(I id, RemappableTimerMetaDataEntry<C> entry) {
        Duration lastTimeout = entry.getLastTimeout();
        OffsetValue<Duration> lastTimeoutOffset = OffsetValue.from(Optional.ofNullable(lastTimeout).orElse(Duration.ZERO));
        Runnable mutator = this.mutatorFactory.createMutator(new InfinispanTimerMetaDataKey<>(id), lastTimeoutOffset);
        return new DefaultTimerMetaData<>(this.config, (lastTimeout != null) ? new MutableTimerMetaDataEntry<>(entry, lastTimeoutOffset) : entry, mutator);
    }

    @Override
    public ImmutableTimerMetaData createImmutableTimerMetaData(RemappableTimerMetaDataEntry<C> entry) {
        return new DefaultImmutableTimerMetaData<>(this.config, entry);
    }
}

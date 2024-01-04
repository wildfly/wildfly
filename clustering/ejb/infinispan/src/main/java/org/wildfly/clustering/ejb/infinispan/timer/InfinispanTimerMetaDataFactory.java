/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.timer;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import org.infinispan.Cache;
import org.infinispan.context.Flag;
import org.wildfly.clustering.ee.Mutator;
import org.wildfly.clustering.ee.cache.offset.OffsetValue;
import org.wildfly.clustering.ee.infinispan.CacheEntryComputeMutator;
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

/**
 * @author Paul Ferraro
 */
public class InfinispanTimerMetaDataFactory<I, C> implements TimerMetaDataFactory<I, RemappableTimerMetaDataEntry<C>, C> {

    private final Cache<TimerIndexKey, I> indexCache;
    private final Cache<TimerMetaDataKey<I>, RemappableTimerMetaDataEntry<C>> readCache;
    private final Cache<TimerMetaDataKey<I>, RemappableTimerMetaDataEntry<C>> writeCache;
    private final Cache<TimerMetaDataKey<I>, RemappableTimerMetaDataEntry<C>> removeCache;
    private final TimerMetaDataConfiguration<C> config;

    public InfinispanTimerMetaDataFactory(InfinispanTimerMetaDataConfiguration<C> config) {
        this.config = config;
        this.indexCache = config.getCache();
        this.readCache = config.getReadForUpdateCache();
        this.writeCache = config.getSilentWriteCache();
        this.removeCache = config.getCache();
    }

    @Override
    public RemappableTimerMetaDataEntry<C> createValue(I id, Map.Entry<RemappableTimerMetaDataEntry<C>, TimerIndex> entry) {
        RemappableTimerMetaDataEntry<C> metaData = entry.getKey();
        TimerIndex index = entry.getValue();
        // If an timer with the same index already exists, return null;
        if ((index != null) && (this.indexCache.getAdvancedCache().withFlags(Flag.FORCE_WRITE_LOCK).putIfAbsent(new InfinispanTimerIndexKey(index), id) != null)) return null;

        this.writeCache.put(new InfinispanTimerMetaDataKey<>(id), metaData);
        return metaData;
    }

    @Override
    public RemappableTimerMetaDataEntry<C> findValue(I id) {
        return this.readCache.get(new InfinispanTimerMetaDataKey<>(id));
    }

    @Override
    public boolean remove(I id) {
        return this.removeCache.remove(new InfinispanTimerMetaDataKey<>(id)) != null;
    }

    @Override
    public TimerMetaData createTimerMetaData(I id, RemappableTimerMetaDataEntry<C> entry) {
        Duration lastTimeout = entry.getLastTimeout();
        OffsetValue<Duration> lastTimeoutOffset = OffsetValue.from(Optional.ofNullable(lastTimeout).orElse(Duration.ZERO));
        Mutator mutator = new CacheEntryComputeMutator<>(this.writeCache, new InfinispanTimerMetaDataKey<>(id), new TimerMetaDataEntryFunction<>(lastTimeoutOffset));
        return new DefaultTimerMetaData<>(this.config, (lastTimeout != null) ? new MutableTimerMetaDataEntry<>(entry, lastTimeoutOffset) : entry, mutator);
    }

    @Override
    public ImmutableTimerMetaData createImmutableTimerMetaData(RemappableTimerMetaDataEntry<C> entry) {
        return new DefaultImmutableTimerMetaData<>(this.config, entry);
    }
}

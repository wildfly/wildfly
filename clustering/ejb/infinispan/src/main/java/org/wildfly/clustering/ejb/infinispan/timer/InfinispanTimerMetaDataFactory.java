/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.timer;

import java.time.Duration;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map;

import org.infinispan.Cache;
import org.infinispan.context.Flag;
import org.wildfly.clustering.ejb.timer.ImmutableTimerMetaData;
import org.wildfly.clustering.ejb.timer.TimerMetaData;

/**
 * @author Paul Ferraro
 */
public class InfinispanTimerMetaDataFactory<I, V> implements TimerMetaDataFactory<I, V> {

    private final Cache<TimerIndexKey, I> indexCache;
    private final Cache<TimerCreationMetaDataKey<I>, TimerCreationMetaData<V>> creationMetaDataReadCache;
    private final Cache<TimerCreationMetaDataKey<I>, TimerCreationMetaData<V>> creationMetaDataWriteCache;
    private final Cache<TimerAccessMetaDataKey<I>, Duration> accessMetaDataCache;
    private final TimerMetaDataConfiguration<V> config;

    public InfinispanTimerMetaDataFactory(TimerMetaDataConfiguration<V> config) {
        this.config = config;
        this.indexCache = config.getCache();
        this.creationMetaDataReadCache = config.getReadForUpdateCache();
        this.creationMetaDataWriteCache = config.getSilentWriteCache();
        this.accessMetaDataCache = config.getSilentWriteCache();
    }

    @Override
    public Map.Entry<TimerCreationMetaData<V>, TimerAccessMetaData> createValue(I id, Map.Entry<TimerCreationMetaData<V>, TimerIndex> entry) {
        TimerCreationMetaData<V> creationMetaData = entry.getKey();
        TimerIndex index = entry.getValue();
        // If an timer with the same index already exists, return null;
        if ((index != null) && (this.indexCache.getAdvancedCache().withFlags(Flag.FORCE_WRITE_LOCK).putIfAbsent(new TimerIndexKey(index), id) != null)) return null;

        this.creationMetaDataWriteCache.put(new TimerCreationMetaDataKey<>(id), creationMetaData);
        TimerAccessMetaData accessMetaData = new TimerAccessMetaDataEntry<>(this.accessMetaDataCache, new TimerAccessMetaDataKey<>(id));
        return new SimpleImmutableEntry<>(creationMetaData, accessMetaData);
    }

    @Override
    public Map.Entry<TimerCreationMetaData<V>, TimerAccessMetaData> findValue(I id) {
        TimerCreationMetaData<V> creationMetaData = this.creationMetaDataReadCache.get(new TimerCreationMetaDataKey<>(id));
        return (creationMetaData != null) ? new SimpleImmutableEntry<>(creationMetaData, new TimerAccessMetaDataEntry<>(this.accessMetaDataCache, new TimerAccessMetaDataKey<>(id))) : null;
    }

    @Override
    public boolean remove(I id) {
        TimerCreationMetaDataKey<I> key = new TimerCreationMetaDataKey<>(id);
        TimerCreationMetaData<V> creationMetaData = this.creationMetaDataReadCache.get(key);
        if (creationMetaData != null) {
            this.accessMetaDataCache.remove(new TimerAccessMetaDataKey<>(id));
            this.creationMetaDataWriteCache.remove(key);
            return true;
        }
        return false;
    }

    @Override
    public TimerMetaData createTimerMetaData(I id, Map.Entry<TimerCreationMetaData<V>, TimerAccessMetaData> entry) {
        return new CompositeTimerMetaData<>(this.config, entry.getKey(), entry.getValue());
    }

    @Override
    public ImmutableTimerMetaData createImmutableTimerMetaData(Map.Entry<TimerCreationMetaData<V>, TimerAccessMetaData> entry) {
        return new CompositeImmutableTimerMetaData<>(this.config, entry.getKey(), entry.getValue());
    }
}

/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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

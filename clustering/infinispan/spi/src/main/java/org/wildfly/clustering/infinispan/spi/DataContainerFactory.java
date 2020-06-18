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

package org.wildfly.clustering.infinispan.spi;

import java.util.function.Predicate;
import java.util.function.Supplier;

import org.infinispan.commons.marshall.WrappedBytes;
import org.infinispan.commons.util.EntrySizeCalculator;
import org.infinispan.configuration.cache.ClusteringConfiguration;
import org.infinispan.configuration.cache.MemoryConfiguration;
import org.infinispan.configuration.cache.MemoryStorageConfiguration;
import org.infinispan.configuration.cache.StorageType;
import org.infinispan.container.DataContainer;
import org.infinispan.container.entries.InternalCacheEntry;
import org.infinispan.container.impl.BoundedSegmentedDataContainer;
import org.infinispan.container.impl.DefaultDataContainer;
import org.infinispan.container.impl.DefaultSegmentedDataContainer;
import org.infinispan.container.impl.InternalDataContainer;
import org.infinispan.container.impl.L1SegmentedDataContainer;
import org.infinispan.container.impl.PeekableTouchableContainerMap;
import org.infinispan.container.impl.PeekableTouchableMap;
import org.infinispan.container.offheap.BoundedOffHeapDataContainer;
import org.infinispan.container.offheap.OffHeapConcurrentMap;
import org.infinispan.container.offheap.OffHeapDataContainer;
import org.infinispan.container.offheap.OffHeapEntryFactory;
import org.infinispan.container.offheap.OffHeapMemoryAllocator;
import org.infinispan.container.offheap.SegmentedBoundedOffHeapDataContainer;
import org.infinispan.eviction.EvictionStrategy;
import org.infinispan.eviction.EvictionType;
import org.infinispan.factories.AbstractNamedCacheComponentFactory;
import org.infinispan.factories.AutoInstantiableFactory;
import org.infinispan.factories.annotations.DefaultFactoryFor;
import org.infinispan.factories.annotations.SurvivesRestarts;

/**
 * @author Paul Ferraro
 */
@DefaultFactoryFor(classes = InternalDataContainer.class)
@SurvivesRestarts
public class DataContainerFactory<K, V> extends AbstractNamedCacheComponentFactory implements AutoInstantiableFactory {

    @Override
    public Object construct(String componentName) {
        MemoryConfiguration memory = this.configuration.memory();
        EvictionStrategy strategy = memory.evictionStrategy();
        // handle case when < 0 value signifies unbounded container or when we are not removal based
        if (strategy.isExceptionBased() || !strategy.isEnabled()) {
            return this.createUnboundedContainer();
        }

        DataContainer<?, ?> container = this.createBoundedDataContainer();
        memory.heapConfiguration().attributes().attribute(MemoryStorageConfiguration.SIZE).addListener((newSize, oldSize) -> container.resize(newSize.get()));
        return container;
    }

    private DataContainer<?, ?> createUnboundedContainer() {
        ClusteringConfiguration clustering = this.configuration.clustering();
        boolean segmented = clustering.cacheMode().needsStateTransfer();
        int level = this.configuration.locking().concurrencyLevel();

        boolean offHeap = this.configuration.memory().storageType() == StorageType.OFF_HEAP;

        if (segmented) {
            Supplier<PeekableTouchableMap<WrappedBytes, WrappedBytes>> mapSupplier = offHeap ? this::createAndStartOffHeapConcurrentMap : PeekableTouchableContainerMap::new;
            int segments = clustering.hash().numSegments();
            return clustering.l1().enabled() ? new L1SegmentedDataContainer<>(mapSupplier, segments) : new DefaultSegmentedDataContainer<>(mapSupplier, segments);
        }
        return offHeap ? new OffHeapDataContainer() : DefaultDataContainer.unBoundedDataContainer(level);
    }

    private DataContainer<?, ?> createBoundedDataContainer() {
        ClusteringConfiguration clustering = this.configuration.clustering();
        boolean segmented = clustering.cacheMode().needsStateTransfer();
        int level = this.configuration.locking().concurrencyLevel();

        MemoryConfiguration memory = this.configuration.memory();
        boolean offHeap = memory.storageType() == StorageType.OFF_HEAP;
        long thresholdSize = memory.size();
        EvictionType type = memory.evictionType();
        DataContainerConfiguration config = this.configuration.module(DataContainerConfiguration.class);
        Predicate<?> evictable = (config != null) ? config.evictable() : DataContainerConfiguration.EVICTABLE_PREDICATE.getDefaultValue();

        if (segmented) {
            int segments = clustering.hash().numSegments();
            if (offHeap) {
                return new SegmentedBoundedOffHeapDataContainer(segments, thresholdSize, type);
            }
            return (type == EvictionType.COUNT) ? new BoundedSegmentedDataContainer<>(segments, thresholdSize, new EvictableEntrySizeCalculator<>(evictable)) : new BoundedSegmentedDataContainer<>(segments, thresholdSize, type);
        }
        if (offHeap) {
            return new BoundedOffHeapDataContainer(thresholdSize, type);
        }
        return (type == EvictionType.COUNT) ? new EvictableDataContainer<>(thresholdSize, new EvictableEntrySizeCalculator<>(evictable)) : DefaultDataContainer.boundedDataContainer(level, thresholdSize, type);
    }

    private OffHeapConcurrentMap createAndStartOffHeapConcurrentMap() {
        OffHeapEntryFactory entryFactory = this.basicComponentRegistry.getComponent(OffHeapEntryFactory.class).wired();
        OffHeapMemoryAllocator memoryAllocator = this.basicComponentRegistry.getComponent(OffHeapMemoryAllocator.class).wired();
        return new OffHeapConcurrentMap(memoryAllocator, entryFactory, null);
    }

    public static class EvictableDataContainer<K, V> extends DefaultDataContainer<K, V> {

        EvictableDataContainer(long size, EntrySizeCalculator<? super K, ? super InternalCacheEntry<K, V>> calculator) {
            super(size, calculator);
        }
    }

    private static class EvictableEntrySizeCalculator<K, V> implements EntrySizeCalculator<K, InternalCacheEntry<K, V>> {
        private final Predicate<K> evictable;

        EvictableEntrySizeCalculator(Predicate<K> evictable) {
            this.evictable = evictable;
        }

        @Override
        public long calculateSize(K key, InternalCacheEntry<K, V> value) {
            return this.evictable.test(key) ? 1 : 0;
        }
    }
}

/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

import org.infinispan.commons.util.EntrySizeCalculator;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.container.DataContainer;
import org.infinispan.container.entries.InternalCacheEntry;
import org.infinispan.container.impl.BoundedSegmentedDataContainer;
import org.infinispan.container.impl.DefaultDataContainer;

/**
 * Custom {@link DataContainer} considers only specific cache entries for eviction.
 * @author Paul Ferraro
 */
public class EvictableDataContainer<K, V> extends DefaultDataContainer<K, V> {

    protected EvictableDataContainer(long size, EntrySizeCalculator<? super K, ? super InternalCacheEntry<K, V>> calculator) {
        super(size, calculator);
    }

    public static <K, V> DataContainer<K, V> createDataContainer(ConfigurationBuilder builder, long size, Predicate<K> evictable) {
        EntrySizeCalculator<? super K, ? super InternalCacheEntry<K, V>> calculator = (key, entry) -> evictable.test(key) ? 1 : 0;
        return builder.clustering().cacheMode().needsStateTransfer() ? new BoundedSegmentedDataContainer<>(builder.clustering().hash().create().numSegments(), size, calculator) : new EvictableDataContainer<>(size, calculator);
    }
}

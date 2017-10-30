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

import java.util.Map;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.function.Predicate;

import org.infinispan.metadata.Metadata;
import org.infinispan.notifications.cachelistener.filter.CacheEventFilter;
import org.infinispan.notifications.cachelistener.filter.EventType;

/**
 * @author Paul Ferraro
 */
public class PredicateCacheEventFilter<K, V> implements CacheEventFilter<K, V> {

    private final Predicate<Map.Entry<? super K, ? super V>> predicate;

    public PredicateCacheEventFilter(Predicate<Map.Entry<? super K, ? super V>> predicate) {
        this.predicate = predicate;
    }

    @Override
    public boolean accept(K key, V oldValue, Metadata oldMetadata, V newValue, Metadata newMetadata, EventType eventType) {
        return this.predicate.test(new SimpleImmutableEntry<>(key, oldValue)) || this.predicate.test(new SimpleImmutableEntry<>(key, newValue));
    }
}

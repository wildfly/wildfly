/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.infinispan.listener;

import java.util.function.Predicate;

import org.infinispan.notifications.cachelistener.filter.CacheEventFilter;

/**
 * A registering cache listener.
 * @author Paul Ferraro
 */
public interface CacheListenerRegistrar<K, V> extends ListenerRegistrar {

    /**
     * Registers this listener events for cache entries whose key is an instance of the specified class.
     * @param keyClass a key class
     * @return a listener registration
     */
    default ListenerRegistration register(Class<? super K> keyClass) {
        return this.register(new KeyFilter<>(keyClass));
    }

    /**
     * Registers this listener events for cache entries whose key matches the specified predicate.
     * @param keyClass a key class
     * @return a listener registration
     */
    default ListenerRegistration register(Predicate<? super K> keyPredicate) {
        return this.register(new KeyFilter<>(keyPredicate));
    }

    /**
     * Registers this listener events for cache entries that match the specified filter.
     * @param filter a cache event filter
     * @return a listener registration
     */
    ListenerRegistration register(CacheEventFilter<? super K, ? super V> filter);
}

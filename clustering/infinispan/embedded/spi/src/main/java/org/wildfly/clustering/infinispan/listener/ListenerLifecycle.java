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

import org.infinispan.Cache;
import org.infinispan.commons.api.Lifecycle;
import org.infinispan.notifications.cachelistener.filter.CacheEventFilter;

/**
 * Instruments cache listener registration via a {@link Lifecycle}.
 * @author Paul Ferraro
 */
public class ListenerLifecycle<K, V> implements Lifecycle {
    private final Cache<K, V> cache;
    private final Object listener;
    private final CacheEventFilter<? super K, ? super V> filter;

    public ListenerLifecycle(Cache<K, V> cache, Object listener) {
        this(cache, listener, (CacheEventFilter<? super K, ? super V>) null);
    }

    public ListenerLifecycle(Cache<K, V> cache, Object listener, Predicate<? super K> keyPredicate) {
        this(cache, listener, new PredicateKeyFilter<>(keyPredicate));
    }

    public ListenerLifecycle(Cache<K, V> cache, Object listener, CacheEventFilter<? super K, ? super V> filter) {
        this.cache = cache;
        this.listener = listener;
        this.filter = filter;
    }

    @Override
    public void start() {
        if (this.filter != null) {
            this.cache.addListener(this.listener, this.filter, null);
        } else {
            this.cache.addListener(this.listener);
        }
    }

    @Override
    public void stop() {
        this.cache.removeListener(this.listener);
    }
}

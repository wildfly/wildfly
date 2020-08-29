/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.infinispan;

import java.util.function.Consumer;

import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.infinispan.cache.impl.AbstractDelegatingAdvancedCache;
import org.infinispan.manager.EmbeddedCacheManager;

/**
 * {@link AdvancedCache} decorator associated with a {@link DefaultCacheContainer}.
 * @author Paul Ferraro
 */
public class DefaultCache<K, V> extends AbstractDelegatingAdvancedCache<K, V> {
    private final EmbeddedCacheManager manager;
    private final Consumer<Cache<?, ?>> consumer;

    DefaultCache(EmbeddedCacheManager manager, AdvancedCache<K, V> cache, Consumer<Cache<?, ?>> consumer) {
        super(cache);
        this.manager = manager;
        this.consumer = consumer;
    }

    @Override
    public EmbeddedCacheManager getCacheManager() {
        return this.manager;
    }

    @Override
    public synchronized void start() {
        super.start();
        this.consumer.accept(this);
    }

    @Override
    public synchronized void stop() {
        this.consumer.accept(null);
        super.stop();
    }

    @Override
    public boolean equals(Object object) {
        return (object == this) || (object == this.cache);
    }

    @Override
    public int hashCode() {
        return this.cache.hashCode();
    }

    @SuppressWarnings("unchecked")
    @Override
    public AdvancedCache rewrap(AdvancedCache newDelegate) {
        return new DefaultCache<>(this.manager, newDelegate, this.consumer);
    }
}

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

package org.wildfly.clustering.ee.hotrod;

import java.time.Duration;
import java.util.function.Function;

import org.infinispan.client.hotrod.RemoteCache;
import org.wildfly.clustering.ee.Mutator;
import org.wildfly.clustering.ee.MutatorFactory;

/**
 * Factory for creating a {@link Mutator} for a remote cache entry.
 * @author Paul Ferraro
 */
public class RemoteCacheMutatorFactory<K, V> implements MutatorFactory<K, V> {

    private final RemoteCache<K, V> cache;
    private final Function<V, Duration> maxIdle;

    public RemoteCacheMutatorFactory(RemoteCache<K, V> cache) {
        this(cache, null);
    }

    public RemoteCacheMutatorFactory(RemoteCache<K, V> cache, Function<V, Duration> maxIdle) {
        this.cache = cache;
        this.maxIdle = maxIdle;
    }

    @Override
    public Mutator createMutator(K key, V value) {
        return new RemoteCacheEntryMutator<>(this.cache, this.maxIdle, key, value);
    }
}

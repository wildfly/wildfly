/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee.hotrod;

import java.util.function.BiFunction;
import java.util.function.Function;

import org.infinispan.client.hotrod.Flag;
import org.infinispan.client.hotrod.RemoteCache;
import org.wildfly.clustering.ee.Mutator;
import org.wildfly.clustering.ee.MutatorFactory;

/**
 * Factory that creates compute-based Mutator instances.
 * @author Paul Ferraro
 * @param <K> the cache key type
 * @param <V> the cache value type
 * @param <O> the function operand type
 */
public class RemoteCacheComputeMutatorFactory<K, V, O> implements MutatorFactory<K, O> {

    private final RemoteCache<K, V> cache;
    private final Flag[] flags;
    private final Function<O, BiFunction<Object, V, V>> functionFactory;

    public RemoteCacheComputeMutatorFactory(RemoteCache<K, V> cache, Flag[] flags, Function<O, BiFunction<Object, V, V>> functionFactory) {
        this.cache = cache;
        this.flags = flags;
        this.functionFactory = functionFactory;
    }

    @Override
    public Mutator createMutator(K key, O operand) {
        return new RemoteCacheEntryComputeMutator<>(this.cache, this.flags, key, this.functionFactory.apply(operand));
    }
}

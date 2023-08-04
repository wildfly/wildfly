/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee.infinispan;

import java.util.function.BiFunction;
import java.util.function.Function;

import org.infinispan.Cache;
import org.wildfly.clustering.ee.Mutator;
import org.wildfly.clustering.ee.MutatorFactory;

/**
 * Factory that creates compute-based Mutator instances.
 * @author Paul Ferraro
 * @param <K> the cache key type
 * @param <V> the cache value type
 * @param <O> the function operand type
 */
public class CacheComputeMutatorFactory<K, V, O> implements MutatorFactory<K, O> {

    private final Cache<K, V> cache;
    private final Function<O, BiFunction<Object, V, V>> functionFactory;

    public CacheComputeMutatorFactory(Cache<K, V> cache, Function<O, BiFunction<Object, V, V>> functionFactory) {
        this.cache = cache;
        this.functionFactory = functionFactory;
    }

    @Override
    public Mutator createMutator(K key, O operand) {
        return new CacheEntryComputeMutator<>(this.cache, key, this.functionFactory.apply(operand));
    }
}

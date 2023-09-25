/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee.cache.function;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Defines operations for creating and copying a concurrent set.
 * A concurrent set can perform writes against the set directly, thus an explicitly copy is unnecessary.
 * @author Paul Ferraro
 * @param <V> the set element type
 */
public class ConcurrentSetOperations<V> implements Operations<Set<V>> {

    @Override
    public Set<V> apply(Set<V> set) {
        return set;
    }

    @Override
    public Set<V> get() {
        return new CopyOnWriteArraySet<>();
    }
}

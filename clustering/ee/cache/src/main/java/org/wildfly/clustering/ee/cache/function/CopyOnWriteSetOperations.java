/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee.cache.function;

import java.util.HashSet;
import java.util.Set;

/**
 * Defines operations for creating and copying a non-concurrent set.
 * A non-concurrent set must perform writes against a copy, thus the underlying set need not be concurrent.
 * @author Paul Ferraro
 * @param <V> the set element type
 */
public class CopyOnWriteSetOperations<V> implements Operations<Set<V>> {

    @Override
    public Set<V> apply(Set<V> set) {
        return new HashSet<>(set);
    }

    @Override
    public Set<V> get() {
        return new HashSet<>();
    }
}

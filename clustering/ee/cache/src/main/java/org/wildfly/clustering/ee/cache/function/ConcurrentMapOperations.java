/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee.cache.function;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Defines operations for creating and copying a concurrent map.
 * A concurrent map can perform writes against the map directly, thus an explicitly copy is unnecessary.
 * @author Paul Ferraro
 * @param <K> the map key type
 * @param <V> the map value type
 */
public class ConcurrentMapOperations<K, V> implements Operations<Map<K, V>> {

    @Override
    public Map<K, V> apply(Map<K, V> map) {
        return map;
    }

    @Override
    public Map<K, V> get() {
        return new ConcurrentHashMap<>();
    }
}

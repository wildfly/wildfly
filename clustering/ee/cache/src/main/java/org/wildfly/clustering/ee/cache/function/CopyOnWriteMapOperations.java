/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee.cache.function;

import java.util.HashMap;
import java.util.Map;

/**
 * Defines operations for creating and copying a non-concurrent map.
 * A non-concurrent map must perform writes against a copy, thus the underlying map need not be concurrent.
 * @author Paul Ferraro
 * @param <K> the map key type
 * @param <V> the map value type
 */
public class CopyOnWriteMapOperations<K, V> implements Operations<Map<K, V>> {

    @Override
    public Map<K, V> apply(Map<K, V> map) {
        return new HashMap<>(map);
    }

    @Override
    public Map<K, V> get() {
        return new HashMap<>();
    }
}

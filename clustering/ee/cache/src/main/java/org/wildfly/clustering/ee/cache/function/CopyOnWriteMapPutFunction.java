/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee.cache.function;

import java.util.AbstractMap;
import java.util.Map;

/**
 * Function that puts an entry into a map within a transactional cache.
 * @author Paul Ferraro
 * @param <K> the map key type
 * @param <V> the map value type
 */
public class CopyOnWriteMapPutFunction<K, V> extends MapPutFunction<K, V> {

    public CopyOnWriteMapPutFunction(K key, V value) {
        this(new AbstractMap.SimpleImmutableEntry<>(key, value));
    }

    public CopyOnWriteMapPutFunction(Map.Entry<K, V> operand) {
        super(operand, new CopyOnWriteMapOperations<>());
    }
}

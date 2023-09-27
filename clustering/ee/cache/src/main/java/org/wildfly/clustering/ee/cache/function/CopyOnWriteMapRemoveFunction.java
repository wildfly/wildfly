/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee.cache.function;

/**
 * Function that removes an entry from a map within a transactional cache.
 * @author Paul Ferraro
 * @param <K> the map key type
 * @param <V> the map value type
 */
public class CopyOnWriteMapRemoveFunction<K, V> extends MapRemoveFunction<K, V> {

    public CopyOnWriteMapRemoveFunction(K key) {
        super(key, new CopyOnWriteMapOperations<>());
    }
}

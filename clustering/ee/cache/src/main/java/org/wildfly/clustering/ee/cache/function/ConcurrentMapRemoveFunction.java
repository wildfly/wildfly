/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee.cache.function;

/**
 * Function that removes an entry from a map within a non-transactional cache.
 * @author Paul Ferraro
 * @param <K> the map key type
 * @param <V> the map value type
 * @deprecated Superseded by {@link MapRemoveFunction}.
 */
@Deprecated(forRemoval = true)
public class ConcurrentMapRemoveFunction<K, V> extends MapRemoveFunction<K, V> {

    public ConcurrentMapRemoveFunction(K key) {
        super(key);
    }
}

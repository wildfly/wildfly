/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee.cache.function;

import java.util.Collections;
import java.util.Map;

/**
 * Function that removes an entry from a map.
 * @author Paul Ferraro
 * @param <K> the map key type
 * @param <V> the map value type
 */
public class MapRemoveFunction<K, V> extends MapFunction<K, V, K> {

    public MapRemoveFunction(K operand, Operations<Map<K, V>> operations) {
        super(operand, operations, Collections::emptyMap);
    }

    @Override
    public void accept(Map<K, V> map, K key) {
        map.remove(key);
    }
}

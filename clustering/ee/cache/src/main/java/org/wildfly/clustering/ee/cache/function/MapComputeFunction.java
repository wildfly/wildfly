/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee.cache.function;

import java.util.Map;

/**
 * Function that applies updates to a map.
 * @author Paul Ferraro
 * @param <K> the map key type
 * @param <V> the map value type
 */
public class MapComputeFunction<K, V> extends MapFunction<K, V, Map<K, V>> {

    public MapComputeFunction(Map<K, V> operand) {
        super(operand, MapOperations.forOperandKey(operand.keySet().iterator().next()));
    }

    @Override
    public void accept(Map<K, V> map, Map<K, V> operand) {
        for (Map.Entry<K, V> entry : operand.entrySet()) {
            K key = entry.getKey();
            V value = entry.getValue();
            if (value != null) {
                map.put(key, value);
            } else {
                map.remove(key);
            }
        }
    }
}

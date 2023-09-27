/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee.cache.function;

import java.util.Map;

/**
 * Function that puts an entry into a map.
 * @author Paul Ferraro
 * @param <K> the map key type
 * @param <V> the map value type
 */
public class MapPutFunction<K, V> extends MapFunction<K, V, Map.Entry<K, V>> {

    public MapPutFunction(Map.Entry<K, V> operand, Operations<Map<K, V>> operations) {
        super(operand, operations, operations);
    }

    @Override
    public Map.Entry<K, V> getOperand() {
        return super.getOperand();
    }

    @Override
    public void accept(Map<K, V> map, Map.Entry<K, V> entry) {
        map.put(entry.getKey(), entry.getValue());
    }
}

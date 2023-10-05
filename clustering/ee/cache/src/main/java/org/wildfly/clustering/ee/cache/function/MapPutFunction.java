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
public class MapPutFunction<K, V> extends MapComputeFunction<K, V> {

    public MapPutFunction(K key, V value) {
        super(Map.of(key, value));
    }

    MapPutFunction(Map.Entry<K, V> entry) {
        this(entry.getKey(), entry.getValue());
    }
}

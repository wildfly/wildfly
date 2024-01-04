/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee.cache.function;

import java.util.Map;

/**
 * Function that operates on a map.
 * @author Paul Ferraro
 * @param <K> the map key type
 * @param <V> the map value type
 */
public abstract class MapFunction<K, V, T> extends AbstractFunction<T, Map<K, V>> {

    public MapFunction(T operand, Operations<Map<K, V>> operations) {
        super(operand, operations.getCopier(), operations.getFactory(), operations.isEmpty());
    }
}

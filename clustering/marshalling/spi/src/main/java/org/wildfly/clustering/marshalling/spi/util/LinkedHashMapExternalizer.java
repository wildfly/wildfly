/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.spi.util;

import static org.wildfly.clustering.marshalling.spi.util.HashSetExternalizer.DEFAULT_LOAD_FACTOR;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import org.wildfly.clustering.marshalling.spi.BooleanExternalizer;

/**
 * @author Paul Ferraro
 */
public class LinkedHashMapExternalizer extends MapExternalizer<LinkedHashMap<Object, Object>, Boolean, Map.Entry<Boolean, Integer>> {

    public static final Function<Map.Entry<Boolean, Integer>, LinkedHashMap<Object, Object>> FACTORY = new Function<>() {
        @Override
        public LinkedHashMap<Object, Object> apply(Map.Entry<Boolean, Integer> entry) {
            int size = entry.getValue();
            int capacity = HashSetExternalizer.CAPACITY.applyAsInt(size);
            return new LinkedHashMap<>(capacity, DEFAULT_LOAD_FACTOR, entry.getKey());
        }
    };

    public static final Function<LinkedHashMap<Object, Object>, Boolean> ACCESS_ORDER = new Function<>() {
        @Override
        public Boolean apply(LinkedHashMap<Object, Object> map) {
            Object insertOrder = new Object();
            Object accessOrder = new Object();
            map.put(insertOrder, null);
            map.put(accessOrder, null);
            // Access first inserted entry
            // If map uses access order, this element will move to the tail of the map
            map.get(insertOrder);
            Iterator<Object> keys = map.keySet().iterator();
            Object element = keys.next();
            while ((element != insertOrder) && (element != accessOrder)) {
                element = keys.next();
            }
            map.remove(insertOrder);
            map.remove(accessOrder);
            return element == accessOrder;
        }
    };

    @SuppressWarnings("unchecked")
    public LinkedHashMapExternalizer() {
        super((Class<LinkedHashMap<Object, Object>>) (Class<?>) LinkedHashMap.class, FACTORY, Function.identity(), ACCESS_ORDER, new BooleanExternalizer<>(Boolean.class, Function.identity(), Function.identity()));
    }
}

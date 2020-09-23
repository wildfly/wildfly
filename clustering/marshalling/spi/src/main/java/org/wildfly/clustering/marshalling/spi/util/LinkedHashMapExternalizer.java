/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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

    public static final Function<Map.Entry<Boolean, Integer>, LinkedHashMap<Object, Object>> FACTORY = new Function<Map.Entry<Boolean, Integer>, LinkedHashMap<Object, Object>>() {
        @Override
        public LinkedHashMap<Object, Object> apply(Map.Entry<Boolean, Integer> entry) {
            int size = entry.getValue().intValue();
            int capacity = HashSetExternalizer.CAPACITY.applyAsInt(size);
            return new LinkedHashMap<>(capacity, DEFAULT_LOAD_FACTOR, entry.getKey());
        }
    };

    public static final Function<LinkedHashMap<Object, Object>, Boolean> ACCESS_ORDER = new Function<LinkedHashMap<Object, Object>, Boolean>() {
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

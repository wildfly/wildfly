/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.util;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class CopyOnWriteArrayListMultiMap<K, V> {

    private final ConcurrentMap<K, CopyOnWriteArrayList<V>> cache = new ConcurrentHashMap<K, CopyOnWriteArrayList<V>>();

    public List<V> get(K k) {
        return cache.get(k);
    }

    public List<V> remove(K k) {
        return cache.remove(k);
    }

    public void putIfAbsent(K k, V v) {
        cache.computeIfAbsent(k, key -> new CopyOnWriteArrayList<>()).addIfAbsent(v);
    }

    public boolean remove(K k, V v) {
        List<V> list = cache.get(k);
        if (list == null) {
            return false;
        }
        if (list.isEmpty()) {

            return false;
        }
        return list.remove(v);
    }

}

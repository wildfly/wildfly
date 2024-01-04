/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class CopyOnWriteArrayListMultiMap<K, V> {

    private final ConcurrentMap<K, List<V>> cache = new ConcurrentHashMap<K, List<V>>();

    public List<V> get(K k) {
        return cache.get(k);
    }

    public synchronized List<V> remove(K k) {
        return cache.remove(k);
    }

    public synchronized void putIfAbsent(K k, V v) {
        List<V> list = cache.get(k);
        if (list == null || list.isEmpty()) {
            list = new ArrayList<V>();
        } else {
            list = new ArrayList<V>(list);
        }
        if (!list.contains(v)) {
            list.add(v);
            cache.put(k, list);
        }
    }

    public synchronized boolean remove(K k, V v) {
        List<V> list = cache.get(k);
        if (list == null) {
            return false;
        }
        if (list.isEmpty()) {
            cache.remove(k);
            return false;
        }
        boolean removed = list.remove(v);
        if (removed) {
            if (list.isEmpty()) {
                cache.remove(k);
            } else {
                list = new ArrayList<V>(list);
                cache.put(k, list);
            }
        }
        return removed;
    }

}

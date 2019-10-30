/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

    public synchronized boolean remove(K k, K v) {
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

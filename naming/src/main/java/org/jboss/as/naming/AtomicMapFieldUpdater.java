/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.naming;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import org.jboss.as.naming.logging.NamingLogger;
import org.jboss.as.naming.util.FastCopyHashMap;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class AtomicMapFieldUpdater<C, K, V> {
    private final AtomicReferenceFieldUpdater<C, Map<K, V>> updater;

    @SuppressWarnings( { "unchecked" })
    public static <C, K, V> AtomicMapFieldUpdater<C, K, V> newMapUpdater(AtomicReferenceFieldUpdater<C, Map> updater) {
        return new AtomicMapFieldUpdater<C, K, V>(updater);
    }

    @SuppressWarnings( { "unchecked" })
    AtomicMapFieldUpdater(AtomicReferenceFieldUpdater<C, Map> updater) {
        this.updater = (AtomicReferenceFieldUpdater) updater;
    }

    public void clear(C instance) {
        updater.set(instance, Collections.<K, V>emptyMap());
    }

    public V get(C instance, Object key) {
        return updater.get(instance).get(key);
    }

    public V put(C instance, K key, V value) {
        if (key == null) {
            throw NamingLogger.ROOT_LOGGER.nullVar("key");
        }
        for (;;) {
            final Map<K, V> oldMap = updater.get(instance);
            final Map<K, V> newMap;
            final V oldValue;
            final int oldSize = oldMap.size();
            if (oldSize == 0) {
                oldValue = null;
                newMap = Collections.singletonMap(key, value);
            } else if (oldSize == 1) {
                final Map.Entry<K, V> entry = oldMap.entrySet().iterator().next();
                final K oldKey = entry.getKey();
                if (oldKey.equals(key)) {
                    newMap = Collections.singletonMap(key, value);
                    oldValue = entry.getValue();
                } else {
                    newMap = new FastCopyHashMap<K, V>(oldMap);
                    oldValue = newMap.put(key, value);
                }
            } else {
                newMap = new FastCopyHashMap<K, V>(oldMap);
                oldValue = newMap.put(key, value);
            }
            final boolean result = updater.compareAndSet(instance, oldMap, newMap);
            if (result) {
                return oldValue;
            }
        }
    }

    /**
     * Put a value if and only if the map has not changed since the given snapshot was taken.
     *
     * @param instance the instance with the map field
     * @param key the key
     * @param value the value
     * @param snapshot the map snapshot
     * @return {@code value} if the snapshot is out of date, {@code null} if it succeeded, the existing value if the put failed
     */
    public V putAtomic(C instance, K key, V value, Map<K, V> snapshot) {
        if (key == null) {
            throw NamingLogger.ROOT_LOGGER.nullVar("key");
        }
        final Map<K, V> newMap;
        final int oldSize = snapshot.size();
        if (oldSize == 0) {
            newMap = Collections.singletonMap(key, value);
        } else if (oldSize == 1) {
            final Map.Entry<K, V> entry = snapshot.entrySet().iterator().next();
            final K oldKey = entry.getKey();
            if (oldKey.equals(key)) {
                return entry.getValue();
            } else {
                newMap = new FastCopyHashMap<K, V>(snapshot);
                newMap.put(key, value);
            }
        } else {
            newMap = new FastCopyHashMap<K, V>(snapshot);
            newMap.put(key, value);
        }
        if (updater.compareAndSet(instance, snapshot, newMap)) {
            return null;
        } else {
            return value;
        }
    }

    public V putIfAbsent(C instance, K key, V value) {
        if (key == null) {
            throw NamingLogger.ROOT_LOGGER.nullVar("key");
        }
        for (;;) {
            final Map<K, V> oldMap = updater.get(instance);
            final Map<K, V> newMap;
            final int oldSize = oldMap.size();
            if (oldSize == 0) {
                newMap = Collections.singletonMap(key, value);
            } else if (oldSize == 1) {
                final Map.Entry<K, V> entry = oldMap.entrySet().iterator().next();
                final K oldKey = entry.getKey();
                if (oldKey.equals(key)) {
                    return entry.getValue();
                } else {
                    newMap = new FastCopyHashMap<K, V>(oldMap);
                    newMap.put(key, value);
                }
            } else {
                if (oldMap.containsKey(key)) {
                    return oldMap.get(key);
                }
                newMap = new FastCopyHashMap<K, V>(oldMap);
                newMap.put(key, value);
            }
            if (updater.compareAndSet(instance, oldMap, newMap)) {
                return null;
            }
        }
    }

    public V remove(C instance, K key) {
        if (key == null) {
            return null;
        }
        for (;;) {
            final Map<K, V> oldMap = updater.get(instance);
            final Map<K, V> newMap;
            final V oldValue;
            final int oldSize = oldMap.size();
            if (oldSize == 0) {
                return null;
            } else if (oldSize == 1) {
                final Map.Entry<K, V> entry = oldMap.entrySet().iterator().next();
                if (entry.getKey().equals(key)) {
                    newMap = Collections.emptyMap();
                    oldValue = entry.getValue();
                } else {
                    return null;
                }
            } else if (oldSize == 2) {
                final Iterator<Map.Entry<K,V>> i = oldMap.entrySet().iterator();
                final Map.Entry<K, V> entry = i.next();
                final Map.Entry<K, V> next = i.next();
                if (entry.getKey().equals(key)) {
                    newMap = Collections.singletonMap(next.getKey(), next.getValue());
                    oldValue = entry.getValue();
                } else if (next.getKey().equals(key)) {
                    newMap = Collections.singletonMap(entry.getKey(), entry.getValue());
                    oldValue = next.getValue();
                } else {
                    return null;
                }
            } else {
                if (! oldMap.containsKey(key)) {
                    return null;
                }
                newMap = new FastCopyHashMap<K, V>(oldMap);
                oldValue = newMap.remove(key);
            }
            if (updater.compareAndSet(instance, oldMap, newMap)) {
                return oldValue;
            }
        }
    }

    public Map<K, V> get(final C subregistry) {
        return updater.get(subregistry);
    }

    public Map<K, V> getReadOnly(final C subregistry) {
        final Map<K, V> snapshot = updater.get(subregistry);
        return snapshot instanceof FastCopyHashMap ? Collections.unmodifiableMap(snapshot) : snapshot;
    }
}

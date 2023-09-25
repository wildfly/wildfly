/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.ee;

/**
 * Locates a value from the cache.
 * @author Paul Ferraro
 */
public interface Locator<K, V> {

    /**
     * Locates the value in the cache with the specified identifier.
     * @param id the cache entry identifier
     * @return the value of the cache entry, or null if not found.
     */
    V findValue(K id);

    /**
     * Returns the value for the specified key, if possible.
     * @param key a cache key
     * @return the value of the cache entry, or null if not found or unavailable.
     */
    default V tryValue(K key) {
        return this.findValue(key);
    }
}

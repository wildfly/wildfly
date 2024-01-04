/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.ee;

/**
 * Removes an entry from the cache
 * @author Paul Ferraro
 */
public interface Remover<K> {
    /**
     * Removes the specified entry from the cache.
     * @param id the cache entry identifier.
     * @return true, if the entry was removed.
     */
    boolean remove(K id);

    /**
     * Like {@link #remove(Object)}, but does not notify listeners.
     * @param id the cache entry identifier.
     * @return true, if the entry was removed.
     */
    default boolean purge(K id) {
        return this.remove(id);
    }
}

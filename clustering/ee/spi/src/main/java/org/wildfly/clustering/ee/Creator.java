package org.wildfly.clustering.ee;

/**
 * Creates a value in the cache.
 * @author Paul Ferraro
 */
public interface Creator<K, V, C> {

    /**
     * Creates a value in the cache, if it does not already exist.
     * @param id the cache entry identifier.
     * @parem context the creation context
     * @return the new value, or the existing value the cache entry already exists.
     */
    V createValue(K id, C context);
}

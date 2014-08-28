package org.wildfly.clustering.ee.infinispan;

/**
 * Creates a value in the cache.
 * @author Paul Ferraro
 */
public interface Creator<K, V> {

    /**
     * Creates a value in the cache, if it does not already exist.
     * @param id the cache entry identifier.
     * @return the new value, or the existing value the cache entry already exists.
     */
    V createValue(K id);
}

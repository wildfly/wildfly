package org.wildfly.clustering.ee;

import java.util.Map;

/**
 * Creates a mutator instance for a given cache entry.
 * @author Paul Ferraro
 */
public interface MutatorFactory<K, V> {
    /**
     * Creates a mutator for the specified cache entry.
     * @param entry a cache entry
     * @return a mutator
     */
    default Mutator createMutator(Map.Entry<K, V> entry) {
        return this.createMutator(entry.getKey(), entry.getValue());
    }

    /**
     * Creates a mutator for the specified cache entry.
     * @param key a cache key
     * @param value a cache value
     * @return a mutator
     */
    Mutator createMutator(K key, V value);
}

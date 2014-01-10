package org.jboss.as.clustering.infinispan.invoker;

import org.infinispan.Cache;

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

    /**
     * Reusable creation operation.
     */
    class CreateOperation<K, V> implements CacheInvoker.Operation<K, V, V> {
        private final K key;
        private final V value;

        public CreateOperation(K key, V value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public V invoke(Cache<K, V> cache) {
            return cache.putIfAbsent(this.key, this.value);
        }
    }
}

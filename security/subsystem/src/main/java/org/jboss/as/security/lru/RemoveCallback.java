package org.jboss.as.security.lru;

/**
 * Allows an LRU Cache to get a callback after a removal has occurred.
 *
 * @author Jason T. Greene
 */
public interface RemoveCallback<K, V> {
    void afterRemove(K key, V value);
}

package org.jboss.as.clustering.registry;

import java.util.Collection;

public interface RegistryCollector<K, V> {
    interface Listener<K, V> {
        void registryAdded(Registry<K, V> registry);
        
        void registryRemoved(Registry<K, V> registry);
    }

    void addListener(Listener<K, V> listener);
    void removeListener(Listener<K, V> listener);

    void add(Registry<K, V> registry);

    void remove(Registry<K, V> registry);
    
    Collection<Registry<K, V>> getRegistries();
}

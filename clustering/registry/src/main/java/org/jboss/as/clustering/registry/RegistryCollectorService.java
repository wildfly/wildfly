package org.jboss.as.clustering.registry;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

public class RegistryCollectorService<K, V> implements Service<RegistryCollector<K, V>>, RegistryCollector<K, V> {

    private final ConcurrentMap<String, Registry<K, V>> registries = new ConcurrentHashMap<String, Registry<K, V>>();
    private final Set<Listener<K, V>> listeners = new CopyOnWriteArraySet<Listener<K, V>>();

    @Override
    public RegistryCollector<K, V> getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    @Override
    public void add(Registry<K, V> registry) {
        if (this.registries.putIfAbsent(registry.getName(), registry) == null) {
            for (Listener<K, V> listener: this.listeners) {
                listener.registryAdded(registry);
            }
        }
    }

    @Override
    public void remove(Registry<K, V> registry) {
        if (this.registries.remove(registry.getName()) != null) {
            for (Listener<K, V> listener: this.listeners) {
                listener.registryRemoved(registry);
            }
        }
    }

    @Override
    public void addListener(Listener<K, V> listener) {
        this.listeners.add(listener);
    }

    @Override
    public void removeListener(Listener<K, V> listener) {
        this.listeners.remove(listener);
    }

    @Override
    public Collection<Registry<K, V>> getRegistries() {
        return Collections.unmodifiableCollection(this.registries.values());
    }

    @Override
    public void start(StartContext context) throws StartException {
    }

    @Override
    public void stop(StopContext context) {
    }
}

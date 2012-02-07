/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

/**
 * @author Paul Ferraro
 */
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

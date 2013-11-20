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
package org.jboss.as.ejb3.remote;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.registry.Registry;

/**
 * @author Paul Ferraro
 */
public class RegistryCollectorService<K, V> extends AbstractService<RegistryCollector<K, V>> implements RegistryCollector<K, V> {

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("ejb", "remoting", "connector", "client-mappings", "registries");

    private final ConcurrentMap<String, Registry<K, V>> registries = new ConcurrentHashMap<>();
    private final Set<Listener<K, V>> listeners = new CopyOnWriteArraySet<>();

    @Override
    public RegistryCollector<K, V> getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    @Override
    public void add(Registry<K, V> registry) {
        if (this.registries.putIfAbsent(registry.getGroup().getName(), registry) == null) {
            for (Listener<K, V> listener: this.listeners) {
                listener.registryAdded(registry);
            }
        }
    }

    @Override
    public void remove(Registry<K, V> registry) {
        if (this.registries.remove(registry.getGroup().getName()) != null) {
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
}

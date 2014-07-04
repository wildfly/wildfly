/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.server.registry;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;

import org.infinispan.Cache;
import org.infinispan.context.Flag;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryRemoved;
import org.infinispan.notifications.cachelistener.annotation.TopologyChanged;
import org.infinispan.notifications.cachelistener.event.CacheEntryModifiedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryRemovedEvent;
import org.infinispan.notifications.cachelistener.event.TopologyChangedEvent;
import org.infinispan.remoting.transport.Address;
import org.jboss.as.clustering.infinispan.invoker.CacheInvoker;
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.group.NodeFactory;
import org.wildfly.clustering.registry.Registry;
import org.wildfly.clustering.registry.RegistryEntryProvider;

/**
 * Clustered {@link Registry} backed by an Infinispan cache.
 * @author Paul Ferraro
 * @param <K> key type
 * @param <V> value type
 */
@org.infinispan.notifications.Listener
public class CacheRegistry<K, V> implements Registry<K, V> {

    private final List<Registry.Listener<K, V>> listeners = new CopyOnWriteArrayList<>();
    private final RegistryEntryProvider<K, V> provider;
    private final Cache<Node, Map.Entry<K, V>> cache;
    private final CacheInvoker invoker;
    private final Group group;
    private final NodeFactory<Address> factory;

    public CacheRegistry(CacheRegistryFactoryConfiguration<K, V> config, RegistryEntryProvider<K, V> provider) {
        this.cache = config.getCache();
        this.invoker = config.getCacheInvoker();
        this.group = config.getGroup();
        this.factory = config.getNodeFactory();
        this.provider = provider;
        this.getLocalEntry();
        this.cache.addListener(this);
    }

    @Override
    public void close() {
        this.cache.removeListener(this);
        this.listeners.clear();
        final Node node = this.getGroup().getLocalNode();
        Operation<Void> operation = new Operation<Void>() {
            @Override
            public Void invoke(Cache<Node, Entry<K, V>> cache) {
                cache.remove(node);
                return null;
            }
        };
        this.invoker.invoke(this.cache, operation, Flag.IGNORE_RETURN_VALUES);
    }

    @Override
    public void addListener(Registry.Listener<K, V> listener) {
        this.listeners.add(listener);
    }

    @Override
    public void removeListener(Registry.Listener<K, V> listener) {
        this.listeners.remove(listener);
    }

    @Override
    public Group getGroup() {
        return this.group;
    }

    @Override
    public Map<K, V> getEntries() {
        Map<K, V> map = new HashMap<>();
        for (Node node: this.cache.keySet()) {
            Map.Entry<K, V> entry = this.cache.get(node);
            if (entry != null) {
                map.put(entry.getKey(), entry.getValue());
            }
        }
        return Collections.unmodifiableMap(map);
    }

    @Override
    public Map.Entry<K, V> getEntry(Node node) {
        return this.cache.get(node);
    }

    @Override
    public Map.Entry<K, V> getLocalEntry() {
        K key = this.provider.getKey();
        if (key == null) return null;
        final Map.Entry<K, V> entry = new AbstractMap.SimpleImmutableEntry<>(key, this.provider.getValue());
        final Node node = this.getGroup().getLocalNode();
        Operation<Void> operation = new Operation<Void>() {
            @Override
            public Void invoke(Cache<Node, Entry<K, V>> cache) {
                cache.put(node, entry);
                return null;
            }
        };
        this.invoker.invoke(this.cache, operation, Flag.IGNORE_RETURN_VALUES);
        return entry;
    }

    @TopologyChanged
    public void topologyChanged(TopologyChangedEvent<Address, Node> event) {
        if (event.isPre()) return;
        List<Address> newAddresses = event.getConsistentHashAtEnd().getMembers();
        // Only run on the coordinator
        if (!newAddresses.get(0).equals(event.getCache().getCacheManager().getAddress())) return;

        Set<Address> addresses = new HashSet<>(event.getConsistentHashAtStart().getMembers());
        // Determine which nodes have left the cache view
        addresses.removeAll(newAddresses);
        final List<Node> nodes = new ArrayList<>(addresses.size());
        for (Address address: addresses) {
            nodes.add(this.factory.createNode(address));
        }
        Operation<Map<K, V>> operation = new Operation<Map<K, V>>() {
            @Override
            public Map<K, V> invoke(Cache<Node, Entry<K, V>> cache) {
                Map<K, V> removed = new HashMap<>();
                for (Node node: nodes) {
                    Map.Entry<K, V> old = cache.remove(node);
                    if (old != null) {
                        removed.put(old.getKey(), old.getValue());
                    }
                }
                return removed;
            }
        };
        Map<K, V> removed = this.invoker.invoke(this.cache, operation, Flag.FORCE_SYNCHRONOUS);
        if (!removed.isEmpty()) {
            for (Listener<K, V> listener: this.listeners) {
                listener.removedEntries(removed);
            }
        }
    }

    @CacheEntryModified
    public void modified(CacheEntryModifiedEvent<Node, Map.Entry<K, V>> event) {
        if (event.isOriginLocal() || event.isPre()) return;
        if (!this.listeners.isEmpty()) {
            Map.Entry<K, V> entry = event.getValue();
            if (entry != null) {
                Map<K, V> entries = Collections.singletonMap(entry.getKey(), entry.getValue());
                for (Listener<K, V> listener: this.listeners) {
                    if (event.isCreated()) {
                        listener.addedEntries(entries);
                    } else {
                        listener.updatedEntries(entries);
                    }
                }
            }
        }
    }

    @CacheEntryRemoved
    public void removed(CacheEntryRemovedEvent<Node, Map.Entry<K, V>> event) {
        if (event.isOriginLocal() || event.isPre()) return;
        Map.Entry<K, V> entry = event.getOldValue();
        if (entry != null) {
            Map<K, V> entries = Collections.singletonMap(entry.getKey(), entry.getValue());
            for (Listener<K, V> listener: this.listeners) {
                listener.removedEntries(entries);
            }
        }
    }

    abstract class Operation<R> implements CacheInvoker.Operation<Node, Map.Entry<K, V>, R> {
    }
}

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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import org.infinispan.Cache;
import org.infinispan.commons.CacheException;
import org.infinispan.context.Flag;
import org.infinispan.distribution.ch.ConsistentHash;
import org.infinispan.filter.KeyFilter;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryCreated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryRemoved;
import org.infinispan.notifications.cachelistener.annotation.TopologyChanged;
import org.infinispan.notifications.cachelistener.event.CacheEntryEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryRemovedEvent;
import org.infinispan.notifications.cachelistener.event.Event;
import org.infinispan.notifications.cachelistener.event.TopologyChangedEvent;
import org.infinispan.remoting.transport.Address;
import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.ee.Batcher;
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.group.NodeFactory;
import org.wildfly.clustering.registry.Registry;
import org.wildfly.clustering.registry.RegistryEntryProvider;
import org.wildfly.clustering.server.logging.ClusteringServerLogger;
import org.wildfly.clustering.service.concurrent.ServiceExecutor;
import org.wildfly.clustering.service.concurrent.StampedLockServiceExecutor;

/**
 * Clustered {@link Registry} backed by an Infinispan cache.
 * @author Paul Ferraro
 * @param <K> key type
 * @param <V> value type
 */
@org.infinispan.notifications.Listener(sync = false)
public class CacheRegistry<K, V> implements Registry<K, V>, KeyFilter<Object> {

    private final List<Registry.Listener<K, V>> listeners = new CopyOnWriteArrayList<>();
    private final Cache<Node, Map.Entry<K, V>> cache;
    private final Batcher<? extends Batch> batcher;
    private final Group group;
    private final NodeFactory<Address> factory;
    private final ServiceExecutor executor = new StampedLockServiceExecutor();
    private final Map.Entry<K, V> registryEntry;

    public CacheRegistry(CacheRegistryFactoryConfiguration<K, V> config, RegistryEntryProvider<K, V> provider) {
        this.cache = config.getCache();
        this.batcher = config.getBatcher();
        this.group = config.getGroup();
        this.factory = config.getNodeFactory();
        this.registryEntry = new AbstractMap.SimpleImmutableEntry<>(provider.getKey(), provider.getValue());
        this.populateRegistry();
        this.cache.addListener(this, new CacheRegistryFilter());
    }

    private void populateRegistry() {
        try (Batch batch = this.batcher.createBatch()) {
            this.cache.getAdvancedCache().withFlags(Flag.IGNORE_RETURN_VALUES).put(this.group.getLocalNode(), registryEntry);
        }
    }

    @Override
    public boolean accept(Object key) {
        return key instanceof Node;
    }

    @Override
    public void close() {
        this.executor.close(() -> {
            this.cache.removeListener(this);
            this.listeners.clear();
            final Node node = this.getGroup().getLocalNode();
            try (Batch batch = this.batcher.createBatch()) {
                // If this remove fails, the entry will be auto-removed on topology change by the new primary owner
                this.cache.getAdvancedCache().withFlags(Flag.IGNORE_RETURN_VALUES, Flag.FAIL_SILENTLY).remove(node);
            }
        });
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
        Set<Node> nodes = this.group.getNodes().stream().collect(Collectors.toSet());
        return this.cache.getAdvancedCache().getAll(nodes).values().stream().collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue()));
    }

    @Override
    public Map.Entry<K, V> getEntry(Node node) {
        return this.cache.get(node);
    }

    @TopologyChanged
    public void topologyChanged(TopologyChangedEvent<Node, Map.Entry<K, V>> event) {
        if (event.isPre()) return;

        this.executor.execute(() -> {
            ConsistentHash hash = event.getConsistentHashAtEnd();
            List<Address> members = hash.getMembers();
            Address localAddress = event.getCache().getCacheManager().getAddress();

            // Determine which nodes have left the cache view
            Set<Address> addresses = new HashSet<>(event.getConsistentHashAtStart().getMembers());
            addresses.removeAll(members);

            if (!addresses.isEmpty()) {
                // We're only interested in the entries for which we are the primary owner
                List<Node> nodes = addresses.stream().filter(address -> hash.locatePrimaryOwner(address).equals(localAddress)).map(address -> this.factory.createNode(address)).collect(Collectors.toList());

                if (!nodes.isEmpty()) {
                    Cache<Node, Map.Entry<K, V>> cache = event.getCache().getAdvancedCache().withFlags(Flag.FORCE_SYNCHRONOUS);
                    Map<K, V> removed = new HashMap<>();
                    try (Batch batch = this.batcher.createBatch()) {
                        for (Node node: nodes) {
                            Map.Entry<K, V> old = cache.remove(node);
                            if (old != null) {
                                removed.put(old.getKey(), old.getValue());
                            }
                        }
                    } catch (CacheException e) {
                        ClusteringServerLogger.ROOT_LOGGER.registryPurgeFailed(e, event.getCache().getCacheManager().getCacheManagerConfiguration().globalJmxStatistics().cacheManagerName(), event.getCache().getName(), nodes);
                    }
                    // Invoke listeners outside above tx context
                    if (!removed.isEmpty()) {
                        this.notifyListeners(Event.Type.CACHE_ENTRY_REMOVED, removed);
                    }
                }
            } else {
                // This is a merge after cluster split: re-populate the cache registry with lost registry entries
                if (!event.getConsistentHashAtStart().getMembers().contains(localAddress)) {
                    // If this node is not a member at merge start, its mapping is lost and needs to be recreated and listeners notified
                    this.populateRegistry();

                    // Invoke listeners outside above tx context
                    this.notifyListeners(Event.Type.CACHE_ENTRY_CREATED, registryEntry);
                }
            }
        });
    }

    @CacheEntryCreated
    @CacheEntryModified
    public void event(CacheEntryEvent<Node, Map.Entry<K, V>> event) {
        if (event.isOriginLocal() || event.isPre()) return;
        if (!this.listeners.isEmpty()) {
            Map.Entry<K, V> entry = event.getValue();
            if (entry != null) {
                this.notifyListeners(event.getType(), entry);
            }
        }
    }

    @CacheEntryRemoved
    public void removed(CacheEntryRemovedEvent<Node, Map.Entry<K, V>> event) {
        if (event.isOriginLocal() || event.isPre()) return;
        if (!this.listeners.isEmpty()) {
            Map.Entry<K, V> entry = event.getOldValue();
            // WFLY-4938 For some reason, the old value can be null
            if (entry != null) {
                this.notifyListeners(event.getType(), entry);
            }
        }
    }

    private void notifyListeners(Event.Type type, Map.Entry<K, V> entry) {
        this.notifyListeners(type, Collections.singletonMap(entry.getKey(), entry.getValue()));
    }

    private void notifyListeners(Event.Type type, Map<K, V> entries) {
        for (Listener<K, V> listener: this.listeners) {
            try {
                switch (type) {
                    case CACHE_ENTRY_CREATED: {
                        listener.addedEntries(entries);
                        break;
                    }
                    case CACHE_ENTRY_MODIFIED: {
                        listener.updatedEntries(entries);
                        break;
                    }
                    case CACHE_ENTRY_REMOVED: {
                        listener.removedEntries(entries);
                        break;
                    }
                    default: {
                        throw new IllegalStateException(type.name());
                    }
                }
            } catch (Throwable e) {
                ClusteringServerLogger.ROOT_LOGGER.registryListenerFailed(e, this.cache.getCacheManager().getCacheManagerConfiguration().globalJmxStatistics().cacheManagerName(), this.cache.getName(), type, entries);
            }
        }
    }
}

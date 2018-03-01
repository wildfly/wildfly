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

import java.security.PrivilegedAction;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

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
import org.jboss.as.clustering.logging.ClusteringLogger;
import org.jboss.threads.JBossThreadFactory;
import org.wildfly.clustering.Registration;
import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.ee.Batcher;
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.infinispan.spi.distribution.ConsistentHashLocality;
import org.wildfly.clustering.infinispan.spi.distribution.Locality;
import org.wildfly.clustering.registry.Registry;
import org.wildfly.clustering.registry.RegistryListener;
import org.wildfly.clustering.server.group.Group;
import org.wildfly.clustering.server.logging.ClusteringServerLogger;
import org.wildfly.clustering.service.concurrent.ClassLoaderThreadFactory;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Clustered {@link Registry} backed by an Infinispan cache.
 * @author Paul Ferraro
 * @param <K> key type
 * @param <V> value type
 */
@org.infinispan.notifications.Listener
public class CacheRegistry<K, V> implements Registry<K, V>, KeyFilter<Object> {

    private static ThreadFactory createThreadFactory(Class<?> targetClass) {
        PrivilegedAction<ThreadFactory> action = () -> new ClassLoaderThreadFactory(new JBossThreadFactory(new ThreadGroup(targetClass.getSimpleName()), Boolean.FALSE, null, "%G - %t", null, null), targetClass.getClassLoader());
        return WildFlySecurityManager.doUnchecked(action);
    }

    private final ExecutorService topologyChangeExecutor = Executors.newSingleThreadExecutor(createThreadFactory(this.getClass()));
    private final Map<RegistryListener<K, V>, ExecutorService> listeners = new ConcurrentHashMap<>();
    private final Cache<Node, Map.Entry<K, V>> cache;
    private final Batcher<? extends Batch> batcher;
    private final Group<Address> group;
    private final Runnable closeTask;
    private final Map.Entry<K, V> entry;

    public CacheRegistry(CacheRegistryConfiguration<K, V> config, Map.Entry<K, V> entry, Runnable closeTask) {
        this.cache = config.getCache();
        this.batcher = config.getBatcher();
        this.group = config.getGroup();
        this.closeTask = closeTask;
        this.entry = new AbstractMap.SimpleImmutableEntry<>(entry);
        this.populateRegistry();
        this.cache.addListener(this, new CacheRegistryFilter());
    }

    private void populateRegistry() {
        try (Batch batch = this.batcher.createBatch()) {
            this.cache.getAdvancedCache().withFlags(Flag.IGNORE_RETURN_VALUES).put(this.group.getLocalMember(), this.entry);
        }
    }

    @Override
    public boolean accept(Object key) {
        return key instanceof Node;
    }

    @Override
    public void close() {
        this.cache.removeListener(this);
        this.shutdown(this.topologyChangeExecutor);
        Node node = this.getGroup().getLocalMember();
        try (Batch batch = this.batcher.createBatch()) {
            // If this remove fails, the entry will be auto-removed on topology change by the new primary owner
            this.cache.getAdvancedCache().withFlags(Flag.IGNORE_RETURN_VALUES, Flag.FAIL_SILENTLY).remove(node);
        } catch (CacheException e) {
            ClusteringLogger.ROOT_LOGGER.warn(e.getLocalizedMessage(), e);
        } finally {
            // Cleanup any unregistered listeners
            for (ExecutorService executor : this.listeners.values()) {
                this.shutdown(executor);
            }
            this.listeners.clear();
            this.closeTask.run();
        }
    }

    @Override
    public Registration register(RegistryListener<K, V> listener) {
        this.listeners.computeIfAbsent(listener, key -> Executors.newSingleThreadExecutor(createThreadFactory(listener.getClass())));
        return () -> this.unregister(listener);
    }

    private void unregister(RegistryListener<K, V> listener) {
        ExecutorService executor = this.listeners.remove(listener);
        if (executor != null) {
            this.shutdown(executor);
        }
    }

    @Deprecated
    @Override
    public void removeListener(Registry.Listener<K, V> listener) {
        this.unregister(listener);
    }

    @Override
    public org.wildfly.clustering.group.Group getGroup() {
        return this.group;
    }

    @Override
    public Map<K, V> getEntries() {
        Set<Node> nodes = new HashSet<>(this.group.getMembership().getMembers());
        Map<K, V> result = new HashMap<>();
        try (Batch batch = this.batcher.createBatch()) {
            for (Map.Entry<K, V> entry : this.cache.getAdvancedCache().getAll(nodes).values()) {
                if (entry != null) {
                    result.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return result;
    }

    @Override
    public Map.Entry<K, V> getEntry(Node node) {
        try (Batch batch = this.batcher.createBatch()) {
            return this.cache.get(node);
        }
    }

    @TopologyChanged
    public void topologyChanged(TopologyChangedEvent<Node, Map.Entry<K, V>> event) {
        if (event.isPre()) return;

        ConsistentHash previousHash = event.getWriteConsistentHashAtStart();
        List<Address> previousMembers = previousHash.getMembers();
        ConsistentHash hash = event.getWriteConsistentHashAtEnd();
        List<Address> members = hash.getMembers();
        Address localAddress = event.getCache().getCacheManager().getAddress();

        // Determine which nodes have left the cache view
        Set<Address> addresses = new HashSet<>(previousMembers);
        addresses.removeAll(members);

        try {
            this.topologyChangeExecutor.submit(() -> {
                if (!addresses.isEmpty()) {
                    Locality locality = new ConsistentHashLocality(event.getCache(), hash);
                    // We're only interested in the entries for which we are the primary owner
                    List<Node> nodes = new ArrayList<>(addresses.size());
                    for (Address address : addresses) {
                        if (locality.isLocal(address)) {
                            nodes.add(this.group.createNode(address));
                        }
                    }

                    if (!nodes.isEmpty()) {
                        Cache<Node, Map.Entry<K, V>> cache = this.cache.getAdvancedCache().withFlags(Flag.FORCE_SYNCHRONOUS);
                        Map<K, V> removed = new HashMap<>();
                        try (Batch batch = this.batcher.createBatch()) {
                            for (Node node: nodes) {
                                Map.Entry<K, V> old = cache.remove(node);
                                if (old != null) {
                                    removed.put(old.getKey(), old.getValue());
                                }
                            }
                        } catch (CacheException e) {
                            ClusteringServerLogger.ROOT_LOGGER.registryPurgeFailed(e, this.cache.getCacheManager().toString(), this.cache.getName(), nodes);
                        }
                        // Invoke listeners outside above tx context
                        if (!removed.isEmpty()) {
                            this.notifyListeners(Event.Type.CACHE_ENTRY_REMOVED, removed);
                        }
                    }
                } else {
                    // This is a merge after cluster split: re-populate the cache registry with lost registry entries
                    if (!previousMembers.contains(localAddress)) {
                        // If this node is not a member at merge start, its mapping is lost and needs to be recreated and listeners notified
                        try {
                            this.populateRegistry();
                            // Local cache events do not trigger notifications
                            this.notifyListeners(Event.Type.CACHE_ENTRY_CREATED, this.entry);
                        } catch (CacheException e) {
                            ClusteringServerLogger.ROOT_LOGGER.failedToRestoreLocalRegistryEntry(e, this.cache.getCacheManager().toString(), this.cache.getName());
                        }
                    }
                }
            });
        } catch (RejectedExecutionException e) {
            // Executor was shutdown
        }
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
        for (Map.Entry<RegistryListener<K, V>, ExecutorService> entry: this.listeners.entrySet()) {
            RegistryListener<K, V> listener = entry.getKey();
            ExecutorService executor = entry.getValue();
            try {
                executor.submit(() -> {
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
                });
            } catch (RejectedExecutionException e) {
                // Executor was shutdown
            }
        }
    }

    private void shutdown(ExecutorService executor) {
        PrivilegedAction<List<Runnable>> action = () -> executor.shutdownNow();
        WildFlySecurityManager.doUnchecked(action);
        try {
            executor.awaitTermination(this.cache.getCacheConfiguration().transaction().cacheStopTimeout(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

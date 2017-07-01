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
package org.wildfly.clustering.server.group;

import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.infinispan.Cache;
import org.infinispan.distribution.DistributionManager;
import org.infinispan.notifications.cachelistener.annotation.TopologyChanged;
import org.infinispan.notifications.cachelistener.event.TopologyChangedEvent;
import org.infinispan.notifications.cachemanagerlistener.annotation.Merged;
import org.infinispan.notifications.cachemanagerlistener.annotation.ViewChanged;
import org.infinispan.notifications.cachemanagerlistener.event.ViewChangedEvent;
import org.infinispan.remoting.transport.Address;
import org.jboss.threads.JBossThreadFactory;
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.server.logging.ClusteringServerLogger;
import org.wildfly.clustering.service.concurrent.ClassLoaderThreadFactory;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * {@link Group} implementation based on the topology of a cache.
 * @author Paul Ferraro
 */
@org.infinispan.notifications.Listener
public class CacheGroup implements Group, AutoCloseable {

    private static ThreadFactory createThreadFactory(Class<?> targetClass) {
        PrivilegedAction<ThreadFactory> action = () -> new JBossThreadFactory(new ThreadGroup(targetClass.getSimpleName()), Boolean.FALSE, null, "%G - %t", null, null);
        return new ClassLoaderThreadFactory(WildFlySecurityManager.doUnchecked(action), targetClass.getClassLoader());
    }

    private final Map<Listener, ExecutorService> listeners = new ConcurrentHashMap<>();
    private final Cache<?, ?> cache;
    private final InfinispanNodeFactory factory;
    private final SortedMap<Integer, Boolean> views = Collections.synchronizedSortedMap(new TreeMap<>());

    public CacheGroup(CacheGroupConfiguration config) {
        this.cache = config.getCache();
        this.factory = config.getNodeFactory();
        this.cache.getCacheManager().addListener(this);
        this.cache.addListener(this);
    }

    @Override
    public void close() {
        this.cache.removeListener(this);
        this.cache.getCacheManager().removeListener(this);
        // Cleanup any unregistered listeners
        this.listeners.values().forEach(executor -> {
            PrivilegedAction<List<Runnable>> action = () -> executor.shutdownNow();
            WildFlySecurityManager.doUnchecked(action);
        });
        this.listeners.clear();
    }

    @Override
    public String getName() {
        return this.cache.getCacheManager().getClusterName();
    }

    @Override
    public boolean isCoordinator() {
        return this.cache.getCacheManager().getAddress().equals(this.getCoordinator());
    }

    @Override
    public Node getLocalNode() {
        return this.factory.createNode(this.cache.getCacheManager().getAddress());
    }

    @Override
    public Node getCoordinatorNode() {
        return this.factory.createNode(this.getCoordinator());
    }

    private Address getCoordinator() {
        DistributionManager dist = this.cache.getAdvancedCache().getDistributionManager();
        return (dist != null) ? dist.getConsistentHash().getMembers().get(0) : this.cache.getCacheManager().getCoordinator();
    }

    @Override
    public List<Node> getNodes() {
        List<Address> addresses = this.getAddresses();
        if (addresses == null) return Collections.singletonList(this.getLocalNode());
        List<Node> nodes = new ArrayList<>(addresses.size());
        for (Address address: addresses) {
            nodes.add(this.factory.createNode(address));
        }
        return nodes;
    }

    @Merged
    @ViewChanged
    public void viewChanged(ViewChangedEvent event) {
        // Record view status for use by @TopologyChanged event
        this.views.put(event.getViewId(), event.isMergeView());
    }

    @TopologyChanged
    public void topologyChanged(TopologyChangedEvent<?, ?> event) {
        if (event.isPre()) return;

        List<Address> oldAddresses = event.getConsistentHashAtStart().getMembers();
        List<Node> oldNodes = this.getNodes(oldAddresses);
        List<Address> newAddresses = event.getConsistentHashAtEnd().getMembers();
        List<Node> newNodes = this.getNodes(newAddresses);

        Set<Address> obsolete = new HashSet<>(oldAddresses);
        obsolete.removeAll(newAddresses);
        this.factory.invalidate(obsolete);

        int viewId = event.getCache().getCacheManager().getTransport().getViewId();
        Boolean status = this.views.get(viewId);
        boolean merged = (status != null) ? status.booleanValue() : false;
        this.listeners.forEach((listener, executor) -> {
            try {
                executor.submit(() -> {
                    try {
                        listener.membershipChanged(oldNodes, newNodes, merged);
                    } catch (Throwable e) {
                        ClusteringServerLogger.ROOT_LOGGER.warn(e.getLocalizedMessage(), e);
                    }
                });
            } catch (RejectedExecutionException e) {
                // Listener was unregistered
            }
        });
        // Purge obsolete views
        this.views.headMap(viewId).clear();
    }

    private List<Node> getNodes(List<Address> addresses) {
        List<Node> nodes = new ArrayList<>(addresses.size());
        for (Address address: addresses) {
            nodes.add(this.factory.createNode(address));
        }
        return nodes;
    }

    @Override
    public void addListener(Listener listener) {
        this.listeners.computeIfAbsent(listener, key -> Executors.newSingleThreadExecutor(createThreadFactory(listener.getClass())));
    }

    @Override
    public void removeListener(Listener listener) {
        ExecutorService executor = this.listeners.remove(listener);
        if (executor != null) {
            PrivilegedAction<List<Runnable>> action = () -> executor.shutdownNow();
            WildFlySecurityManager.doUnchecked(action);
            try {
                executor.awaitTermination(this.cache.getCacheConfiguration().transaction().cacheStopTimeout(), TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private List<Address> getAddresses() {
        DistributionManager dist = this.cache.getAdvancedCache().getDistributionManager();
        return (dist != null) ? dist.getConsistentHash().getMembers() : this.cache.getCacheManager().getMembers();
    }

    @Override
    public boolean isLocal() {
        return !this.cache.getCacheConfiguration().clustering().cacheMode().isClustered();
    }
}

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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.infinispan.Cache;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.TransportConfiguration;
import org.infinispan.distribution.DistributionManager;
import org.infinispan.notifications.cachelistener.annotation.TopologyChanged;
import org.infinispan.notifications.cachelistener.event.TopologyChangedEvent;
import org.infinispan.notifications.cachemanagerlistener.annotation.Merged;
import org.infinispan.notifications.cachemanagerlistener.annotation.ViewChanged;
import org.infinispan.notifications.cachemanagerlistener.event.ViewChangedEvent;
import org.infinispan.remoting.transport.Address;
import org.infinispan.remoting.transport.LocalModeAddress;
import org.infinispan.remoting.transport.Transport;
import org.infinispan.remoting.transport.jgroups.JGroupsAddress;
import org.infinispan.remoting.transport.jgroups.JGroupsAddressCache;
import org.jboss.threads.JBossThreadFactory;
import org.wildfly.clustering.Registration;
import org.wildfly.clustering.group.GroupListener;
import org.wildfly.clustering.group.Membership;
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.server.logging.ClusteringServerLogger;
import org.wildfly.clustering.service.concurrent.ClassLoaderThreadFactory;
import org.wildfly.clustering.spi.NodeFactory;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * {@link Group} implementation based on the topology of a cache.
 * @author Paul Ferraro
 */
@org.infinispan.notifications.Listener
public class CacheGroup implements Group<Address>, AutoCloseable {

    private static ThreadFactory createThreadFactory(Class<?> targetClass) {
        PrivilegedAction<ThreadFactory> action = () -> new JBossThreadFactory(new ThreadGroup(targetClass.getSimpleName()), Boolean.FALSE, null, "%G - %t", null, null);
        return new ClassLoaderThreadFactory(WildFlySecurityManager.doUnchecked(action), targetClass.getClassLoader());
    }

    private final Map<GroupListener, ExecutorService> listeners = new ConcurrentHashMap<>();
    private final Cache<?, ?> cache;
    private final NodeFactory<org.jgroups.Address> nodeFactory;
    private final SortedMap<Integer, Boolean> views = Collections.synchronizedSortedMap(new TreeMap<>());

    public CacheGroup(CacheGroupConfiguration config) {
        this.cache = config.getCache();
        this.nodeFactory = config.getMemberFactory();
        this.cache.getCacheManager().addListener(this);
        this.cache.addListener(this);
    }

    @Override
    public void close() {
        this.cache.removeListener(this);
        this.cache.getCacheManager().removeListener(this);
        // Cleanup any unregistered listeners
        for (ExecutorService executor : this.listeners.values()) {
            PrivilegedAction<List<Runnable>> action = () -> executor.shutdownNow();
            WildFlySecurityManager.doUnchecked(action);
        }
        this.listeners.clear();
    }

    @Override
    public String getName() {
        GlobalConfiguration global = this.cache.getCacheManager().getCacheManagerConfiguration();
        TransportConfiguration transport = global.transport();
        return transport.transport() != null ? transport.clusterName() : global.globalJmxStatistics().cacheManagerName();
    }

    @Override
    public Node getLocalMember() {
        return this.createNode(this.cache.getCacheManager().getAddress());
    }

    @Override
    public Membership getMembership() {
        if (this.isSingleton()) {
            return new SingletonMembership(this.getLocalMember());
        }
        Transport transport = this.cache.getCacheManager().getTransport();
        DistributionManager dist = this.cache.getAdvancedCache().getDistributionManager();
        return (dist != null) ? new CacheMembership(transport.getAddress(), dist.getCacheTopology(), this) : new CacheMembership(transport, this);
    }

    @Override
    public boolean isSingleton() {
        return this.cache.getCacheManager().getTransport() == null;
    }

    @Override
    public Node createNode(Address address) {
        return this.nodeFactory.createNode(toJGroupsAddress(address));
    }

    @Override
    public Address getAddress(Node node) {
        return (node instanceof AddressableNode) ? JGroupsAddressCache.fromJGroupsAddress(((AddressableNode) node).getAddress()) : LocalModeAddress.INSTANCE;
    }

    private static org.jgroups.Address toJGroupsAddress(Address address) {
        if ((address == null) || (address == LocalModeAddress.INSTANCE)) return null;
        if (address instanceof JGroupsAddress) {
            JGroupsAddress jgroupsAddress = (JGroupsAddress) address;
            return jgroupsAddress.getJGroupsAddress();
        }
        throw new IllegalArgumentException(address.toString());
    }

    @Merged
    @ViewChanged
    public void viewChanged(ViewChangedEvent event) {
        if (this.cache.getAdvancedCache().getDistributionManager() != null) {
            // Record view status for use by @TopologyChanged event
            this.views.put(event.getViewId(), event.isMergeView());
        } else if (!this.listeners.isEmpty()) {
            Membership previousMembership = new CacheMembership(event.getLocalAddress(), event.getOldMembers(), this);
            Membership membership = new CacheMembership(event.getLocalAddress(), event.getNewMembers(), this);
            for (Map.Entry<GroupListener, ExecutorService> entry : this.listeners.entrySet()) {
                GroupListener listener = entry.getKey();
                ExecutorService executor = entry.getValue();
                try {
                    executor.submit(() -> {
                        try {
                            listener.membershipChanged(previousMembership, membership, event.isMergeView());
                        } catch (Throwable e) {
                            ClusteringServerLogger.ROOT_LOGGER.warn(e.getLocalizedMessage(), e);
                        }
                    });
                } catch (RejectedExecutionException e) {
                    // Listener was unregistered
                }
            }
        }
    }

    @TopologyChanged
    public void topologyChanged(TopologyChangedEvent<?, ?> event) {
        if (event.isPre()) return;

        int viewId = event.getCache().getCacheManager().getTransport().getViewId();
        if (!this.listeners.isEmpty()) {
            Address localAddress = event.getCache().getCacheManager().getAddress();
            Membership previousMembership = new CacheMembership(localAddress, event.getWriteConsistentHashAtStart(), this);
            Membership membership = new CacheMembership(localAddress, event.getWriteConsistentHashAtEnd(), this);
            Boolean status = this.views.get(viewId);
            boolean merged = (status != null) ? status.booleanValue() : false;
            for (Map.Entry<GroupListener, ExecutorService> entry : this.listeners.entrySet()) {
                GroupListener listener = entry.getKey();
                ExecutorService executor = entry.getValue();
                try {
                    executor.submit(() -> {
                        try {
                            listener.membershipChanged(previousMembership, membership, merged);
                        } catch (Throwable e) {
                            ClusteringServerLogger.ROOT_LOGGER.warn(e.getLocalizedMessage(), e);
                        }
                    });
                } catch (RejectedExecutionException e) {
                    // Listener was unregistered
                }
            }
        }
        // Purge obsolete views
        this.views.headMap(viewId).clear();
    }

    @Override
    public Registration register(GroupListener listener) {
        this.listeners.computeIfAbsent(listener, key -> Executors.newSingleThreadExecutor(createThreadFactory(listener.getClass())));
        return () -> this.unregister(listener);
    }

    private void unregister(GroupListener listener) {
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

    @Deprecated
    @Override
    public void removeListener(Listener listener) {
        this.unregister(listener);
    }
}

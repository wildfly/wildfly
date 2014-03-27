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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import org.infinispan.Cache;
import org.infinispan.distribution.DistributionManager;
import org.infinispan.notifications.cachelistener.annotation.TopologyChanged;
import org.infinispan.notifications.cachelistener.event.TopologyChangedEvent;
import org.infinispan.remoting.transport.Address;
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.group.Node;

/**
 * {@link Cache} based {@link Group} implementation
 * @author Paul Ferraro
 */
@org.infinispan.notifications.Listener(sync = false)
public class CacheGroup implements Group, AutoCloseable {

    private final List<Listener> listeners = new CopyOnWriteArrayList<>();
    private final Cache<?, ?> cache;
    private final CacheNodeFactory factory;

    public CacheGroup(CacheGroupConfiguration config) {
        this.cache = config.getCache();
        this.factory = config.getNodeFactory();
        this.cache.addListener(this);
    }

    @Override
    public void close() {
        this.cache.removeListener(this);
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
        List<Node> nodes = new ArrayList<>(addresses.size());
        for (Address address: addresses) {
            nodes.add(this.factory.createNode(address));
        }
        return nodes;
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

        for (Listener listener: this.listeners) {
            listener.membershipChanged(oldNodes, newNodes, false);
        }
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
        this.listeners.add(listener);
    }

    @Override
    public void removeListener(Listener listener) {
        this.listeners.remove(listener);
    }

    private List<Address> getAddresses() {
        DistributionManager dist = this.cache.getAdvancedCache().getDistributionManager();
        return (dist != null) ? dist.getConsistentHash().getMembers() : this.cache.getCacheManager().getMembers();
    }
}

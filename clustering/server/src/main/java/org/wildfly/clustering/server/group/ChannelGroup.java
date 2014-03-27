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

import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.notifications.cachemanagerlistener.annotation.ViewChanged;
import org.infinispan.notifications.cachemanagerlistener.event.ViewChangedEvent;
import org.infinispan.remoting.transport.Address;
import org.infinispan.remoting.transport.jgroups.JGroupsAddress;
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.group.Node;

/**
 * {@link Channel} based {@link Group} implementation.
 * @author Paul Ferraro
 */
@org.infinispan.notifications.Listener(sync = false)
public class ChannelGroup implements Group, AutoCloseable {

    private final List<Listener> listeners = new CopyOnWriteArrayList<>();
    private final EmbeddedCacheManager manager;
    private final ChannelNodeFactory factory;

    public ChannelGroup(ChannelGroupConfiguration config) {
        this.manager = config.getCacheContainer();
        this.factory = config.getNodeFactory();
        this.manager.addListener(this);
    }

    @Override
    public void close() {
        this.manager.removeListener(this);
    }

    @Override
    public String getName() {
        return this.manager.getClusterName();
    }

    @Override
    public boolean isCoordinator() {
        return this.manager.isCoordinator();
    }

    @Override
    public Node getLocalNode() {
        return this.factory.createNode(toJGroupsAddress(this.manager.getAddress()));
    }

    @Override
    public Node getCoordinatorNode() {
        return this.factory.createNode(toJGroupsAddress(this.manager.getCoordinator()));
    }

    @Override
    public List<Node> getNodes() {
        List<Address> addresses = this.manager.getMembers();
        List<Node> nodes = new ArrayList<>(addresses.size());
        for (Address address: addresses) {
            nodes.add(this.factory.createNode(toJGroupsAddress(address)));
        }
        return nodes;
    }

    @ViewChanged
    public void viewChanged(ViewChangedEvent event) {

        List<Address> oldAddresses = event.getOldMembers();
        List<Node> oldNodes = this.getNodes(oldAddresses);
        List<Address> newAddresses = event.getNewMembers();
        List<Node> newNodes = this.getNodes(newAddresses);

        Set<Address> members = new HashSet<>(newAddresses);
        List<org.jgroups.Address> obsolete = new ArrayList<>(oldAddresses.size());
        for (Address address: oldAddresses) {
            if (!members.contains(address)) {
                obsolete.add(toJGroupsAddress(address));
            }
        }
        this.factory.invalidate(obsolete);

        for (Listener listener: this.listeners) {
            listener.membershipChanged(oldNodes, newNodes, false);
        }
    }

    @Override
    public void addListener(Listener listener) {
        this.listeners.add(listener);
    }

    @Override
    public void removeListener(Listener listener) {
        this.listeners.remove(listener);
    }

    private List<Node> getNodes(List<Address> addresses) {
        List<Node> nodes = new ArrayList<>(addresses.size());
        for (Address address: addresses) {
            nodes.add(this.factory.createNode(toJGroupsAddress(address)));
        }
        return nodes;
    }

    private static org.jgroups.Address toJGroupsAddress(Address address) {
        if (address instanceof JGroupsAddress) {
            JGroupsAddress jgroupsAddress = (JGroupsAddress) address;
            return jgroupsAddress.getJGroupsAddress();
        }
        throw new IllegalArgumentException(address.toString());
    }
}

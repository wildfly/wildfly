/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.server.infinispan.dispatcher;

import java.util.ArrayList;
import java.util.List;

import org.jgroups.Address;
import org.jgroups.View;
import org.wildfly.clustering.group.Membership;
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.server.NodeFactory;

/**
 * A group membership based on a JGroups view.
 * @author Paul Ferraro
 */
public class ViewMembership implements Membership {

    private final Address localAddress;
    private final View view;
    private final NodeFactory<Address> factory;

    public ViewMembership(Address localAddress, View view, NodeFactory<Address> factory) {
        this.localAddress = localAddress;
        this.view = view;
        this.factory = factory;
    }

    @Override
    public boolean isCoordinator() {
        return this.localAddress.equals(this.view.getCoord());
    }

    @Override
    public Node getCoordinator() {
        return this.factory.createNode(this.view.getCoord());
    }

    @Override
    public List<Node> getMembers() {
        List<Node> members = new ArrayList<>(this.view.size());
        for (Address address : this.view.getMembersRaw()) {
            Node member = this.factory.createNode(address);
            if (member != null) {
                members.add(member);
            }
        }
        return members;
    }

    @Override
    public int hashCode() {
        return this.view.hashCode();
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof ViewMembership)) return false;
        ViewMembership membership = (ViewMembership) object;
        return this.view.equals(membership.view);
    }

    @Override
    public String toString() {
        return this.view.toString();
    }
}

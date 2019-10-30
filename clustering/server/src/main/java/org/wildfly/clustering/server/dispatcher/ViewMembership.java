/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.server.dispatcher;

import java.util.ArrayList;
import java.util.List;

import org.jgroups.Address;
import org.jgroups.View;
import org.wildfly.clustering.group.Membership;
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.spi.NodeFactory;

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
            members.add(this.factory.createNode(address));
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

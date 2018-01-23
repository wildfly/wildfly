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

package org.wildfly.clustering.group;

import java.util.List;

/**
 * Encapsulates an immutable membership of a group.
 * @author Paul Ferraro
 */
public interface Membership {
    /**
     * Indicates whether or not the local node is the coordinator of this group membership.
     * Semantically equivalent to:
     * {@code group.getLocalNode().equals(#getCoordinator())}
     *
     * @return true, if we are the group membership coordinator, false otherwise
     */
    boolean isCoordinator();

    /**
     * Returns the coordinator node of this group membership.
     * All nodes of this membership will always agree on which node is the coordinator.
     *
     * @return the group coordinator node
     */
    Node getCoordinator();

    /**
     * Returns the nodes that comprise this group membership.
     * The membership order will be consistent on each node in the group.
     *
     * @return a list of nodes ordered by descending age.
     */
    List<Node> getMembers();
}

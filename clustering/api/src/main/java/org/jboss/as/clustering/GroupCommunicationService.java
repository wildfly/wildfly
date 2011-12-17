/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.clustering;

import java.util.List;

/**
 * Abstraction of a server that provides group communication services to a set of nodes that share a common group communication
 * infrastructure. This is a base interface that provides common methods expected to be used by subinterfaces that provide more
 * useful services.
 *
 * @author Brian Stansberry
 *
 * @version $Revision: 104233 $
 */
public interface GroupCommunicationService {
    /**
     * Gets the object that represents this node in the current group.
     *
     * @return ClusterNode containing the current node name
     */
    ClusterNode getClusterNode();

    /**
     * Return the name of this node in the current group. The name will be the String returned by
     * <code>getClusterNode().getName()</code>.
     *
     * @return The node name
     *
     * @see #getClusterNode()
     */
    String getNodeName();

    /**
     * The name of the group with which communication occurs.
     *
     * @return Name of the current group
     */
    String getGroupName();

    /**
     * Gets the member nodes that comprise the current group membership.
     *
     * @return An array of ClusterNode listing the current members of the group. This array will be in the same order in all
     *         nodes in the cluster that have received the current membership view.
     */
    List<ClusterNode> getClusterNodes();

    /**
     * Identifier for the current group topology. Each time the group topology changes, a new view is computed. A view is a list
     * of members, the first member being the coordinator of the view. Each view also has a distinct identifier.
     *
     * @return the identifier of the current view
     */
    long getCurrentViewId();

    /**
     * Gets whether this GroupCommunicationService is logically consistent with another service; e.g. is using common group
     * communication infrastructure.
     *
     * @param other the other GroupCommunicationService. Cannot be <code>null</code>.
     *
     * @return <code>true</code> if an application can use this service and <code>other</code> together to interact with the
     *         same set of nodes.
     */
    boolean isConsistentWith(GroupCommunicationService other);

    /**
     * Indicates whether this node is the group coordinator.
     * @return true if this node is the coordinator, false otherwise
     */
    boolean isCoordinator();
}
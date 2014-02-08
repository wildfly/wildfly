/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
 * Represents a groups of nodes.
 *
 * @author Paul Ferraro
 */
public interface Group {

    /**
     * Listener for membership changes.
     */
    interface Listener {
        /**
         * Indicates that the membership of the group has changed.
         *
         * @param previousMembers previous group members
         * @param members         new group members
         * @param merged          indicates whether the membership change is the result of a merge view
         */
        void membershipChanged(List<Node> previousMembers, List<Node> members, boolean merged);
    }

    /**
     * Registers a membership listener for the group.
     *
     * @param listener listener to be added
     */
    void addListener(Listener listener);

    /**
     * Removes a registered listener from the group.
     *
     * @param listener listener to be removed
     */
    void removeListener(Listener listener);

    /**
     * Returns the name of this group.
     *
     * @return the group name
     */
    String getName();

    /**
     * Indicates whether or not we are the group coordinator.
     *
     * @return true, if we are the group coordinator, false otherwise
     */
    boolean isCoordinator();

    /**
     * Returns the local node.
     *
     * @return the local node
     */
    Node getLocalNode();

    /**
     * Returns the group coordinator node.
     *
     * @return the group coordinator node
     */
    Node getCoordinatorNode();

    /**
     * Returns the list of nodes that are members of this group.
     *
     * @return a list of nodes
     */
    List<Node> getNodes();
}

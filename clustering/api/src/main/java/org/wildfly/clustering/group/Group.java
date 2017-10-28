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

import org.wildfly.clustering.Registrar;

/**
 * Represents a groups of nodes.
 *
 * @author Paul Ferraro
 */
public interface Group extends Registrar<GroupListener> {

    /**
     * @deprecated Replaced by {@link GroupListener}.
     */
    @Deprecated interface Listener extends GroupListener {
    }

    /**
     * Registers a membership listener for the group.
     *
     * @param listener listener to be added
     * @deprecated Replaced by {@link #register(GroupListener)}.
     */
    @Deprecated default void addListener(Listener listener) {
        this.register(listener);
    }

    /**
     * Removes a registered listener from the group.
     *
     * @param listener listener to be removed
     * @deprecated Replaced by {@link org.wildfly.clustering.Registration#close()}
     */
    @Deprecated void removeListener(Listener listener);

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

    /**
     * Indicates whether this is a local group.  A local group only ever contains a single member.
     * @return true, if this is a local group, false otherwise.
     */
    boolean isLocal();
}

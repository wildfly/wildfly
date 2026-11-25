/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.group;

import java.util.List;

/**
 * Encapsulates an immutable membership of a group.
 * @author Paul Ferraro
 * @deprecated Replaced by {@link org.wildfly.clustering.server.GroupMembership}.
 */
@Deprecated(forRemoval = true)
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

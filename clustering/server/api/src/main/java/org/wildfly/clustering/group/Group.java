/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.group;

import org.wildfly.clustering.Registrar;

/**
 * Represents a groups of nodes.
 *
 * @author Paul Ferraro
 * @deprecated Replaced by {@link org.wildfly.clustering.server.Group}.
 */
@Deprecated(forRemoval = true)
public interface Group extends Registrar<GroupListener> {

    /**
     * Returns the logical name of this group.
     *
     * @return the group name
     */
    String getName();

    /**
     * Returns the local member.
     *
     * @return the local member
     */
    Node getLocalMember();

    /**
     * Gets the current membership of this group
     * @return the group membership
     */
    Membership getMembership();

    /**
     * Indicates whether or not this is a singleton group.  The membership of a singleton group contains only the local member and never changes.
     * @return true, if this is a singleton group, false otherwise.
     */
    boolean isSingleton();
}

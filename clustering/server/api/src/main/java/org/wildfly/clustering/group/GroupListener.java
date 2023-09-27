/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.group;

/**
 * Listener for {@link Group} membership changes.
 * @author Paul Ferraro
 */
public interface GroupListener {
    /**
     * Indicates that the membership of the group has changed.
     *
     * @param previousMembership previous group membership
     * @param membership new group membership
     * @param merged indicates whether the membership change is the result of a merge view
     */
    void membershipChanged(Membership previousMembership, Membership membership, boolean merged);
}

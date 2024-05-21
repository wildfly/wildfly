/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.singleton;

import java.util.List;

import org.wildfly.clustering.group.Node;

/**
 * Used by a singleton service to elect the primary node from among the list of nodes that can provide the given service.
 * @author Paul Ferraro
 * @deprecated Replaced by {@link org.wildfly.clustering.singleton.election.SingletonElectionPolicy}.
 */
@Deprecated(forRemoval = true)
public interface SingletonElectionPolicy {
    /**
     * Elect a single member from the specified list of candidate members.
     * @param members a list of candidate members.
     * @return the elected member
     */
    Node elect(List<Node> candidates);
}

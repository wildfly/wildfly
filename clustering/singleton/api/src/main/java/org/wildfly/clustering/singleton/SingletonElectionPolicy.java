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
 */
public interface SingletonElectionPolicy {
    /**
     * Elect a single node from the specified list of candidate nodes.
     * @param nodes a list of candidate nodes.
     * @return the elected node
     */
    Node elect(List<Node> nodes);
}

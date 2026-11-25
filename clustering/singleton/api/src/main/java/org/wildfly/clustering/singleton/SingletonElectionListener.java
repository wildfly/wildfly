/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.singleton;

import java.util.List;

import org.wildfly.clustering.group.Node;

/**
 * Listener for singleton election results.
 * @author Paul Ferraro
 * @deprecated Replaced by {@link org.wildfly.clustering.singleton.election.SingletonElectionListener}
 */
@Deprecated(forRemoval = true)
public interface SingletonElectionListener {
    /**
     * Triggered when a singleton election completes, electing the specified member from the specified list of candidates.
     * @param candidateMembers the list of candidate members
     * @param electedMember the elected primary provider of a singleton service
     */
    void elected(List<Node> candidateMembers, Node electedMember);
}

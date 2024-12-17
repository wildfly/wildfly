/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.singleton.election;

import java.util.List;

import org.wildfly.clustering.server.GroupMember;

/**
 * Listener for singleton election results.
 * @author Paul Ferraro
 */
public interface SingletonElectionListener {
    /**
     * Triggered when a singleton election completes, electing the specified member from the specified list of candidates.
     * @param candidateMembers the list of candidate members
     * @param electedMember the elected primary provider of a singleton service
     */
    void elected(List<GroupMember> candidateMembers, GroupMember electedMember);
}

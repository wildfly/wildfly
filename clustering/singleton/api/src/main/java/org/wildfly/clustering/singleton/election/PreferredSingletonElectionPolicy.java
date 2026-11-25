/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.singleton.election;

import java.util.Arrays;
import java.util.List;

import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.singleton.SingletonElectionPolicy;

/**
 * An election policy that always elects a preferred node, and defers to a default policy
 * if the preferred node is not a candidate.  The means of specifying the preferred node is
 * the responsibility of the extending class.
 * @author Paul Ferraro
 * @deprecated Replaced by {@link org.wildfly.clustering.singleton.election.SingletonElectionPolicy#prefer(List)}.
 */
@Deprecated(forRemoval = true)
public class PreferredSingletonElectionPolicy implements SingletonElectionPolicy {
    private final List<Preference> preferences;
    private final SingletonElectionPolicy policy;

    public PreferredSingletonElectionPolicy(SingletonElectionPolicy policy, Preference... preferences) {
        this(policy, Arrays.asList(preferences));
    }

    public PreferredSingletonElectionPolicy(SingletonElectionPolicy policy, List<Preference> preferences) {
        this.policy = policy;
        this.preferences = preferences;
    }

    @Override
    public Node elect(List<Node> candidates) {
        for (Preference preference: this.preferences) {
            for (Node candidate: candidates) {
                if (preference.preferred(candidate)) {
                    return candidate;
                }
            }
        }
        return this.policy.elect(candidates);
    }
}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.singleton.compat;

import java.util.List;
import java.util.stream.Collectors;

import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.server.GroupMember;
import org.wildfly.clustering.singleton.election.SingletonElectionListener;
import org.wildfly.clustering.singleton.election.SingletonElectionPolicy;

/**
 * Compatibility {@link org.wildfly.clustering.singleton.SingletonServiceBuilder} extension that adapts any legacy election listener/policy.
 * @author Paul Ferraro
 */
@Deprecated
public interface SingletonServiceBuilder<T> extends org.wildfly.clustering.singleton.SingletonServiceBuilder<T> {

    @Override
    default SingletonServiceBuilder<T> electionPolicy(org.wildfly.clustering.singleton.SingletonElectionPolicy policy) {
        return this.withElectionPolicy(new SingletonElectionPolicy() {
            @Override
            public GroupMember elect(List<GroupMember> candidates) {
                List<org.wildfly.clustering.group.Node> nodes = candidates.stream().map(LegacyMember::new).collect(Collectors.toUnmodifiableList());
                org.wildfly.clustering.group.Node result = policy.elect(nodes);
                return (result != null) ? candidates.get(nodes.indexOf(result)) : null;
            }
        });
    }

    @Override
    default SingletonServiceBuilder<T> electionListener(org.wildfly.clustering.singleton.SingletonElectionListener listener) {
        return this.withElectionListener(new SingletonElectionListener() {
            @Override
            public void elected(List<GroupMember> candidateMembers, GroupMember electedMember) {
                List<org.wildfly.clustering.group.Node> candidates = candidateMembers.stream().map(LegacyMember::new).collect(Collectors.toUnmodifiableList());
                listener.elected(candidates, (electedMember != null) ? candidates.get(candidateMembers.indexOf(electedMember)) : null);
            }
        });
    }

    SingletonServiceBuilder<T> withElectionPolicy(SingletonElectionPolicy policy);

    SingletonServiceBuilder<T> withElectionListener(SingletonElectionListener listener);

    @Override
    SingletonServiceBuilder<T> requireQuorum(int quorum);

    @Override
    org.wildfly.clustering.singleton.service.SingletonServiceBuilder<T> build(ServiceTarget target);
}

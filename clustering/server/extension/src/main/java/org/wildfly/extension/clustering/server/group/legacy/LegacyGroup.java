/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.server.group.legacy;

import java.util.List;
import java.util.stream.Collectors;

import org.wildfly.clustering.group.GroupListener;
import org.wildfly.clustering.group.Membership;
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.server.GroupMembership;
import org.wildfly.clustering.server.GroupMembershipEvent;
import org.wildfly.clustering.server.GroupMembershipListener;
import org.wildfly.clustering.server.GroupMembershipMergeEvent;
import org.wildfly.clustering.server.Registration;
import org.wildfly.clustering.server.group.Group;
import org.wildfly.clustering.server.group.GroupMember;

/**
 * @author Paul Ferraro
 */
@Deprecated
public interface LegacyGroup<A extends Comparable<A>, M extends GroupMember<A>> extends org.wildfly.clustering.group.Group {

    Group<A, M> unwrap();

    Node wrap(M member);

    M unwrap(Node node);

    default Membership wrap(GroupMembership<M> membership) {
        return new Membership() {
            @Override
            public Node getCoordinator() {
                return LegacyGroup.this.wrap(membership.getCoordinator());
            }

            @Override
            public List<Node> getMembers() {
                return membership.getMembers().stream().map(LegacyGroup.this::wrap).collect(Collectors.toList());
            }

            @Override
            public boolean isCoordinator() {
                return membership.getCoordinator().equals(LegacyGroup.this.unwrap().getLocalMember());
            }
        };
    }

    @Override
    default String getName() {
        return this.unwrap().getName();
    }

    @Override
    default Node getLocalMember() {
        return this.wrap(this.unwrap().getLocalMember());
    }

    @Override
    default Membership getMembership() {
        return this.wrap(this.unwrap().getMembership());
    }

    @Override
    default boolean isSingleton() {
        return this.unwrap().isSingleton();
    }

    @Override
    default org.wildfly.clustering.Registration register(GroupListener listener) {
        Registration registration = this.unwrap().register(new GroupMembershipListener<>() {
            @Override
            public void updated(GroupMembershipEvent<M> event) {
                listener.membershipChanged(LegacyGroup.this.wrap(event.getPreviousMembership()), LegacyGroup.this.wrap(event.getCurrentMembership()), false);
            }

            @Override
            public void merged(GroupMembershipMergeEvent<M> event) {
                listener.membershipChanged(LegacyGroup.this.wrap(event.getPreviousMembership()), LegacyGroup.this.wrap(event.getCurrentMembership()), true);
            }
        });
        return registration::close;
    }
}

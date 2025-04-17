/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.singleton.election;

import java.util.List;
import java.util.Random;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.wildfly.clustering.server.GroupMember;
import org.wildfly.service.descriptor.UnaryServiceDescriptor;

/**
 * Elects a primary provider of a singleton service from a list of candidates.
 * @author Paul Ferraro
 */
public interface SingletonElectionPolicy {
    UnaryServiceDescriptor<SingletonElectionPolicy> SERVICE_DESCRIPTOR = UnaryServiceDescriptor.of("org.wildfly.clustering.singleton-policy.election", SingletonElectionPolicy.class);

    /**
     * Elect a single member from the specified list of candidate members.
     * @param candidates a list of candidate members.
     * @return the elected member
     */
    GroupMember elect(List<GroupMember> candidates);

    default SingletonElectionPolicy prefer(List<Predicate<GroupMember>> preferences) {
        return !preferences.isEmpty() ? new SingletonElectionPolicy() {
            @Override
            public GroupMember elect(List<GroupMember> candidates) {
                for (Predicate<GroupMember> preference : preferences) {
                    List<GroupMember> preferred = candidates.stream().filter(preference).collect(Collectors.toUnmodifiableList());
                    if (!preferred.isEmpty()) {
                        return SingletonElectionPolicy.this.elect(preferred);
                    }
                }
                return SingletonElectionPolicy.this.elect(candidates);
            }
        } : this;
    }

    static SingletonElectionPolicy random() {
        Random random = new Random();
        return new SingletonElectionPolicy() {
            @Override
            public GroupMember elect(List<GroupMember> candidates) {
                int size = candidates.size();
                return (size > 0) ? candidates.get(random.nextInt(size)) : null;
            }
        };
    }

    static SingletonElectionPolicy oldest() {
        return position(0);
    }

    static SingletonElectionPolicy youngest() {
        return position(-1);
    }

    static SingletonElectionPolicy position(int position) {
        return new SingletonElectionPolicy() {
            @Override
            public GroupMember elect(List<GroupMember> candidates) {
                int size = candidates.size();
                return (size > 0) ? candidates.get(((position % size) + size) % size) : null;
            }
        };
    }
}

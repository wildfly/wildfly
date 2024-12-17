/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.singleton.election;

import java.util.List;
import java.util.Random;

import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.singleton.SingletonElectionPolicy;

/**
 * {@link SingletonElectionPolicy} that elects a random member.
 * @author Paul Ferraro
 * @deprecated Replaced by {@link org.wildfly.clustering.singleton.election.SingletonElectionPolicy#random()}.
 */
@Deprecated(forRemoval = true)
public class RandomSingletonElectionPolicy implements SingletonElectionPolicy {

    private final Random random = new Random(System.currentTimeMillis());

    /**
     * {@inheritDoc}
     */
    @Override
    public Node elect(List<Node> nodes) {
        int size = nodes.size();
        return (size > 0) ? nodes.get(this.random.nextInt(size)) : null;
    }
}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.singleton.election;

import java.util.List;

import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.singleton.SingletonElectionPolicy;

/**
 * A simple concrete policy service that decides which node in the cluster should be the primary node to run certain HASingleton
 * service based on attribute "Position". The value will be divided by partition size and only remainder will be used.
 *
 * Let's say partition size is n: 0 means the first oldest node. 1 means the 2nd oldest node. ... n-1 means the nth oldest node.
 *
 * -1 means the youngest node. -2 means the 2nd youngest node. ... -n means the nth youngest node.
 *
 * E.g. the following attribute says the singleton will be running on the 3rd oldest node of the current partition: <attribute
 * name="Position">2</attribute>
 *
 * If no election policy is defined, the oldest node in the cluster runs the singleton. This behavior can be achieved with this
 * policy when "position" is set to 0.
 *
 * @author <a href="mailto:Alex.Fu@novell.com">Alex Fu</a>
 * @author <a href="mailto:galder.zamarreno@jboss.com">Galder Zamarreno</a>
 * @author Paul Ferraro
 * @deprecated Replaced by {@link org.wildfly.clustering.singleton.election.SingletonElectionPolicy#position(int)}.
 */
@Deprecated(forRemoval = true)
public class SimpleSingletonElectionPolicy implements SingletonElectionPolicy {

    private final int position;

    public SimpleSingletonElectionPolicy() {
        this(0);
    }

    public SimpleSingletonElectionPolicy(int position) {
        this.position = position;
    }

    @Override
    public Node elect(List<Node> candidates) {
        int size = candidates.size();
        return (size > 0) ? candidates.get(((this.position % size) + size) % size) : null;
    }
}

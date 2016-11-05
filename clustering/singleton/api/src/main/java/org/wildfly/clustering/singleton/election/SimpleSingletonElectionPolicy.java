/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
 */
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

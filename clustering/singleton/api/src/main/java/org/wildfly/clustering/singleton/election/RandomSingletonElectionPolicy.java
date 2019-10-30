/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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
import java.util.Random;

import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.singleton.SingletonElectionPolicy;

/**
 * {@link SingletonElectionPolicy} that elects a random member.
 * @author Paul Ferraro
 */
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

/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.clustering.singleton.election;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jboss.as.clustering.ClusterNode;
import org.junit.Test;

/**
 * @author Paul Ferraro
 */
public class SimpleSingletonElectionPolicyTestCase {
    @Test
    public void elect() {
        ClusterNode node1 = mock(ClusterNode.class);
        ClusterNode node2 = mock(ClusterNode.class);
        ClusterNode node3 = mock(ClusterNode.class);
        List<ClusterNode> nodes = Arrays.asList(node1, node2, node3);

        assertSame(node1, new SimpleSingletonElectionPolicy().elect(nodes));
        assertSame(node1, new SimpleSingletonElectionPolicy(0).elect(nodes));
        assertSame(node2, new SimpleSingletonElectionPolicy(1).elect(nodes));
        assertSame(node3, new SimpleSingletonElectionPolicy(2).elect(nodes));
        assertSame(node1, new SimpleSingletonElectionPolicy(3).elect(nodes));

        assertNull(new SimpleSingletonElectionPolicy().elect(Collections.<ClusterNode>emptyList()));
    }
}

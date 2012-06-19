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

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;

import org.jboss.as.clustering.ClusterNode;
import org.jboss.as.clustering.singleton.SingletonElectionPolicy;
import org.junit.Test;

/**
 * @author Paul Ferraro
 */
public class PreferredSingletonElectionPolicyTestCase {
    @Test
    public void elect() {
        SingletonElectionPolicy policy = mock(SingletonElectionPolicy.class);
        Preference preference1 = mock(Preference.class);
        Preference preference2 = mock(Preference.class);

        ClusterNode node1 = mock(ClusterNode.class);
        ClusterNode node2 = mock(ClusterNode.class);
        ClusterNode node3 = mock(ClusterNode.class);
        ClusterNode node4 = mock(ClusterNode.class);

        when(preference1.preferred(same(node1))).thenReturn(true);
        when(preference1.preferred(same(node2))).thenReturn(false);
        when(preference1.preferred(same(node3))).thenReturn(false);
        when(preference1.preferred(same(node4))).thenReturn(false);

        when(preference2.preferred(same(node1))).thenReturn(false);
        when(preference2.preferred(same(node2))).thenReturn(true);
        when(preference2.preferred(same(node3))).thenReturn(false);
        when(preference2.preferred(same(node4))).thenReturn(false);

        assertSame(node1, new PreferredSingletonElectionPolicy(policy, preference1, preference2).elect(Arrays.asList(node1, node2, node3, node4)));
        assertSame(node1, new PreferredSingletonElectionPolicy(policy, preference1, preference2).elect(Arrays.asList(node4, node3, node2, node1)));
        assertSame(node2, new PreferredSingletonElectionPolicy(policy, preference1, preference2).elect(Arrays.asList(node2, node3, node4)));
        assertSame(node2, new PreferredSingletonElectionPolicy(policy, preference1, preference2).elect(Arrays.asList(node4, node3, node2)));

        List<ClusterNode> nodes = Arrays.asList(node3, node4);
        when(policy.elect(nodes)).thenReturn(node3);

        assertSame(node3, new PreferredSingletonElectionPolicy(policy, preference1, preference2).elect(nodes));

        when(policy.elect(nodes)).thenReturn(node4);

        assertSame(node4, new PreferredSingletonElectionPolicy(policy, preference1, preference2).elect(nodes));

        when(policy.elect(nodes)).thenReturn(null);

        assertNull(new PreferredSingletonElectionPolicy(policy, preference1, preference2).elect(nodes));
    }
}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee.infinispan;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.function.Function;

import org.infinispan.remoting.transport.Address;
import org.junit.Test;
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.infinispan.distribution.KeyDistribution;
import org.wildfly.clustering.server.NodeFactory;

/**
 * @author Paul Ferraro
 */
public class PrimaryOwnerLocatorTestCase {

    @Test
    public void test() {
        KeyDistribution distribution = mock(KeyDistribution.class);
        NodeFactory<Address> memberFactory = mock(NodeFactory.class);
        Address staleAddress = mock(Address.class);
        Address address = mock(Address.class);
        Node member = mock(Node.class);
        Object key = new Object();

        Function<Object, Node> locator = new PrimaryOwnerLocator<>(distribution, memberFactory);

        when(distribution.getPrimaryOwner(key)).thenReturn(staleAddress, address);
        when(memberFactory.createNode(staleAddress)).thenReturn(null);
        when(memberFactory.createNode(address)).thenReturn(member);

        Node result = locator.apply(key);

        assertSame(member, result);
    }
}

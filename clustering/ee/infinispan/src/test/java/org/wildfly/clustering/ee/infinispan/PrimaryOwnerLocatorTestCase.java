/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.ee.infinispan;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.function.Function;

import org.infinispan.remoting.transport.Address;
import org.junit.Test;
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.infinispan.spi.distribution.KeyDistribution;
import org.wildfly.clustering.spi.NodeFactory;

/**
 * @author Paul Ferraro
 */
public class PrimaryOwnerLocatorTestCase {

    @Test
    public void test() {
        KeyDistribution distribution = mock(KeyDistribution.class);
        NodeFactory<Address> memberFactory = mock(NodeFactory.class);
        Address address = mock(Address.class);
        Node member = mock(Node.class);
        Object key = new Object();

        Function<Object, Node> locator = new PrimaryOwnerLocator<>(distribution, memberFactory);

        when(distribution.getPrimaryOwner(key)).thenReturn(address);
        when(memberFactory.createNode(address)).thenReturn(member);

        Node result = locator.apply(key);

        assertSame(member, result);
    }
}

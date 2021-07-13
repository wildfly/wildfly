/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.web.infinispan.routing;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Arrays;
import java.util.Collections;

import org.infinispan.remoting.transport.Address;
import org.junit.Test;
import org.wildfly.clustering.ee.infinispan.GroupedKey;
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.infinispan.spi.distribution.KeyDistribution;
import org.wildfly.clustering.registry.Registry;
import org.wildfly.clustering.spi.NodeFactory;
import org.wildfly.clustering.web.routing.RouteLocator;

/**
 * @author Paul Ferraro
 */
public class RankedRouteLocatorTestCase {

    @Test
    public void test() {
        KeyDistribution distribution = mock(KeyDistribution.class);
        NodeFactory<Address> factory = mock(NodeFactory.class);
        Registry<String, Void> registry = mock(Registry.class);
        Group group = mock(Group.class);
        Address owner1 = mock(Address.class);
        Address owner2 = mock(Address.class);
        Address owner3 = mock(Address.class);
        Address owner4 = mock(Address.class);
        Address unregistered = mock(Address.class);
        Address local = mock(Address.class);
        Node member1 = mock(Node.class);
        Node member2 = mock(Node.class);
        Node member3 = mock(Node.class);
        Node member4 = mock(Node.class);
        Node unregisteredMember = mock(Node.class);
        Node localMember = mock(Node.class);

        when(registry.getGroup()).thenReturn(group);
        when(group.getLocalMember()).thenReturn(localMember);
        when(registry.getEntry(member1)).thenReturn(new SimpleImmutableEntry<>("member1", null));
        when(registry.getEntry(member2)).thenReturn(new SimpleImmutableEntry<>("member2", null));
        when(registry.getEntry(member3)).thenReturn(new SimpleImmutableEntry<>("member3", null));
        when(registry.getEntry(member4)).thenReturn(new SimpleImmutableEntry<>("member4", null));
        when(registry.getEntry(localMember)).thenReturn(new SimpleImmutableEntry<>("local", null));
        when(registry.getEntry(unregisteredMember)).thenReturn(null);
        when(factory.createNode(owner1)).thenReturn(member1);
        when(factory.createNode(owner2)).thenReturn(member2);
        when(factory.createNode(owner3)).thenReturn(member3);
        when(factory.createNode(owner4)).thenReturn(member4);
        when(factory.createNode(local)).thenReturn(localMember);
        when(factory.createNode(unregistered)).thenReturn(unregisteredMember);

        RouteLocator locator = new RankedRouteLocator(distribution, registry, factory, ".", 3);

        when(distribution.getOwners(new GroupedKey<>("key"))).thenReturn(Arrays.asList(owner1, owner2, owner3, owner4));

        assertEquals("member1.member2.member3", locator.locate("key"));

        when(distribution.getOwners(new GroupedKey<>("key"))).thenReturn(Arrays.asList(owner1, owner2, owner3, local));

        assertEquals("member1.member2.member3", locator.locate("key"));

        when(distribution.getOwners(new GroupedKey<>("key"))).thenReturn(Arrays.asList(owner1, owner2, unregistered, owner4));

        assertEquals("member1.member2.member4", locator.locate("key"));

        when(distribution.getOwners(new GroupedKey<>("key"))).thenReturn(Arrays.asList(owner1, local, owner3));

        assertEquals("member1.local.member3", locator.locate("key"));

        when(distribution.getOwners(new GroupedKey<>("key"))).thenReturn(Arrays.asList(owner1, owner2));

        assertEquals("member1.member2.local", locator.locate("key"));

        when(distribution.getOwners(new GroupedKey<>("key"))).thenReturn(Arrays.asList(local, owner2));

        assertEquals("local.member2", locator.locate("key"));

        when(distribution.getOwners(new GroupedKey<>("key"))).thenReturn(Arrays.asList(owner1));

        assertEquals("member1.local", locator.locate("key"));

        when(distribution.getOwners(new GroupedKey<>("key"))).thenReturn(Arrays.asList(local));

        assertEquals("local", locator.locate("key"));

        when(distribution.getOwners(new GroupedKey<>("key"))).thenReturn(Arrays.asList(unregistered));

        assertEquals("local", locator.locate("key"));

        when(distribution.getOwners(new GroupedKey<>("key"))).thenReturn(Collections.emptyList());

        assertEquals("local", locator.locate("key"));
    }
}

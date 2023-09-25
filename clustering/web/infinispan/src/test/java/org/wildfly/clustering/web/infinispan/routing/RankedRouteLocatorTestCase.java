/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.infinispan.routing;

import static org.junit.Assert.assertEquals;
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
import org.wildfly.clustering.infinispan.distribution.KeyDistribution;
import org.wildfly.clustering.registry.Registry;
import org.wildfly.clustering.server.NodeFactory;
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

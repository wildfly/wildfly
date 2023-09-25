/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.infinispan.routing;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.function.Function;

import org.junit.Test;
import org.wildfly.clustering.ee.infinispan.GroupedKey;
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.registry.Registry;
import org.wildfly.clustering.web.routing.RouteLocator;

/**
 * Unit test for {@link InfinispanRouteLocator}.
 * @author Paul Ferraro
 */
public class PrimaryOwnerRouteLocatorTestCase {

    @Test
    public void test() {
        Function<GroupedKey<String>, Node> locator = mock(Function.class);
        Registry<String, Void> registry = mock(Registry.class);
        Group group = mock(Group.class);

        Node primary = mock(Node.class);
        Node local = mock(Node.class);
        Node missing = mock(Node.class);
        String primaryRoute = "primary";
        String localRoute = "local";

        when(registry.getGroup()).thenReturn(group);
        when(group.getLocalMember()).thenReturn(local);
        when(registry.getEntry(local)).thenReturn(new SimpleImmutableEntry<>(localRoute, null));

        RouteLocator routeLocator = new PrimaryOwnerRouteLocator(locator, registry);

        when(locator.apply(new GroupedKey<>("session"))).thenReturn(primary);
        when(registry.getEntry(primary)).thenReturn(new SimpleImmutableEntry<>(primaryRoute, null));

        String result = routeLocator.locate("session");
        assertSame(primaryRoute, result);

        when(locator.apply(new GroupedKey<>("missing"))).thenReturn(missing);
        when(registry.getEntry(missing)).thenReturn(null);

        result = routeLocator.locate("missing");

        assertSame(localRoute, result);
    }
}

/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

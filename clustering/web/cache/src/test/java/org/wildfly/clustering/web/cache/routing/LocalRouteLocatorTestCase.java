/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.routing;

import org.junit.Assert;
import org.junit.Test;
import org.wildfly.clustering.web.routing.RouteLocator;

/**
 * @author Paul Ferraro
 */
public class LocalRouteLocatorTestCase {

    @Test
    public void test() {
        String route = "route";
        RouteLocator locator = new LocalRouteLocator(route);
        String result = locator.locate("abc123");
        Assert.assertSame(route, result);
    }
}

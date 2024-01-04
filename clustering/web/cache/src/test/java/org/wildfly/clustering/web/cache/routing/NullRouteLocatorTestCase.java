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
public class NullRouteLocatorTestCase {

    @Test
    public void test() {
        RouteLocator locator = new NullRouteLocator();

        Assert.assertNull(locator.locate("abc123"));
    }
}

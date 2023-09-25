/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.routing;

import org.wildfly.clustering.web.routing.RouteLocator;

/**
 * Route locator that always returns the route of the local member.
 * @author Paul Ferraro
 */
public class LocalRouteLocator implements RouteLocator {

    private final String route;

    public LocalRouteLocator(String route) {
        this.route = route;
    }

    @Override
    public String locate(String sessionId) {
        return this.route;
    }
}

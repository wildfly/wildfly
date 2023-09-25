/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.routing;

import org.wildfly.clustering.web.routing.RouteLocator;

/**
 * Route locator that always returns {@code null}.
 * @author Paul Ferraro
 */
public class NullRouteLocator implements RouteLocator {

    @Override
    public String locate(String sessionId) {
        return null;
    }
}

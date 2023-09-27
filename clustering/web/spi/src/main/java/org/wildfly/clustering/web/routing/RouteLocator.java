/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.web.routing;

/**
 * Locates the route appropriate for a given session identifier.
 * @author Paul Ferraro
 */
public interface RouteLocator {
    /**
     * Returns the route identifier most appropriate for the specified session identifier.
     * @param sessionId a unique session identifier
     * @return a unique instance identifier
     */
    String locate(String sessionId);
}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.undertow.routing;

import org.jboss.as.web.session.AffinityLocator;
import org.wildfly.clustering.web.routing.RouteLocator;

/**
 * The {@link AffinityLocator} implementation that leverages distributable {@link RouteLocator}.
 *
 * @author Radoslav Husar
 */
public class DistributableAffinityLocator implements AffinityLocator {

    private final RouteLocator locator;

    public DistributableAffinityLocator(RouteLocator locator) {
        this.locator = locator;
    }

    @Override
    public String locate(String sessionId) {
        return this.locator.locate(sessionId);
    }
}

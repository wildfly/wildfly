/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.undertow.routing;

import java.util.function.UnaryOperator;

import org.jboss.as.web.session.AffinityLocator;

/**
 * The {@link AffinityLocator} implementation that leverages distributable {@link RouteLocator}.
 *
 * @author Radoslav Husar
 */
public class DistributableAffinityLocator implements AffinityLocator {

    private final UnaryOperator<String> locator;

    public DistributableAffinityLocator(UnaryOperator<String> locator) {
        this.locator = locator;
    }

    @Override
    public String locate(String sessionId) {
        return this.locator.apply(sessionId);
    }
}

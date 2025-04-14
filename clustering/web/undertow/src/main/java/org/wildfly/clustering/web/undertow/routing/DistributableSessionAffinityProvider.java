/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.undertow.routing;

import java.util.function.UnaryOperator;

import org.jboss.as.web.session.SessionAffinityProvider;

/**
 * The {@link SessionAffinityProvider} implementation that leverages distributable {@link RouteLocator}.
 *
 * @author Radoslav Husar
 */
public class DistributableSessionAffinityProvider implements SessionAffinityProvider {

    private final UnaryOperator<String> locator;

    public DistributableSessionAffinityProvider(UnaryOperator<String> locator) {
        this.locator = locator;
    }

    @Override
    public String getAffinity(String sessionId) {
        return this.locator.apply(sessionId);
    }
}

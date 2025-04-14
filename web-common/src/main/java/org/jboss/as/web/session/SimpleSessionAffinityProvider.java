/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.web.session;

/**
 * Simple affinity locator implementation using static affinity.
 *
 * @author Radoslav Husar
 */
public class SimpleSessionAffinityProvider implements SessionAffinityProvider {

    private final String route;

    public SimpleSessionAffinityProvider(String route) {
        this.route = route;
    }

    @Override
    public String getAffinity(String sessionID) {
        return route;
    }

}

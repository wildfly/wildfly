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
public class SimpleAffinityLocator implements AffinityLocator {

    private final String route;

    public SimpleAffinityLocator(String route) {
        this.route = route;
    }

    @Override
    public String locate(String sessionID) {
        return route;
    }

}

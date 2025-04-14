/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.undertow.session;

/**
 * Simple session affinity provider using a fixed route.
 *
 * @author Radoslav Husar
 */
public class SimpleSessionAffinityProvider implements SessionAffinityProvider {

    private final String route;

    public SimpleSessionAffinityProvider(String route) {
        this.route = route;
    }

    @Override
    public String getAffinity(String sessionId) {
        return route;
    }
}

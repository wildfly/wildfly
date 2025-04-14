/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow.session;

import org.jboss.as.web.session.SessionAffinityProvider;
import org.wildfly.extension.undertow.Server;

/**
 * Provider for non-distributable sessions that always have affinity for the same server.
 * @author Paul Ferraro
 */
public class NonDistributableSessionAffinityProvider implements SessionAffinityProvider {
    private final String serverId;

    public NonDistributableSessionAffinityProvider(Server server) {
        this.serverId = server.getRoute();
    }

    @Override
    public String getAffinity(String sessionId) {
        return this.serverId;
    }
}

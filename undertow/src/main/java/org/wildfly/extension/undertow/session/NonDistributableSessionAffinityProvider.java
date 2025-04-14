/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow.session;

import java.util.Optional;

import org.wildfly.extension.undertow.Server;

/**
 * Provider for non-distributable sessions that always have affinity for the same server.
 * @author Paul Ferraro
 */
public class NonDistributableSessionAffinityProvider implements SessionAffinityProvider {
    private final Optional<String> serverId;

    public NonDistributableSessionAffinityProvider(Server server) {
        this.serverId = Optional.of(server.getRoute());
    }

    @Override
    public Optional<String> getAffinity(String sessionId) {
        return this.serverId;
    }
}

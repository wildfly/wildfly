/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.mod_cluster.undertow;

import java.util.Collections;

import org.jboss.modcluster.container.Connector;
import org.jboss.modcluster.container.Engine;
import org.jboss.modcluster.container.Server;
import org.wildfly.extension.undertow.Capabilities;
import org.wildfly.extension.undertow.UndertowService;

/**
 * Adapts {@link UndertowService} to a {@link Server}.
 *
 * @author Radoslav Husar
 * @author Paul Ferraro
 * @since 8.0
 */
public class UndertowServer implements Server {

    private final String serverName;
    private final UndertowService service;
    private final Connector connector;

    public UndertowServer(String serverName, UndertowService service, Connector connector) {
        this.serverName = serverName;
        this.service = service;
        this.connector = connector;
    }

    @Override
    public Iterable<Engine> getEngines() {
        for (org.wildfly.extension.undertow.Server server : this.service.getServers()) {
            if (server.getName().equals(this.serverName)) {
                return Collections.singleton(new UndertowEngine(serverName, server, this.service, this.connector));
            }
        }
        throw new IllegalStateException();
    }

    @Override
    public String toString() {
        return Capabilities.CAPABILITY_UNDERTOW;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof UndertowServer)) return false;

        UndertowServer server = (UndertowServer) object;
        return this.service.equals(server.service);
    }

    @Override
    public int hashCode() {
        return this.service.hashCode();
    }
}

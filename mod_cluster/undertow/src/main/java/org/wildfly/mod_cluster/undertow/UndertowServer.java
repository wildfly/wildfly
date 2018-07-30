/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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

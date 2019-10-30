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


import java.util.Iterator;

import org.jboss.modcluster.container.Connector;
import org.jboss.modcluster.container.Engine;
import org.jboss.modcluster.container.Host;
import org.jboss.modcluster.container.Server;
import org.wildfly.extension.undertow.SessionCookieConfig;
import org.wildfly.extension.undertow.UndertowListener;
import org.wildfly.extension.undertow.UndertowService;

/**
 * Adapts {@link org.wildfly.extension.undertow.Server} to an {@link Engine}.
 *
 * @author Radoslav Husar
 * @since 8.0
 */
public class UndertowEngine implements Engine {

    private final String serverName;
    private final UndertowService service;
    private final org.wildfly.extension.undertow.Server server;
    private final Connector connector;

    public UndertowEngine(String serverName, org.wildfly.extension.undertow.Server server, UndertowService service, Connector connector) {
        this.serverName = serverName;
        this.server = server;
        this.service = service;
        this.connector = connector;
    }

    @Override
    public String getName() {
        return this.server.getName();
    }

    @Override
    public Server getServer() {
        return new UndertowServer(this.serverName, this.service, this.connector);
    }

    @Override
    public Iterable<Host> getHosts() {

        final Iterator<org.wildfly.extension.undertow.Host> hosts = this.server.getHosts().iterator();

        final Iterator<Host> iterator = new Iterator<Host>() {
            @Override
            public boolean hasNext() {
                return hosts.hasNext();
            }

            @Override
            public Host next() {
                org.wildfly.extension.undertow.Host host = hosts.next();
                return new UndertowHost(host, UndertowEngine.this);
            }

            @Override
            public void remove() {
                hosts.remove();
            }
        };

        return new Iterable<Host>() {
            @Override
            public Iterator<Host> iterator() {
                return iterator;
            }
        };

    }

    @Override
    public Connector getProxyConnector() {
        return this.connector;
    }

    @Override
    public Iterable<Connector> getConnectors() {
        final Iterator<UndertowListener> listeners = this.server.getListeners().iterator();

        final Iterator<Connector> iterator = new Iterator<Connector>() {
            @Override
            public boolean hasNext() {
                return listeners.hasNext();
            }

            @Override
            public Connector next() {
                return new UndertowConnector(listeners.next());
            }

            @Override
            public void remove() {
                listeners.remove();
            }
        };

        return new Iterable<Connector>() {
            @Override
            public Iterator<Connector> iterator() {
                return iterator;
            }
        };
    }

    @Override
    public String getJvmRoute() {
        return this.server.getRoute();
    }

    @Override
    public void setJvmRoute(String jvmRoute) {
        // Ignore
    }

    @Override
    public Host findHost(String name) {
        for (org.wildfly.extension.undertow.Host host : this.server.getHosts()) {
            if (host.getName().equals(name)) {
                return new UndertowHost(host, UndertowEngine.this);
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @return overridden session cookie name if defined, otherwise {@link io.undertow.server.session.SessionCookieConfig#DEFAULT_SESSION_ID}
     */
    @Override
    public String getSessionCookieName() {
        SessionCookieConfig override = server.getServletContainer().getSessionCookieConfig();
        if (override == null || override.getName() == null) {
            return io.undertow.server.session.SessionCookieConfig.DEFAULT_SESSION_ID;
        }
        return override.getName();
    }

    /**
     * {@inheritDoc}
     *
     * @return lowercase value of {@link #getSessionCookieName()}
     */
    @Override
    public String getSessionParameterName() {
        return getSessionCookieName().toLowerCase();
    }

    @Override
    public String getDefaultHost() {
        return this.server.getDefaultHost();
    }

    @Override
    public String toString() {
        return this.getName();
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof UndertowEngine)) return false;

        UndertowEngine engine = (UndertowEngine) object;
        return this.getName().equals(engine.getName());
    }

    @Override
    public int hashCode() {
        return this.server.getName().hashCode();
    }
}

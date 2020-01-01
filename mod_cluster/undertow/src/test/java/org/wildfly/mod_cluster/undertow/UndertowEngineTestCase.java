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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.Collections;
import java.util.Iterator;

import org.jboss.as.controller.PathAddress;
import org.jboss.modcluster.container.Connector;
import org.jboss.modcluster.container.Engine;
import org.junit.Test;
import org.wildfly.extension.undertow.Constants;
import org.wildfly.extension.undertow.Host;
import org.wildfly.extension.undertow.HttpsListenerService;
import org.wildfly.extension.undertow.Server;
import org.wildfly.extension.undertow.UndertowService;
import org.xnio.OptionMap;

import io.undertow.util.StatusCodes;

/**
 * @author Paul Ferraro
 * @author Radoslav Husar
 */
public class UndertowEngineTestCase {
    private final String serverName = "default-server";
    private final String hostName = "default-host";
    private final String route = "route";
    private final Host host = new Host(this.hostName, Collections.emptyList(), "ROOT.war", StatusCodes.NOT_FOUND, false);
    private final HttpsListenerService listener = new HttpsListenerService(PathAddress.pathAddress(Constants.HTTPS_LISTENER, "default"), "https", OptionMap.EMPTY, null, OptionMap.EMPTY, false);

    private final UndertowService service = new TestUndertowService("default-container", this.serverName, this.hostName, this.route, false, this.server);
    private final Server server = new TestServer(this.serverName, this.hostName, this.service, this.host, this.listener);
    private final Connector connector = mock(Connector.class);
    private final Engine engine = new UndertowEngine(this.serverName, this.server, this.service, this.connector);

    @Test
    public void getName() {
        assertSame(this.serverName, this.engine.getName());
    }

    @Test
    public void getHosts() {
        Iterator<org.jboss.modcluster.container.Host> results = this.engine.getHosts().iterator();
        assertTrue(results.hasNext());
        org.jboss.modcluster.container.Host host = results.next();
        assertSame(this.hostName, host.getName());
        assertSame(this.engine, host.getEngine());
        assertFalse(results.hasNext());
    }

    @Test
    public void getConnectors() {
        Iterator<org.jboss.modcluster.container.Connector> results = this.engine.getConnectors().iterator();
        assertTrue(results.hasNext());
        org.jboss.modcluster.container.Connector connector = results.next();

        String listenerName = "default";
        assertSame(listenerName, connector.toString());
        assertFalse(results.hasNext());
    }

    @Test
    public void getDefaultHost() {
        assertSame(this.hostName, this.engine.getDefaultHost());
    }

    @Test
    public void findHost() {
        org.jboss.modcluster.container.Host result = this.engine.findHost(this.hostName);
        assertSame(this.hostName, result.getName());
        assertSame(this.engine, result.getEngine());
        assertNull(this.engine.findHost("no-such-host"));
    }

    @Test
    public void getJvmRoute() {
        assertSame(this.route, this.engine.getJvmRoute());
    }

    @Test
    public void getObfuscatedJvmRoute() {
        // scenario 1, just create a service with obfuscated route but same config as this.service
        final TestUndertowService service1 = new TestUndertowService("default-container", this.serverName, this.hostName, this.route, true, null);
        final Server server1 = new TestServer(this.serverName, this.hostName, service1, this.host, this.listener);
        final Engine engine1 = new UndertowEngine(this.serverName, server1, service1, this.connector);

        assertNotEquals(this.route, engine1.getJvmRoute());

        // after restart, recreate all objects, is the route still the same if config is kept unchanged?
        final Host host2 = new Host(this.hostName, Collections.emptyList(), "ROOT.war", StatusCodes.NOT_FOUND, false);
        final HttpsListenerService listener2 = new HttpsListenerService(PathAddress.pathAddress(Constants.HTTPS_LISTENER, "default"), "https", OptionMap.EMPTY, null, OptionMap.EMPTY, false);
        final UndertowService service2 = new TestUndertowService("default-container", this.serverName, this.hostName, this.route, true, null);
        final Server server2 = new TestServer(this.serverName, this.hostName, service2, host2, listener2);
        final Connector connector2 = mock(Connector.class);
        final Engine engine2 = new UndertowEngine(this.serverName, server2, service2, connector2);

        assertEquals(engine1.getJvmRoute(), engine2.getJvmRoute());

        // with a different route, is the obfuscated route different from previous one?
        final TestUndertowService service3 = new TestUndertowService("default-container", this.serverName, this.hostName, "adifferentroute", true, null);
        final Server server3 = new TestServer(this.serverName, this.hostName, service3, this.host, this.listener);
        final Engine engine3 = new UndertowEngine(this.serverName, server3, service3, this.connector);

        assertNotEquals(engine1.getJvmRoute(), engine3.getJvmRoute());
        // just double check it is obfuscated for engine3 as well
        assertNotEquals("adifferentrout", engine3.getJvmRoute());
    }

    @Test
    public void getProxyConnector() {
        assertSame(this.connector, this.engine.getProxyConnector());
    }
}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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

import org.jboss.modcluster.container.Connector;
import org.jboss.modcluster.container.Engine;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.junit.Test;
import org.wildfly.extension.undertow.Server;

/**
 * @author Paul Ferraro
 * @author Radoslav Husar
 */
public class UndertowEngineTestCase {
    private final String serverName = "default-server";
    private final String hostName = "default-host";
    private final String route = "route";
    private final TestUndertowService service = new TestUndertowService("default-container", this.serverName, this.hostName, this.route, false);
    private final TestServer server = new TestServer(this.serverName, this.hostName, this.service);
    private final TestHost host = new TestHost(this.hostName, Collections.emptyList(), this.server);
    private final TestListener listener = new TestListener(this.server);

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
    public void getJvmRoute() throws StartException {
        server.start(mock(StartContext.class));
        assertSame(this.route, this.engine.getJvmRoute());
    }

    @Test
    public void getObfuscatedJvmRoute() throws StartException {
        // scenario 1, just create a service with obfuscated route but same config as this.service
        final TestUndertowService service1 = new TestUndertowService("default-container", this.serverName, this.hostName, this.route, true);
        final TestServer server1 = new TestServer(this.serverName, this.hostName, service1);
        server1.start(mock(StartContext.class));
        final Engine engine1 = new UndertowEngine(this.serverName, server1, service1, this.connector);

        assertNotEquals(this.route, engine1.getJvmRoute());

        // after restart, recreate all objects, is the route still the same if config is kept unchanged?
        final TestUndertowService service2 = new TestUndertowService("default-container", this.serverName, this.hostName, this.route, true);
        final Server server2 = new TestServer(this.serverName, this.hostName, service2);

        server2.start(mock(StartContext.class));
        final Connector connector2 = mock(Connector.class);
        final Engine engine2 = new UndertowEngine(this.serverName, server2, service2, connector2);

        assertEquals(engine1.getJvmRoute(), engine2.getJvmRoute());

        // with a different route, is the obfuscated route different from previous one?
        final TestUndertowService service3 = new TestUndertowService("default-container", this.serverName, this.hostName, "adifferentroute", true);
        final TestServer server3 = new TestServer(this.serverName, this.hostName, service3);
        server3.start(mock(StartContext.class));
        final Engine engine3 = new UndertowEngine(this.serverName, server3, service3, this.connector);

        assertNotEquals(engine1.getJvmRoute(), engine3.getJvmRoute());
        // just double check it is obfuscated for engine3 as well
        assertNotEquals("adifferentroute", engine3.getJvmRoute());

        // with a different server name, is the obfuscated route different from previous one?
        final TestUndertowService service4 = new TestUndertowService("default-container", "another.server", this.hostName, "this.route", true);
        final TestServer server4 = new TestServer("another.server", this.hostName, service4);
        server4.start(mock(StartContext.class));
        final Engine engine4 = new UndertowEngine(this.serverName, server4, service4, this.connector);

        assertNotEquals(engine1.getJvmRoute(), engine4.getJvmRoute());
        // just double check it is obfuscated for engine4 as well
        assertNotEquals(this.route, engine4.getJvmRoute());
    }

    @Test
    public void getProxyConnector() {
        assertSame(this.connector, this.engine.getProxyConnector());
    }

}

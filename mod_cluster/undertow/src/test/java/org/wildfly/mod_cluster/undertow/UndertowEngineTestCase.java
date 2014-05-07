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

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import java.util.Collections;
import java.util.Iterator;

import org.jboss.modcluster.container.Connector;
import org.jboss.modcluster.container.Engine;
import org.junit.Test;
import org.wildfly.extension.undertow.HttpsListenerService;
import org.wildfly.extension.undertow.ListenerService;
import org.wildfly.extension.undertow.Host;
import org.wildfly.extension.undertow.Server;
import org.wildfly.extension.undertow.UndertowService;
import org.xnio.OptionMap;

public class UndertowEngineTestCase {
    private final String defaultHost = "default-host";
    private final String serverName = "name";
    private final String hostName = "host";
    private final Host host = new Host(this.hostName, Collections.<String>emptyList(), "ROOT.war") {};
    private final HttpsListenerService listener = new HttpsListenerService("default", "https",OptionMap.EMPTY, OptionMap.EMPTY);
    private final Server server = new TestServer(this.serverName, this.defaultHost, this.host, this.listener);
    private final UndertowService service = new TestUndertowService("default-container", "default-server", "default-virtual-host", "instance-id", this.server);
    private final Connector connector = mock(Connector.class);
    private final Engine engine = new UndertowEngine(this.server, this.service, this.connector);

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
        assertSame(this.defaultHost, this.engine.getDefaultHost());
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
        String expected = "node0";
        
        this.service.setInstanceId("node0");
        
        assertSame(expected, this.engine.getJvmRoute());
    }
    
    @Test
    public void setJvmRoute() {
        String expected = "node1";
        
        this.engine.setJvmRoute(expected);
        
        assertSame(expected, this.service.getInstanceId());
    }
    
    @Test
    public void getProxyConnector() {
        assertSame(this.connector, this.engine.getProxyConnector());
    }
}

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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.Iterator;

import org.jboss.modcluster.container.Connector;
import org.jboss.modcluster.container.Engine;
import org.jboss.modcluster.container.Server;
import org.junit.Test;
import org.wildfly.extension.undertow.UndertowService;

public class UndertowServerTestCase {
    private final Connector connector = mock(Connector.class);
    private final String route = "route";
    private final org.wildfly.extension.undertow.Server undertowServer = new TestServer("default-server", "default-host");
    private final UndertowService service = new TestUndertowService("default-container", "default-server", "default-virtual-host", this.route, this.undertowServer);
    private final Server server = new UndertowServer(this.service, this.connector);

    @Test
    public void getEngines() {
        Iterator<Engine> engines = this.server.getEngines().iterator();

        assertTrue(engines.hasNext());
        Engine engine = engines.next();
        assertSame(this.connector, engine.getProxyConnector());
        assertFalse(engines.hasNext());
    }
}

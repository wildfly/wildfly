/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.mod_cluster.undertow;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Iterator;

import org.jboss.modcluster.container.Connector;
import org.jboss.modcluster.container.Engine;
import org.jboss.modcluster.container.Server;
import org.junit.Test;
import org.wildfly.extension.undertow.UndertowService;

/**
 * @author Paul Ferraro
 * @author Radoslav Husar
 */
public class UndertowServerTestCase {
    private final Connector connector = mock(Connector.class);
    private final String route = "route";
    private final org.wildfly.extension.undertow.Server undertowServer = new TestServer("default-server", "default-host");
    private final UndertowService service = new TestUndertowService(null, "default-container", "default-server", "default-virtual-host", this.route, false, this.undertowServer);
    private final UndertowEventHandlerAdapterConfiguration configuration = mock(UndertowEventHandlerAdapterConfiguration.class);
    private final Server server = new UndertowServer(undertowServer.getName(), this.service, this.connector);

    @Test
    public void getEngines() {
        when(configuration.getServer()).thenReturn(mock(org.wildfly.extension.undertow.Server.class));
        when(configuration.getServer().getName()).thenReturn(undertowServer.getName());

        Iterator<Engine> engines = this.server.getEngines().iterator();

        assertTrue(engines.hasNext());
        Engine engine = engines.next();
        assertSame(this.connector, engine.getProxyConnector());
        assertFalse(engines.hasNext());
    }
}

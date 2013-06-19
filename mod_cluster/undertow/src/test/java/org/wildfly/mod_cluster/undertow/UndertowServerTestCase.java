package org.wildfly.mod_cluster.undertow;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import java.util.Iterator;

import org.jboss.modcluster.container.Connector;
import org.jboss.modcluster.container.Engine;
import org.jboss.modcluster.container.Server;
import org.junit.Test;
import org.wildfly.extension.undertow.UndertowService;

public class UndertowServerTestCase {
    private final Connector connector = mock(Connector.class);
    private final org.wildfly.extension.undertow.Server undertowServer = new TestServer("server-name", "default-host");
    private final UndertowService service = new TestUndertowService("default-container", "defaultServer", "default-virtual-host", "instance-id", this.undertowServer);
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

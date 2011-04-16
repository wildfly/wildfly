/*
* JBoss, Home of Professional Open Source.
* Copyright 2006, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.controller.test;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.AccessController;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ServerSocketFactory;

import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ProxyController;
import org.jboss.as.controller.remote.RemoteProxyController;
import org.jboss.as.protocol.Connection;
import org.jboss.as.protocol.ConnectionHandler;
import org.jboss.as.protocol.MessageHandler;
import org.jboss.as.protocol.ProtocolServer;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.as.protocol.mgmt.AbstractMessageHandler;
import org.jboss.as.protocol.mgmt.ManagementRequestConnectionStrategy;
import org.jboss.threads.JBossThreadFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;

/**
 * Tests a proxy controller where the main side uses an existing connection opened by the proxied side.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
@Ignore("Disabled as it intermittently fails which proves that duplexing over jboss-as-protocol connections is invalid")
public class ExistingConnectionRemoteProxyControllerTestCase extends AbstractProxyControllerTest {

    ModelController proxiedController;
    PathAddress proxyNodeAddress;
    DelegatingProxyController proxyController = new DelegatingProxyController();
    // Server running on the mainController side
    ProtocolServer server;
    // Conn to main side opened by proxied side
    Connection clientConn;
    // Main side end of clientConn
    Connection serverConn;

    @Before
    public void start() throws Exception {

        setupNodes();

        // Set up the main-controller-side comm server
        final ProtocolServer.Configuration config = new ProtocolServer.Configuration();
        config.setBindAddress(new InetSocketAddress(InetAddress.getByName("localhost"), 0));
        final AtomicInteger increment = new AtomicInteger();
        config.setThreadFactory(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "Server-" + increment.incrementAndGet());
            }
        });
        final ThreadFactory threadFactory = new JBossThreadFactory(new ThreadGroup("MainController-ProtocolServer-threads"), Boolean.FALSE, null, "%G - %t", null, null, AccessController.getContext());
        config.setThreadFactory(threadFactory);
        config.setReadExecutor(Executors.newCachedThreadPool(threadFactory));
        config.setSocketFactory(ServerSocketFactory.getDefault());
        config.setBacklog(50);

        final CountDownLatch connectionLatch = new CountDownLatch(1);
        config.setConnectionHandler(new ConnectionHandler() {
            @Override
            public MessageHandler handleConnected(Connection connection) throws IOException {

                serverConn = connection;
                try {
                    MessageHandler handler = new TestManagementHeaderMessageHandler();
                    return handler;
                } finally {
                    connectionLatch.countDown();
                }
            }});

        server = new ProtocolServer(config);
        server.start();
        int port = server.getBoundAddress().getPort();

        // Connect to it from the proxied side
        final ThreadFactory clientThreadFactory = new JBossThreadFactory(new ThreadGroup("ProxiedController-ProtocolServer-threads"), Boolean.FALSE, null, "%G - %t", null, null, AccessController.getContext());
        clientConn = new ManagementRequestConnectionStrategy.EstablishConnectingStrategy(
                InetAddress.getByName("localhost"),
                port,
                5000,
                Executors.newCachedThreadPool(clientThreadFactory),
                clientThreadFactory).getConnection();

        connectionLatch.await();
        System.out.println("connected");

        // Configure client conn to handle requests
        MessageHandler handler = new RemoteModelControllerSetup.SetupManagementHeaderMessageHandler(proxiedController);
        clientConn.setMessageHandler(handler);

        // Configure main side proxy-controller to use main-side of clientConn
        proxyController.setDelegate(RemoteProxyController.create(serverConn, proxyNodeAddress));
    }

    @After
    public void stop() {
        StreamUtils.safeClose(clientConn);
        StreamUtils.safeClose(serverConn);
        server.stop();
        proxyController.setDelegate(null);
    }

    @Override
    protected ProxyController createProxyController(final ModelController proxiedController, final PathAddress proxyNodeAddress) {
        this.proxiedController = proxiedController;
        this.proxyNodeAddress = proxyNodeAddress;
        return proxyController;
    }

    private class TestManagementHeaderMessageHandler extends AbstractMessageHandler {
        @Override
        public void handle(Connection connection, InputStream dataStream) throws IOException {
            throw new IllegalStateException("[JBAS-8930] The request handler on the main side should not be invoked");
        }
    }
}

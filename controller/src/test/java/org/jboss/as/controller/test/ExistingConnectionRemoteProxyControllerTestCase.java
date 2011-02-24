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
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ServerSocketFactory;

import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ProxyController;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.remote.RemoteProxyController;
import org.jboss.as.protocol.Connection;
import org.jboss.as.protocol.ConnectionHandler;
import org.jboss.as.protocol.MessageHandler;
import org.jboss.as.protocol.ProtocolServer;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.as.protocol.mgmt.ManagementHeaderMessageHandler;
import org.jboss.as.protocol.mgmt.ManagementRequestConnectionStrategy;
import org.jboss.dmr.ModelNode;
import org.junit.After;
import org.junit.Before;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class ExistingConnectionRemoteProxyControllerTestCase extends AbstractProxyControllerTest implements ConnectionHandler {

    ModelController proxyController;
    PathAddress proxyNodeAddress;
    DelegatingProxyController testController = new DelegatingProxyController();
    ProtocolServer server;
    CountDownLatch connectionLatch = new CountDownLatch(1);
    Connection clientConn;
    Connection serverConn;

    @Before
    public void start() throws Exception {
        final ProtocolServer.Configuration config = new ProtocolServer.Configuration();
        config.setBindAddress(new InetSocketAddress(InetAddress.getByName("localhost"), 0));
        final AtomicInteger increment = new AtomicInteger();
        config.setThreadFactory(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "Server-" + increment.incrementAndGet());
            }
        });
        config.setReadExecutor(Executors.newCachedThreadPool());
        config.setSocketFactory(ServerSocketFactory.getDefault());
        config.setBacklog(50);
        config.setConnectionHandler(this);

        server = new ProtocolServer(config);
        server.start();
        int port = server.getBoundAddress().getPort();

        Connection clientConn = new ManagementRequestConnectionStrategy.EstablishConnectingStrategy(
                InetAddress.getByName("localhost"),
                port,
                5000,
                Executors.newCachedThreadPool(),
                Executors.defaultThreadFactory()).getConnection();

        connectionLatch.await();
        System.out.println("connected");

        MessageHandler handler = new RemoteModelControllerSetup.SetupManagementHeaderMessageHandler(proxyController);
        clientConn.setMessageHandler(handler);

        testController.setDelegate(RemoteProxyController.create(ModelControllerClient.Type.STANDALONE, serverConn, proxyNodeAddress));
    }

    @After
    public void stop() {
        StreamUtils.safeClose(clientConn);
        server.stop();
        testController.setDelegate(null);
    }

    @Override
    protected ProxyController createProxyController(final ModelController targetController, final PathAddress proxyNodeAddress) {
        this.proxyController = targetController;
        this.proxyNodeAddress = proxyNodeAddress;
        return testController;
    }

    @Override
    public MessageHandler handleConnected(Connection connection) throws IOException {
        serverConn = connection;
        try {
            MessageHandler handler = new TestManagementHeaderMessageHandler();
            return handler;
        } finally {
            connectionLatch.countDown();
        }
    }

    private class TestManagementHeaderMessageHandler extends ManagementHeaderMessageHandler {
        @Override
        protected MessageHandler getHandlerForId(byte handlerId) {
            return new MessageHandler() {

                @Override
                public void handleShutdown(Connection connection) throws IOException {
                }

                @Override
                public void handleMessage(Connection connection, InputStream dataStream)
                        throws IOException {
                }

                @Override
                public void handleFinished(Connection connection) throws IOException {
                }

                @Override
                public void handleFailure(Connection connection, IOException e)
                        throws IOException {
                }
            };
        }
    }

    private static class DelegatingProxyController implements ProxyController {

        ProxyController delegate;

        void setDelegate(ProxyController delegate) {
            this.delegate = delegate;
        }

        @Override
        public OperationResult execute(ModelNode operation, ResultHandler handler) {
            return delegate.execute(operation, handler);
        }

        @Override
        public ModelNode execute(ModelNode operation) throws CancellationException {
            return delegate.execute(operation);
        }

        @Override
        public PathAddress getProxyNodeAddress() {
            return delegate.getProxyNodeAddress();
        }

    }
}

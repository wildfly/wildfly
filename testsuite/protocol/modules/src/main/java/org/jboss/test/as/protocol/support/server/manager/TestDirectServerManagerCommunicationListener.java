/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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
package org.jboss.test.as.protocol.support.server.manager;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.jboss.as.communication.SocketListener.SocketHandler;
import org.jboss.as.host.controller.HostController;
import org.jboss.as.host.controller.DirectServerManagerCommunicationHandler.ShutdownListener;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.logging.Logger;

/**
 * Copies the functionality of the {@link DirectServerManagerCommunicationHandler}
 * but keeps track of handlers by server
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class TestDirectServerManagerCommunicationListener implements ShutdownListener {

    private final Logger log = Logger.getLogger(TestDirectServerManagerCommunicationListener.class);

    private final SocketListener socketListener;

    @SuppressWarnings("unused")
    private final HostController serverManager;

    private final TestServerManagerMessageHandler messageHandler;

    private volatile CountDownLatch newConnectionLatch = new CountDownLatch(1);

    private final Map<String, DirectServerManagerCommunicationHandler> handlers = new ConcurrentHashMap<String, DirectServerManagerCommunicationHandler>();

    private TestDirectServerManagerCommunicationListener(HostController serverManager, InetAddress address, int port, int backlog, TestServerManagerMessageHandler messageHandler) throws IOException {
        this.serverManager = serverManager;
        socketListener = SocketListener.createSocketListener("ServerManager", new ServerAcceptor(), address, port, backlog);
        this.messageHandler = messageHandler;
    }

    public static TestDirectServerManagerCommunicationListener create(HostController serverManager, InetAddress address, int port, int backlog, TestServerManagerMessageHandler messageHandler) throws IOException {
        TestDirectServerManagerCommunicationListener listener = new TestDirectServerManagerCommunicationListener(serverManager, address, port, backlog, messageHandler);
        listener.start();
        return listener;
    }

    void start() throws IOException {
        socketListener.start();
    }

    public void shutdown() {
        socketListener.shutdown();
    }

    public int getSmPort() {
        return socketListener.getPort();
    }


    public DirectServerManagerCommunicationHandler getManagerHandler(String processName) {
        return handlers.get(processName);
    }

    public void resetNewConnectionLatch(int count) {
        newConnectionLatch = new CountDownLatch(count);
    }

    public void waitForNewConnection() throws InterruptedException {
        if (!newConnectionLatch.await(10, TimeUnit.SECONDS))
            throw new RuntimeException("New connection latch timed out");
    }

    @Override
    public void connectionClosed(String processName) {
    }

    class ServerAcceptor implements SocketHandler {

        @Override
        public void initializeConnection(Socket socket) throws IOException, InitialSocketRequestException {
            InputStream in = socket.getInputStream();
            StringBuilder sb = new StringBuilder();

            //TODO Timeout on the read?
            Status status = StreamUtils.readWord(in, sb);
            if (status != Status.MORE) {
                throw new InitialSocketRequestException("Server acceptor: received '" + sb.toString() + "' but no more");
            }
            if (!sb.toString().equals("CONNECTED")) {
                throw new InitialSocketRequestException("Server acceptor: received unknown connect command '" + sb.toString() + "'");
            }
            sb = new StringBuilder();
            while (status == Status.MORE) {
                status = StreamUtils.readWord(in, sb);
            }

            String processName = sb.toString();

            log.infof("Server acceptor: connected server %s", processName);
            DirectServerManagerCommunicationHandler handler = DirectServerManagerCommunicationHandler.create(SocketConnection.accepted(socket), processName, messageHandler, TestDirectServerManagerCommunicationListener.this);
            handlers.put(processName, handler);

            newConnectionLatch.countDown();
        }
    }
}

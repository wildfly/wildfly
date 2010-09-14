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
package org.jboss.as.server.manager;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;

import org.jboss.as.communication.InitialSocketRequestException;
import org.jboss.as.communication.SocketConnection;
import org.jboss.as.communication.SocketListener;
import org.jboss.as.communication.SocketListener.SocketHandler;
import org.jboss.as.process.Status;
import org.jboss.as.process.StreamUtils;
import org.jboss.logging.Logger;

/**
 * Contains the server socket listening for requests from the
 * server processes
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
class DirectServerCommunicationListener{
    private final Logger log = Logger.getLogger(DirectServerCommunicationListener.class);

    private final SocketListener socketListener;

    private final ServerManager serverManager;

    DirectServerCommunicationListener(ServerManager serverManager, InetAddress address, int port, int backlog) throws IOException {
        this.serverManager = serverManager;
        socketListener = SocketListener.createSocketListener("ServerManager", new ServerAcceptor(), address, port, backlog);
    }

    static DirectServerCommunicationListener create(ServerManager serverManager, InetAddress address, int port, int backlog) throws IOException {
        DirectServerCommunicationListener listener = new DirectServerCommunicationListener(serverManager, address, port, backlog);
        listener.start();
        return listener;
    }

    void start() throws IOException {
        socketListener.start();
    }

    void shutdown() {
        socketListener.shutdown();
    }

    int getSmPort() {
        return socketListener.getPort();
    }

    InetAddress getSmAddress() {
        return socketListener.getAddress();
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

            Server server = serverManager.getServer(processName);

            if (server == null) {
                throw new InitialSocketRequestException("Server acceptor: unknown server " + processName);
            }
            server.setCommunicationHandler(DirectServerCommunicationHandler.create(SocketConnection.accepted(socket), processName, new MessageHandler(serverManager), serverManager));
        }
    }
}

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
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.as.communication.SocketConnection;
import org.jboss.as.process.StreamUtils;
import org.jboss.as.process.ProcessManagerSlave.Handler;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class DirectServerCommunicationHandler implements ServerCommunicationHandler{
    private final SocketConnection socketConnection;
    private final OutputStream output;
    private final InputStream input;
    private final String serverName;
    private final Handler messageHandler;
    private final InputStreamHandler inputHandler;
    private final ShutdownListener shutdownListener;

    protected DirectServerCommunicationHandler(SocketConnection socketConnection, String serverName, Handler messageHandler, ShutdownListener shutdownListener) {
        this.socketConnection = socketConnection;
        this.output = socketConnection.getOutputStream();
        this.input = socketConnection.getInputStream();
        this.serverName = serverName;
        this.messageHandler = messageHandler;
        this.inputHandler = new InputStreamHandler();
        this.shutdownListener = shutdownListener;
    }

    static DirectServerCommunicationHandler create(SocketConnection socketConnection, String serverName, Handler messageHandler, ShutdownListener shutdownListener) {
        DirectServerCommunicationHandler handler = new DirectServerCommunicationHandler(socketConnection, serverName, messageHandler, shutdownListener);
        handler.start();
        return handler;
    }

    protected void start() {
        Thread t = new Thread(inputHandler);
        t.start();
    }

    @Override
    public void sendMessage(List<String> message) throws IOException {
        throw new IllegalArgumentException("Only byte messages are supported");
    }

    @Override
    public void sendMessage(byte[] message) throws IOException {
        synchronized (output) {
            StreamUtils.writeInt(output, message.length);
            output.write(message, 0, message.length);
            output.flush();
        }
    }

    protected void shutdown() {
        inputHandler.shutdown();
    }

    protected boolean isClosed() {
        return !socketConnection.isOpen();
    }

    class InputStreamHandler implements Runnable {
        AtomicBoolean shutdown = new AtomicBoolean();

        @Override
        public void run() {
            try {
                while (!shutdown.get()) {
                    byte[] bytes = StreamUtils.readBytesWithLength(input);
                    messageHandler.handleMessage(serverName, bytes);
                }
            } catch (IOException e) {
            } finally {
                shutdown();
            }
        }

        void shutdown() {
            if (!shutdown.getAndSet(true)) {
                socketConnection.close();
                if (shutdownListener != null)
                    shutdownListener.connectionClosed(serverName);
            }
        }
    }

    public interface ShutdownListener {
        void connectionClosed(String processName);
    }
}

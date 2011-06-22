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

package org.jboss.as.protocol.old;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;

import org.jboss.as.protocol.old.Connection.ClosedCallback;
import org.jboss.logging.Logger;

import javax.net.SocketFactory;

/**
 * A protocol client for management commands, which can also asynchronously receive protocol messages.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ProtocolClient {
    private static final Logger log = Logger.getLogger("org.jboss.as.protocol.client");

    private final ThreadFactory threadFactory;
    private final SocketFactory socketFactory;
    private final InetSocketAddress serverAddress;
    private final MessageHandler messageHandler;
    private final InetSocketAddress bindAddress;
    private final int connectTimeout;
    private final int readTimeout;
    private final Executor readExecutor;
    private final ClosedCallback callback;

    public ProtocolClient(final Configuration configuration) {
        threadFactory = configuration.getThreadFactory();
        bindAddress = configuration.getBindAddress();
        connectTimeout = configuration.getConnectTimeout();
        socketFactory = configuration.getSocketFactory();
        messageHandler = configuration.getMessageHandler();
        serverAddress = configuration.getServerAddress();
        readTimeout = configuration.getReadTimeout();
        readExecutor = configuration.getReadExecutor();
        callback = configuration.getClosedCallback();
        if (threadFactory == null) {
            throw new IllegalArgumentException("threadFactory is null");
        }
        if (socketFactory == null) {
            throw new IllegalArgumentException("factory is null");
        }
        if (serverAddress == null) {
            throw new IllegalArgumentException("serverAddress is null");
        }
        if (messageHandler == null) {
            throw new IllegalArgumentException("handler is null");
        }
        if (readExecutor == null) {
            throw new IllegalArgumentException("readExecutor is null");
        }
    }

    public Connection connect() throws IOException {
        log.tracef("Creating connection to %s", serverAddress);
        final Socket socket = socketFactory.createSocket();
        final ConnectionImpl connection = new ConnectionImpl(socket, messageHandler, readExecutor, callback);
        final Thread thread = threadFactory.newThread(connection.getReadTask());
        if (thread == null) {
            throw new IllegalStateException("Thread creation was refused");
        }
        if (bindAddress != null) socket.bind(bindAddress);
        if (readTimeout != 0) socket.setSoTimeout(readTimeout);
        socket.connect(serverAddress, connectTimeout);
        thread.setName("Read thread for " + serverAddress);
        thread.start();
        log.tracef("Connected to %s", serverAddress);
        return connection;
    }

    public static final class Configuration {
        private ThreadFactory threadFactory;
        private SocketFactory socketFactory;
        private InetSocketAddress serverAddress;
        private MessageHandler messageHandler;
        private InetSocketAddress bindAddress;
        private Executor readExecutor;
        private int connectTimeout = 0;
        private int readTimeout = 0;
        private ClosedCallback closedCallback;

        public Configuration() {
        }

        public ThreadFactory getThreadFactory() {
            return threadFactory;
        }

        public void setThreadFactory(final ThreadFactory threadFactory) {
            this.threadFactory = threadFactory;
        }

        public SocketFactory getSocketFactory() {
            return socketFactory;
        }

        public void setSocketFactory(final SocketFactory socketFactory) {
            this.socketFactory = socketFactory;
        }

        public InetSocketAddress getServerAddress() {
            return serverAddress;
        }

        public void setServerAddress(final InetSocketAddress serverAddress) {
            this.serverAddress = serverAddress;
        }

        public MessageHandler getMessageHandler() {
            return messageHandler;
        }

        public void setMessageHandler(final MessageHandler messageHandler) {
            this.messageHandler = messageHandler;
        }

        public InetSocketAddress getBindAddress() {
            return bindAddress;
        }

        public void setBindAddress(final InetSocketAddress bindAddress) {
            this.bindAddress = bindAddress;
        }

        public Executor getReadExecutor() {
            return readExecutor;
        }

        public void setReadExecutor(final Executor readExecutor) {
            this.readExecutor = readExecutor;
        }

        public int getConnectTimeout() {
            return connectTimeout;
        }

        public void setConnectTimeout(final int connectTimeout) {
            this.connectTimeout = connectTimeout;
        }

        public int getReadTimeout() {
            return readTimeout;
        }

        public void setReadTimeout(final int readTimeout) {
            this.readTimeout = readTimeout;
        }

        public ClosedCallback getClosedCallback() {
            return closedCallback;
        }

        public void setClosedCallback(ClosedCallback closedCallback) {
            this.closedCallback = closedCallback;
        }
    }
}

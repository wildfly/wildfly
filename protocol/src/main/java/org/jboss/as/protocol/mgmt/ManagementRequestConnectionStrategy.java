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

package org.jboss.as.protocol.mgmt;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import javax.net.SocketFactory;
import org.jboss.as.protocol.Connection;
import org.jboss.as.protocol.MessageHandler;
import org.jboss.as.protocol.ProtocolClient;
import static org.jboss.as.protocol.StreamUtils.safeClose;

/**
 * Strategy used to retrieve a connection for {@link org.jboss.as.protocol.mgmt.ManagementRequest}.
 *
 * @author John Bailey
 */
public interface ManagementRequestConnectionStrategy {
    /**
     * Get the connection for the request.
     *
     * @return The connection
     * @throws IOException If any problems occure getting the connection.
     */
    Connection getConnection() throws IOException;

    /**
     * Called when the request is complete.  This can be used to cleanup or close the connection.
     */
    void complete();

    /**
     * Strategy that uses an existing connection.
     */
    static class ExistingConnectionStrategy implements ManagementRequestConnectionStrategy {
        private final Connection connection;

        public ExistingConnectionStrategy(Connection connection) {
            this.connection = connection;
        }

        /** {@inheritDoc} */
        public Connection getConnection() throws IOException {
            return connection;
        }

        /** {@inheritDoc} */
        public void complete() {
            // NOOP
        }
    }

    /**
     * Strategy that establishes a new connection.
     */
    static class EstablishConnectingStrategy implements ManagementRequestConnectionStrategy {
        private final InetAddress address;
        private final int port;
        private final long connectTimeout;
        private final ExecutorService executorService;
        private final ThreadFactory threadFactory;
        private Connection connection;

        public EstablishConnectingStrategy(final InetAddress address, final int port, final long connectTimeout, final ExecutorService executorService, final ThreadFactory threadFactory) {
            this.address = address;
            this.port = port;
            this.connectTimeout = connectTimeout;
            this.executorService = executorService;
            this.threadFactory = threadFactory;
        }

        /** {@inheritDoc} */
        public synchronized Connection getConnection() throws IOException {
            if (connection == null) {
                final int timeout = (int) TimeUnit.SECONDS.toMillis(connectTimeout);

                final ProtocolClient.Configuration config = new ProtocolClient.Configuration();
                config.setMessageHandler(MessageHandler.NULL);
                config.setConnectTimeout(timeout);
                config.setReadExecutor(executorService);
                config.setSocketFactory(SocketFactory.getDefault());
                config.setServerAddress(new InetSocketAddress(address, port));
                config.setThreadFactory(threadFactory);

                final ProtocolClient protocolClient = new ProtocolClient(config);
                connection = protocolClient.connect();
            }
            return connection;
        }

        /** {@inheritDoc} */
        public synchronized void complete() {
            safeClose(connection);
        }
    }
}

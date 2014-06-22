/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.domain.management.util;

import org.jboss.as.protocol.ProtocolChannelClient;
import org.jboss.as.protocol.mgmt.ManagementChannelAssociation;
import org.jboss.as.protocol.mgmt.ManagementChannelHandler;
import org.jboss.as.protocol.mgmt.ManagementClientChannelStrategy;
import org.jboss.remoting3.Channel;
import org.jboss.remoting3.CloseHandler;
import org.jboss.remoting3.Connection;
import org.xnio.IoFuture;
import org.xnio.OptionMap;

import javax.security.auth.callback.CallbackHandler;
import java.io.Closeable;
import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Connection utility allowing the {@linkplain DomainLifecycleUtil} to share a remoting connection between potentially
 * multiple controller clients, each using it's own channel.
 *
 * @author Emanuel Muckenhuber
 */
class DomainTestConnection implements Closeable {

    private static final String DEFAULT_CHANNEL_SERVICE_TYPE = "management";

    private final ProtocolChannelClient client;
    private final CallbackHandler callbackHandler;
    private final ExecutorService executorService;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    volatile Connection connection;

    DomainTestConnection(final ProtocolChannelClient client, final CallbackHandler callbackHandler, final ExecutorService executorService) {
        this.client = client;
        this.callbackHandler = callbackHandler;
        this.executorService = executorService;
    }

    /**
     * Create a controller client instance.
     *
     * @return the controller client
     */
    protected DomainTestClient createClient() {
        return createClient(executorService);
    }

    /**
     * Create a controller client instance.
     *
     * @param executorService the executor service
     * @return the controller client
     */
    protected DomainTestClient createClient(final ExecutorService executorService) {
        final ChannelStrategy strategy = new ChannelStrategy(executorService);
        final ManagementChannelHandler handler = strategy.handler;
        final DomainTestClient client = new DomainTestClient() {
            @Override
            Connection getConnection() {
                return connection;
            }

            @Override
            protected ManagementChannelAssociation getChannelAssociation() throws IOException {
                return handler;
            }

            @Override
            Channel getChannel() {
                return strategy.channel;
            }

            @Override
            public void close() throws IOException {
                strategy.close();
            }
        };
        handler.addHandlerFactory(client);
        return client;
    }

    /**
     * Try to connect to the remote controller.
     *
     * @return the underlying remoting connection
     * @throws IOException
     */
    protected Connection connect() throws IOException {
        return connect(callbackHandler);
    }

    /**
     * Try to connect to the remote host controller.
     *
     * @param callbackHandler the security callback handler
     * @return the underlying remoting connection
     * @throws IOException
     */
    protected Connection connect(final CallbackHandler callbackHandler) throws IOException {
        if(closed.get()) {
            throw new IllegalStateException();
        }
        synchronized (this) {
            if(isConnected()) {
                return connection;
            }
            connection = client.connectSync(callbackHandler, Collections.<String, String>emptyMap(), null);
            connection.addCloseHandler(new CloseHandler<Connection>() {
                @Override
                public void handleClose(Connection old, IOException exception) {
                    synchronized (DomainTestConnection.this) {
                        if(connection == old) {
                            connection = null;
                        }
                        DomainTestConnection.this.notifyAll();
                    }
                }
            });
            return connection;
        }
    }

    /**
     * Check if we are currently connected.
     *
     * @return
     */
    protected boolean isConnected() {
        return connection != null;
    }

    /**
     * Disconnect from the remote controller, allowing reconnection.
     *
     * @throws IOException
     */
    protected void disconnect() throws IOException {
        final Connection connection = this.connection;
        if(connection != null) {
            connection.close();
        }
    }

    /**
     * Await the connection the be closed.
     *
     * @param ref the referenced connection
     * @throws InterruptedException
     */
    protected void awaitConnectionClosed(final Connection ref) throws InterruptedException {
        synchronized (this) {
            for(;;) {
                final Connection connection = this.connection;
                if(connection == null) {
                    return;
                } else if (connection != ref) {
                    return;
                }
                wait();
            }
        }
    }

    @Override
    public void close() throws IOException {
        synchronized (this) {
            if(closed.compareAndSet(false, true)) {
                final Connection connection = this.connection;
                if(connection != null) {
                    connection.close();
                }
            }
        }
    }

    class ChannelStrategy extends ManagementClientChannelStrategy {

        volatile Channel channel;

        private final ManagementChannelHandler handler;
        ChannelStrategy(ExecutorService executorService) {
            this.handler = new ManagementChannelHandler(this, executorService);
        }

        @Override
        public Channel getChannel() throws IOException {
            if(channel == null) {
                synchronized (this) {
                    if(channel == null) {
                        final Connection connection = connect();
                        channel = openChannel(connection);
                        channel.receiveMessage(handler.getReceiver());
                    }
                }
            }
            return channel;
        }

        Channel openChannel(final Connection connection) throws IOException {
            final IoFuture<Channel> future = connection.openChannel(DEFAULT_CHANNEL_SERVICE_TYPE, OptionMap.EMPTY);
            final Channel channel = future.get();
            channel.addCloseHandler(new CloseHandler<Channel>() {
                @Override
                public void handleClose(final Channel old, final IOException e) {
                    synchronized (ChannelStrategy.this) {
                        if(ChannelStrategy.this.channel == old) {
                            ChannelStrategy.this.channel = null;
                        }
                    }
                    handler.handleChannelClosed(old, e);
                }
            });
            return channel;
        }

        @Override
        public void close() throws IOException {
            final Channel channel = this.channel;
            if(channel != null) {
                channel.close();
            }
        }
    }

}

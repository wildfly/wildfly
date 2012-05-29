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

package org.jboss.as.protocol.mgmt;

import org.jboss.as.protocol.ProtocolConnectionConfiguration;
import org.jboss.as.protocol.ProtocolConnectionManager;
import org.jboss.as.protocol.ProtocolMessages;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.remoting3.Channel;
import org.jboss.remoting3.CloseHandler;
import org.jboss.remoting3.Connection;
import org.xnio.IoFuture;
import org.xnio.OptionMap;

import java.io.IOException;

/**
 * Base class for a connecting {@code ManagementClientChannelStrategy}.
 *
 * @author Emanuel Muckenhuber
 */
public abstract class FutureManagementChannel extends ManagementClientChannelStrategy implements ProtocolConnectionManager.ConnectionOpenHandler {

    private final Object lock = new Object();
    private volatile Channel channel;
    private volatile boolean closed;

    @Override
    public Channel getChannel() throws IOException {
        final Channel channel = this.channel;
        if(channel == null && closed) {
            throw ProtocolMessages.MESSAGES.channelClosed();
        }
        return channel;
    }

    @Override
    public void close() throws IOException {
        synchronized (lock) {
            if(closed) {
                return;
            }
            closed = true;
            StreamUtils.safeClose(channel);
            lock.notifyAll();
        }
    }

    /**
     * Check if connected.
     *
     * @return {@code true} if the connection is open, {@code false} otherwise
     */
    protected boolean isConnected() {
        return channel != null && !closed;
    }

    /**
     * Get the underlying channel. This may block until the channel is set.
     *
     * @return the channel
     * @throws IOException for any error
     */
    protected Channel awaitChannel() throws IOException {
        Channel channel = this.channel;
        if(channel != null) {
            return channel;
        }
        synchronized (lock) {
            for(;;) {
                if(closed) {
                    throw ProtocolMessages.MESSAGES.channelClosed();
                }
                channel = this.channel;
                if(channel != null) {
                    return channel;
                }
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    throw new IOException(e);
                }
            }
        }
    }

    /**
     * Open a channel.
     *
     * @param connection the connection
     * @param serviceType the service type
     * @param options the channel options
     * @return the opened channel
     * @throws IOException
     */
    protected Channel openChannel(final Connection connection, final String serviceType, final OptionMap options) throws IOException {
        final IoFuture<Channel> futureChannel = connection.openChannel(serviceType, options);
        return futureChannel.get();
    }

    /**
     * Set the channel. This will return whether the channel could be set successfully or not.
     *
     * @param newChannel the channel
     * @return whether the operation succeeded or not
     */
    protected boolean setChannel(final Channel newChannel) {
        if(newChannel == null) {
            return false;
        }
        synchronized (lock) {
            if(closed || channel != null) {
                return false;
            }
            this.channel = newChannel;
            this.channel.addCloseHandler(new CloseHandler<Channel>() {
                @Override
                public void handleClose(final Channel closed, final IOException exception) {
                    synchronized (lock) {
                        if(FutureManagementChannel.this.channel == closed) {
                            FutureManagementChannel.this.channel = null;
                        }
                        lock.notifyAll();
                    }
                }
            });
            lock.notifyAll();
            return true;
        }
    }

    /**
     * A connecting channel strategy.
     */
    static class Establishing extends FutureManagementChannel {

        private final String serviceType;
        private final OptionMap channelOptions;
        private final Channel.Receiver receiver;
        private final ProtocolConnectionManager connectionManager;

        Establishing(final String serviceType, final Channel.Receiver receiver, final ProtocolConnectionConfiguration configuration) {
            this.serviceType = serviceType;
            this.receiver = receiver;
            this.channelOptions = configuration.getOptionMap();
            this.connectionManager = ProtocolConnectionManager.create(configuration, this);
        }

        @Override
        public Channel getChannel() throws IOException {
            final Channel channel = super.getChannel();
            if(channel != null) {
                return channel;
            }
            // Try to connect and wait for the channel
            connectionManager.connect();
            return awaitChannel();
        }

        @Override
        public void connectionOpened(final Connection connection) throws IOException {
            final Channel channel = openChannel(connection, serviceType, channelOptions);
            if(setChannel(channel)) {
                channel.receiveMessage(receiver);
            } else {
                channel.closeAsync();
            }
        }

        @Override
        public void close() throws IOException {
            try {
                connectionManager.shutdown();
            } finally {
                super.close();
            }
        }

    }

}

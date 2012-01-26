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
package org.jboss.as.protocol.mgmt;

import java.io.Closeable;
import java.io.DataInput;
import java.io.IOException;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.security.auth.callback.CallbackHandler;

import org.jboss.as.protocol.ProtocolChannelClient;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.remoting3.Channel;
import org.jboss.remoting3.CloseHandler;
import org.jboss.remoting3.Connection;
import org.xnio.OptionMap;

/**
 * Strategy management clients can use for controlling the lifecycle of the channel.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author Emanuel Muckenhuber
 */
public abstract class ManagementClientChannelStrategy implements Closeable {

    /** The remoting channel service type. */
    private static final String DEFAULT_CHANNEL_SERVICE_TYPE = "management";

    /**
     * Get the channel.
     *
     * @return the channel
     * @throws IOException
     */
    public abstract Channel getChannel() throws IOException;

    /**
     * Create a new client channel strategy.
     *
     * @param channel the existing channel
     * @return the management client channel strategy
     */
    public static ManagementClientChannelStrategy create(final Channel channel) {
        return new Existing(channel);
    }

    /**
     * Create a new establishing management client channel-strategy
     *
     * @param setup the remoting setup
     * @param handler the {@code ManagementMessageHandler}
     * @param cbHandler a callback handler
     * @param saslOptions the sasl options
     * @param closeHandler a close handler
     * @return the management client channel strategy
     * @throws IOException
     */
    public static ManagementClientChannelStrategy create(final ProtocolChannelClient setup,
                                                   final ManagementMessageHandler handler,
                                                   final CallbackHandler cbHandler,
                                                   final Map<String, String> saslOptions,
                                                   final SSLContext sslContext,
                                                   final CloseHandler<Channel> closeHandler) throws IOException {
        return new Establishing(DEFAULT_CHANNEL_SERVICE_TYPE, setup, saslOptions, cbHandler, sslContext, handler, closeHandler);
    }

    /**
     * The existing channel strategy.
     */
    private static class Existing extends ManagementClientChannelStrategy {
        // The underlying channel
        private final Channel channel;

        private Existing(final Channel channel) {
            this.channel = channel;
        }

        @Override
        public Channel getChannel() {
            return channel;
        }

        @Override
        public void close() {
            // closing is not our responsibility
        }
    }

    /**
     * When getting the underlying channel this strategy is trying to automatically (re-)connect
     * when either the connection or channel was closed.
     */
    private static class Establishing extends ManagementClientChannelStrategy {

        private final String channelName;
        private final Map<String,String> saslOptions;
        private final CallbackHandler callbackHandler;
        private final SSLContext sslContext;
        private final Channel.Receiver receiver;
        private final ProtocolChannelClient setup;
        private final CloseHandler<Channel> closeHandlerDelegate;

        volatile Connection connection;
        volatile Channel channel;

        public Establishing(final String channelName, final ProtocolChannelClient setup, final Map<String, String> saslOptions,
                            final CallbackHandler callbackHandler, final SSLContext sslContext, final ManagementMessageHandler handler,
                            final CloseHandler<Channel> closeHandler) {
            this.channelName = channelName;
            this.saslOptions = saslOptions;
            this.sslContext = sslContext;
            this.setup = setup;
            this.callbackHandler = callbackHandler;
            this.closeHandlerDelegate = closeHandler;
            // Basic management channel receiver, which delegates messages to a {@code ManagementMessageHandler}
            // Additionally legacy bye-bye messages result in resetting the current channel
            this.receiver = new ManagementChannelReceiver() {

                @Override
                public void handleMessage(final Channel channel, final DataInput input, final ManagementProtocolHeader header) throws IOException {
                    handler.handleMessage(channel, input, header);
                }

                @Override
                protected void handleChannelReset(Channel channel) {
                    resetChannel(channel);
                }

            };
        }

        @Override
        public Channel getChannel() throws IOException {
            boolean ok = false;
            try {
                synchronized (this) {
                    if (connection == null) {
                        // Connect with the configured timeout
                        this.connection = setup.connectSync(callbackHandler, saslOptions, sslContext);
                        this.connection.addCloseHandler(new CloseHandler<Connection>() {
                            @Override
                            public void handleClose(Connection closed, IOException exception) {
                                synchronized (Establishing.this) {
                                    if(connection == closed) {
                                        connection = null;
                                    }
                                }
                            }
                        });
                    }
                    if (channel == null) {
                        channel = connection.openChannel(channelName, OptionMap.EMPTY).get();
                        channel.addCloseHandler(new CloseHandler<Channel>() {
                            @Override
                            public void handleClose(Channel closed, IOException exception) {
                                synchronized (Establishing.this) {
                                    if(channel == closed) {
                                        channel = null;
                                    }
                                }
                                if(closeHandlerDelegate != null) {
                                    closeHandlerDelegate.handleClose(closed, exception);
                                }
                            }
                        });
                        channel.receiveMessage(receiver);
                    }
                    ok = true;
                }
            } finally {
                if (! ok) {
                    StreamUtils.safeClose(connection);
                    StreamUtils.safeClose(channel);
                }
            }
            return channel;
        }

        private void resetChannel(final Channel old) {
            boolean reset = false;
            synchronized (this) {
                if(channel == old) {
                    channel = null;
                    reset = true;
                }
            }
            // Since this is used by older clients to signal that they are about to close the channel
            // we just close it to make sure that we don't leak it
            if(reset) {
                old.closeAsync();
            }
        }

        @Override
        public void close() {
            StreamUtils.safeClose(channel);
            StreamUtils.safeClose(connection);
        }
    }

}

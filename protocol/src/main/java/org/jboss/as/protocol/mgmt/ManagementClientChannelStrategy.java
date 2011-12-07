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
import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.util.Map;

import javax.security.auth.callback.CallbackHandler;

import org.jboss.as.protocol.ProtocolChannelClient;
import org.jboss.as.protocol.ProtocolChannelSetup;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.remoting3.Channel;
import org.jboss.remoting3.CloseHandler;
import org.jboss.remoting3.Connection;
import org.jboss.remoting3.Endpoint;
import org.xnio.IoUtils;
import org.xnio.OptionMap;

/**
 * Strategy management clients can use for controlling the lifecycle of the channel.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public abstract class ManagementClientChannelStrategy implements Closeable {

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
     * @param receiver the channel receiver
     * @param cbHandler a callback handler
     * @param saslOptions the sasl options
     * @return the management client channel strategy
     * @throws IOException
     */
    public static ManagementClientChannelStrategy create(final ProtocolChannelSetup setup,
                                                   final Channel.Receiver receiver,
                                                   final CallbackHandler cbHandler,
                                                   final Map<String, String> saslOptions) throws IOException {
        return new Establishing("management", setup, saslOptions, cbHandler, receiver);
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
        public Channel getChannel() throws IOException {
            return channel;
        }

        @Override
        public void close() throws IOException {
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
        private final Channel.Receiver receiver;
        private final ProtocolChannelSetup setup;

        volatile Connection connection;
        volatile Channel channel;

        public Establishing(String channelName, final ProtocolChannelSetup setup, Map<String, String> saslOptions, CallbackHandler callbackHandler, final Channel.Receiver receiver) {
            this.channelName = channelName;
            this.receiver = receiver;
            this.saslOptions = saslOptions;
            this.setup = setup;
            this.callbackHandler = callbackHandler;
        }

        @Override
        public Channel getChannel() throws IOException {
            boolean ok = false;
            try {
                synchronized (this) {
                    if (connection == null) {
                        this.connection = setup.connect(callbackHandler, saslOptions).get();
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

        @Override
        public void close() throws IOException {
            StreamUtils.safeClose(channel);
            StreamUtils.safeClose(connection);
        }
    }

}

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
import org.jboss.as.protocol.ProtocolConnectionConfiguration;
import org.jboss.as.protocol.ProtocolMessages;
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
     * @param sslContext the ssl context
     * @param closeHandler a close handler
     * @return the management client channel strategy
     */
    public static ManagementClientChannelStrategy create(final ProtocolChannelClient setup,
                                                   final ManagementMessageHandler handler,
                                                   final CallbackHandler cbHandler,
                                                   final Map<String, String> saslOptions,
                                                   final SSLContext sslContext,
                                                   final CloseHandler<Channel> closeHandler) {
        return create(createConfiguration(setup.getConfiguration(), saslOptions, cbHandler, sslContext), ManagementChannelReceiver.createDelegating(handler), closeHandler);
    }

    /**
     * Create a new establishing management client channel-strategy
     *
     * @param configuration the connection configuration
     * @param receiver the channel receiver
     * @param closeHandler the close handler
     * @return the management client channel strategy
     */
    public static ManagementClientChannelStrategy create(final ProtocolConnectionConfiguration configuration, final Channel.Receiver receiver, final CloseHandler<Channel> closeHandler) {
        return new Establishing(configuration, receiver, closeHandler);
    }

    /**
     * The existing channel strategy.
     */
    private static class Existing extends ManagementClientChannelStrategy {
        // The underlying channel
        private final Channel channel;
        private volatile boolean closed = false;
        private Existing(final Channel channel) {
            this.channel = channel;
        }

        @Override
        public Channel getChannel() throws IOException {
            if(closed) {
                throw ProtocolMessages.MESSAGES.channelClosed();
            }
            return channel;
        }

        @Override
        public void close() {
            this.closed = true;
            // closing the channel is not our responsibility
        }
    }

    private static ProtocolConnectionConfiguration createConfiguration(final ProtocolConnectionConfiguration configuration,
                                                                       final Map<String, String> saslOptions, final CallbackHandler callbackHandler,
                                                                       final SSLContext sslContext) {
        final ProtocolConnectionConfiguration config = ProtocolConnectionConfiguration.copy(configuration);
        config.setCallbackHandler(callbackHandler);
        config.setSslContext(sslContext);
        config.setSaslOptions(saslOptions);
        return config;
    }

    /**
     * When getting the underlying channel this strategy is trying to automatically (re-)connect
     * when either the connection or channel was closed.
     */
    private static class Establishing extends FutureManagementChannel.Establishing {

        private final CloseHandler<Channel> closeHandler;
        private Establishing(final ProtocolConnectionConfiguration configuration, final Channel.Receiver receiver, final CloseHandler<Channel> closeHandler) {
            super(DEFAULT_CHANNEL_SERVICE_TYPE, receiver, configuration);
            this.closeHandler = closeHandler;
        }

        @Override
        protected Channel openChannel(final Connection connection, final String serviceType, final OptionMap options) throws IOException {
            final Channel channel = super.openChannel(connection, serviceType, options);
            channel.addCloseHandler(closeHandler);
            return channel;
        }

    }

}

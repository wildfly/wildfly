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

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.util.Map;

import javax.security.auth.callback.CallbackHandler;

import org.jboss.as.protocol.ProtocolChannelClient;
import org.jboss.remoting3.Channel;
import org.jboss.remoting3.Endpoint;
import org.xnio.IoUtils;

/**
 * Strategy {@link ManagementChannel} clients can use for controlling the lifecycle of the channel.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public abstract class ManagementClientChannelStrategy {

    public abstract ManagementChannel getChannel() throws IOException;

    public abstract void requestDone();

    public static synchronized ManagementClientChannelStrategy create(final Channel channel) {
        return new Existing(new ManagementChannel("no clue", channel));
    }

    public static synchronized ManagementClientChannelStrategy create(final ManagementChannel channel) {
        return new Existing(channel);
    }

    public static ManagementClientChannelStrategy create(String hostName, int port, final Endpoint endpoint,
                                                         final ChannelReceiverFactory receiverFactory,
                                                         final CallbackHandler cbHandler,
                                                         final Map<String, String> saslOptions) throws IOException {
        return new Establishing(hostName, port, endpoint, receiverFactory, cbHandler, saslOptions);
    }

    private static class Existing extends ManagementClientChannelStrategy {
        private final ManagementChannel channel;

        Existing(final ManagementChannel channel) {
            this.channel = channel;
        }

        @Override
        public ManagementChannel getChannel() throws IOException {
            return channel;
        }

        @Override
        public void requestDone() {
        }
    }

    private static class Establishing extends ManagementClientChannelStrategy {

        private final Endpoint endpoint;
        private final String hostName;
        private final int port;
        private final ChannelReceiverFactory receiverFactory;
        private volatile ProtocolChannelClient<ManagementChannel> client;
        private volatile ManagementChannel channel;
        private final CallbackHandler callbackHandler;
        private final Map<String,String> saslOptions;

        public Establishing(final String hostName, final int port, final Endpoint endpoint,
                            final ChannelReceiverFactory receiverFactory, final CallbackHandler callbackHandler,
                            final Map<String, String> saslOptions) {
            this.hostName = hostName;
            this.port = port;
            this.endpoint = endpoint;
            this.receiverFactory = receiverFactory;
            this.callbackHandler = callbackHandler;
            this.saslOptions = saslOptions;
        }

        @Override
        public ManagementChannel getChannel() throws IOException {

            final ProtocolChannelClient.Configuration<ManagementChannel> configuration = new ProtocolChannelClient.Configuration<ManagementChannel>();
            try {
                configuration.setEndpoint(endpoint);
                configuration.setUri(new URI("remote://" + hostName +  ":" + port));
                configuration.setChannelFactory(new ManagementChannelFactory(receiverFactory.createReceiver()));
                client = ProtocolChannelClient.create(configuration);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            boolean ok = false;
            try {
                try {
                    client.connect(callbackHandler, saslOptions);
                } catch (ConnectException e) {
                    throw e;
                }
                channel = client.openChannel("management");
                channel.startReceiving();
                ok = true;
            } finally {
                if (!ok) {
                    IoUtils.safeClose(channel);
                    IoUtils.safeClose(client);
                }
            }
            return channel;
        }

        @Override
        public void requestDone() {
            IoUtils.safeClose(client);
        }
    }

    public interface ChannelReceiverFactory {

        Channel.Receiver createReceiver();

    }

}

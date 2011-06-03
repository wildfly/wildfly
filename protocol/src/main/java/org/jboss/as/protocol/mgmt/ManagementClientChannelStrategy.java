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
import java.net.URISyntaxException;
import java.util.concurrent.ExecutorService;

import org.jboss.as.protocol.ProtocolChannel;
import org.jboss.as.protocol.ProtocolChannelClient;
import org.xnio.IoUtils;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public abstract class ManagementClientChannelStrategy {

    public abstract ProtocolChannel getChannel();

    public abstract void requestDone();

    public static synchronized ManagementClientChannelStrategy create(final ProtocolChannel channel) {
        return new Existing(channel);
    }

    public static ManagementClientChannelStrategy create(String hostName, int port, final ExecutorService executorService, final ManagementOperationHandler handler) throws URISyntaxException, IOException {
        return new Establishing(hostName, port, executorService, handler);
    }

    private static class Existing extends ManagementClientChannelStrategy {
        private final ProtocolChannel channel;

        Existing(final ProtocolChannel channel) {
            this.channel = channel;
        }

        @Override
        public ProtocolChannel getChannel() {
            return channel;
        }

        @Override
        public void requestDone() {
        }
    }

    private static class Establishing extends ManagementClientChannelStrategy {
        private final String hostName;
        private final int port;
        private final ExecutorService executorService;
        private final ManagementOperationHandler handler;
        private volatile ProtocolChannelClient client;
        private volatile ProtocolChannel channel;

        public Establishing(String hostName, int port, final ExecutorService executorService, final ManagementOperationHandler handler) {
            this.hostName = hostName;
            this.port = port;
            this.executorService = executorService;
            this.handler = handler;
        }

        @Override
        public ProtocolChannel getChannel() {
            try {
                final ProtocolChannelClient.Configuration configuration = new ProtocolChannelClient.Configuration();
                configuration.setEndpointName("endpoint");
                configuration.setUriScheme("remote");
                configuration.setUri(new URI("remote://" + hostName +  ":" + port));
                configuration.setExecutor(executorService);
                configuration.setChannelReceiverFactory(new ManagementChannelReceiverFactory());
                client = ProtocolChannelClient.create(configuration);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            //Try reconnecting a few times
            for (int i = 0 ; i < 20 ; i++) {
                try {
                    client.connect();
                    break;
                } catch (ConnectException e) {
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            try {
                channel = client.openChannel("management");
                channel.getReceiver(ManagementChannelReceiver.class).setOperationHandler(handler);
                channel.startReceiving();
                return channel;
            } catch (IOException e) {
                // AutoGenerated
                throw new RuntimeException(e);
            }
        }

        @Override
        public void requestDone() {
            IoUtils.safeClose(channel);
            IoUtils.safeClose(client);
        }
    }
}

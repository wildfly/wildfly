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

import org.jboss.as.protocol.ProtocolChannelClient;
import org.jboss.as.protocol.ProtocolChannelClient.Configuration;
import org.jboss.remoting3.Endpoint;
import org.xnio.IoUtils;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public abstract class ManagementClientChannelStrategy {

    public abstract ManagementChannel getChannel() throws IOException;

    public abstract void requestDone();

    public static synchronized ManagementClientChannelStrategy create(final ManagementChannel channel) {
        return new Existing(channel);
    }

    public static ManagementClientChannelStrategy create(String hostName, int port, final ExecutorService executorService, final ManagementOperationHandler handler) throws URISyntaxException, IOException {
        ManagementClientChannelStrategy strategy = new EstablishingWithNewEndpoint(hostName, port, executorService, handler);
        //Make sure the other end is alive
        try {
            strategy.getChannel();
        } finally {
            strategy.requestDone();
        }
        return strategy;
    }

    public static ManagementClientChannelStrategy create(String hostName, int port, final Endpoint endpoint, final ManagementOperationHandler handler) throws URISyntaxException, IOException {
        ManagementClientChannelStrategy strategy = new EstablishingWithExistingEndpoint(hostName, port, endpoint, handler);
        //Make sure the other end is alive
        try {
            strategy.getChannel();
        } finally {
            strategy.requestDone();
        }
        return strategy;
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

    private abstract static class Establishing extends ManagementClientChannelStrategy {

        private static final String CONNECT_TIME_OUT_PROPERTY = "org.jboss.as.client.connect.timeout";

        private final String hostName;
        private final int port;
        private final ManagementOperationHandler handler;
        private volatile ProtocolChannelClient<ManagementChannel> client;
        private volatile ManagementChannel channel;

        public Establishing(String hostName, int port, final ManagementOperationHandler handler) {
            this.hostName = hostName;
            this.port = port;
            this.handler = handler;
        }

        @Override
        public ManagementChannel getChannel() throws IOException {
            final ProtocolChannelClient.Configuration<ManagementChannel> configuration = new ProtocolChannelClient.Configuration<ManagementChannel>();
            try {
                addConfigurationProperties(configuration);
                configuration.setUri(new URI("remote://" + hostName +  ":" + port));
                configuration.setChannelFactory(new ManagementChannelFactory());
                client = ProtocolChannelClient.create(configuration);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }


            boolean ok = false;
            try {
                try {
                    client.connect();
                } catch (ConnectException e) {
                    throw new ConnectException("Could not connect to " + configuration.getUri() + " in " + configuration.getConnectTimeout() + "ms. " +
                              "Consider setting a longer timeout by setting -D" + CONNECT_TIME_OUT_PROPERTY + "=<timeout in ms>.");
                }
                channel = client.openChannel("management");
                channel.setOperationHandler(handler);
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

        abstract void addConfigurationProperties(ProtocolChannelClient.Configuration<ManagementChannel> configuration);
    }

    private static class EstablishingWithNewEndpoint extends Establishing {
        private final ExecutorService executorService;

        public EstablishingWithNewEndpoint(String hostName, int port, ExecutorService executorService, ManagementOperationHandler handler) {
            super(hostName, port, handler);
            this.executorService = executorService;
        }

        @Override
        void addConfigurationProperties(Configuration<ManagementChannel> configuration) {
            configuration.setUriScheme("remote");
            configuration.setEndpointName("endpoint");
            configuration.setExecutor(executorService);
        }

    }

    private static class EstablishingWithExistingEndpoint extends Establishing {
        private final Endpoint endpoint;

        public EstablishingWithExistingEndpoint(String hostName, int port, Endpoint endpoint, ManagementOperationHandler handler) {
            super(hostName, port, handler);
            this.endpoint = endpoint;
        }

        @Override
        void addConfigurationProperties(Configuration<ManagementChannel> configuration) {
            configuration.setEndpoint(endpoint);
        }
    }
}

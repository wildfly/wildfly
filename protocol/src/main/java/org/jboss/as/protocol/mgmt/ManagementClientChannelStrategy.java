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
import java.security.Provider;
import java.security.Security;
import java.util.concurrent.ExecutorService;

import javax.security.auth.callback.CallbackHandler;

import org.jboss.as.protocol.ProtocolChannelClient;
import org.jboss.as.protocol.ProtocolChannelClient.Configuration;
import org.jboss.remoting3.Endpoint;
import org.jboss.sasl.JBossSaslProvider;
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

    public static ManagementClientChannelStrategy create(String hostName, int port, final ExecutorService executorService, final ManagementOperationHandler handler, final CallbackHandler cbHandler) throws URISyntaxException, IOException {
        return new EstablishingWithNewEndpoint(hostName, port, executorService, handler, cbHandler);
    }

    public static ManagementClientChannelStrategy create(String hostName, int port, final Endpoint endpoint, final ManagementOperationHandler handler, final CallbackHandler cbHandler) throws URISyntaxException, IOException {
        return new EstablishingWithExistingEndpoint(hostName, port, endpoint, handler, cbHandler);
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
        private static final Provider saslProvider = new JBossSaslProvider();
        private final String hostName;
        private final int port;
        private final ManagementOperationHandler handler;
        private volatile ProtocolChannelClient<ManagementChannel> client;
        private volatile ManagementChannel channel;
        private final CallbackHandler callbackHandler;

        public Establishing(String hostName, int port, final ManagementOperationHandler handler, final CallbackHandler callbackHandler) {
            this.hostName = hostName;
            this.port = port;
            this.handler = handler;
            this.callbackHandler = callbackHandler;
        }

        @Override
        public ManagementChannel getChannel() throws IOException {
            if (Security.getProvider(saslProvider.getName()) == null) {
                Security.insertProviderAt(saslProvider, 1);
            }

            final ProtocolChannelClient.Configuration<ManagementChannel> configuration = new ProtocolChannelClient.Configuration<ManagementChannel>();
            try {
                addConfigurationProperties(configuration);
                configuration.setUri(new URI("remote://" + hostName +  ":" + port));
                configuration.setChannelFactory(new ManagementChannelFactory());
                configuration.setConnectTimeoutProperty(CONNECT_TIME_OUT_PROPERTY);
                client = ProtocolChannelClient.create(configuration);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            boolean ok = false;
            try {
                try {
                    client.connect(callbackHandler);
                } catch (ConnectException e) {
                    throw new ConnectException("Could not connect to " + configuration.getUri() + " in " + configuration.getConnectTimeout() + "ms. " +
                              "Make sure the server is running and/or consider setting a longer timeout by setting -D" + CONNECT_TIME_OUT_PROPERTY + "=<timeout in ms>.");
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

        public EstablishingWithNewEndpoint(String hostName, int port, ExecutorService executorService, ManagementOperationHandler handler, CallbackHandler cbHandler) {
            super(hostName, port, handler, cbHandler);
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

        public EstablishingWithExistingEndpoint(String hostName, int port, Endpoint endpoint, ManagementOperationHandler handler, CallbackHandler cbHandler) {
            super(hostName, port, handler, cbHandler);
            this.endpoint = endpoint;
        }

        @Override
        void addConfigurationProperties(Configuration<ManagementChannel> configuration) {
            configuration.setEndpoint(endpoint);
        }
    }
}

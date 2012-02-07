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

package org.jboss.as.server.mgmt.domain;

import org.jboss.as.protocol.ProtocolChannelClient;
import org.jboss.as.protocol.ProtocolMessages;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.as.protocol.mgmt.AbstractManagementRequest;
import org.jboss.as.protocol.mgmt.ActiveOperation;
import org.jboss.as.protocol.mgmt.FlushableDataOutput;
import org.jboss.as.protocol.mgmt.ManagementChannelHandler;
import org.jboss.as.protocol.mgmt.ManagementClientChannelStrategy;
import org.jboss.as.protocol.mgmt.ManagementRequestContext;
import org.jboss.as.remoting.management.ManagementRemotingServices;
import org.jboss.as.server.ServerMessages;
import org.jboss.remoting3.Channel;
import org.jboss.remoting3.CloseHandler;
import org.jboss.remoting3.Connection;
import org.xnio.OptionMap;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.security.auth.callback.CallbackHandler;
import java.io.DataInput;
import java.io.IOException;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.ExecutorService;

/**
 * The connection to the host-controller. In case the channel is closed it's the host-controllers responsibility
 * to ask individual managed servers to reconnect.
 *
 * @author Emanuel Muckenhuber
 */
public class HostControllerServerConnection extends ManagementClientChannelStrategy {

    private static final String SERVER_CHANNEL_TYPE = ManagementRemotingServices.SERVER_CHANNEL;

    private final String serverProcessName;
    private final ProtocolChannelClient.Configuration configuration;
    private final ManagementChannelHandler channelHandler;

    private volatile Connection connection;
    private volatile Channel channel;

    public HostControllerServerConnection(final String serverProcessName, final ProtocolChannelClient.Configuration configuration,
                                          final ExecutorService executorService) {
        this.serverProcessName = serverProcessName;
        this.configuration = configuration;
        this.channelHandler = new ManagementChannelHandler(this, executorService);
    }

    /**
     * Connect to the remote host-controller.
     *
     * @param callback the completed callback
     * @throws IOException
     */
    public synchronized void connect(final CallbackHandler callbackHandler, final ActiveOperation.CompletedCallback<Void> callback) throws IOException {
        boolean ok = false;
        try {
            openChannel(callbackHandler);
            channelHandler.executeRequest(new ServerRegisterRequest(), null, callback);
            ok = true;
        } finally {
            if(!ok) {
                close();
            }
        }
    }

    /**
     * Reconnect to the server.
     *
     * @param connectionURI the connection uri
     * @param callbackHandler the callback handler
     * @param callback the completed callback
     * @throws IOException
     */
    public synchronized void reconnect(final URI connectionURI, final CallbackHandler callbackHandler, final ActiveOperation.CompletedCallback<Boolean> callback) throws IOException {
        boolean ok = false;
        try {
            configuration.setUri(connectionURI);
            openChannel(callbackHandler);
            channelHandler.executeRequest(new ServerReconnectRequest(), null, callback);
            ok = true;
        } finally {
            if(!ok) {
                close();
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public Channel getChannel() throws IOException {
        final Channel channel = this.channel;
        if(channel == null) {
            synchronized (this) {
                if(this.channel == null) {
                    throw ProtocolMessages.MESSAGES.channelClosed();
                }
            }
        }
        return channel;
    }

    /** {@inheritDoc} */
    @Override
    public void close() throws IOException {
        StreamUtils.safeClose(channel);
        StreamUtils.safeClose(connection);
    }

    /**
     * Get the management channel handler.
     *
     * @return the handler
     */
    public ManagementChannelHandler getChannelHandler() {
        return channelHandler;
    }

    /**
     * Establish the connection.
     *
     * @throws IOException
     */
    private synchronized void openChannel(final CallbackHandler callbackHandler) throws IOException {
        boolean ok = false;
        try {
            // Connect
            final ProtocolChannelClient client = ProtocolChannelClient.create(configuration);
            connection = client.connectSync(callbackHandler, null, getAcceptingSSLContext());
            connection.addCloseHandler(new CloseHandler<Connection>() {
                @Override
                public void handleClose(final Connection closed, final IOException exception) {
                    synchronized (this) {
                        if(connection == closed) {
                            connection = null;
                        }
                    }
                }
            });
            channel = connection.openChannel(SERVER_CHANNEL_TYPE, OptionMap.EMPTY).get();
            channel.addCloseHandler(new CloseHandler<Channel>() {
                @Override
                public void handleClose(final Channel closed, final IOException exception) {
                    // Cancel active operations
                    channelHandler.handleChannelClosed(closed, exception);
                    synchronized (this) {
                        if(channel == closed) {
                            channel = null;
                        }
                    }
                }
            });
            channel.receiveMessage(channelHandler.getReceiver());
            ok = true;
        } finally {
            if(!ok) {
                StreamUtils.safeClose(channel);
                StreamUtils.safeClose(connection);
            }
        }
    }

    private static SSLContext getAcceptingSSLContext() throws IOException {
        /*
         * This connection is only a connection back to the local host controller.
         *
         * The HostController that started this process will have already provided the
         * required information regarding the connection so quietly allow the SSL connection
         * to be established.
         */
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            TrustManager[] trustManagers = new TrustManager[] { new X509TrustManager() {
                public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                }

                public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                }

                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
            } };

            sslContext.init(null, trustManagers, null);

            return sslContext;
        } catch (GeneralSecurityException e) {
            throw ServerMessages.MESSAGES.unableToInitialiseSSLContext(e.getMessage());
        }
    }

    /**
     * The server registration request.
     */
    private class ServerRegisterRequest extends AbstractManagementRequest<Void, Void> {

        @Override
        public byte getOperationType() {
            return DomainServerProtocol.REGISTER_REQUEST;
        }

        @Override
        protected void sendRequest(ActiveOperation.ResultHandler<Void> resultHandler, ManagementRequestContext<Void> voidManagementRequestContext, FlushableDataOutput output) throws IOException {
            output.write(DomainServerProtocol.PARAM_SERVER_NAME);
            output.writeUTF(serverProcessName);
        }

        @Override
        public void handleRequest(DataInput input, ActiveOperation.ResultHandler<Void> resultHandler, ManagementRequestContext<Void> voidManagementRequestContext) throws IOException {
            resultHandler.done(null);
        }

    }

    /**
     * The server reconnect request. Additionally to registering the server at the HC, the response will
     * contain whether this server is still in sync or needs to be restarted.
     */
    public class ServerReconnectRequest extends AbstractManagementRequest<Boolean, Void> {

        @Override
        public byte getOperationType() {
            return DomainServerProtocol.SERVER_RECONNECT_REQUEST;
        }

        @Override
        protected void sendRequest(final ActiveOperation.ResultHandler<Boolean> resultHandler, final ManagementRequestContext<Void> context, final FlushableDataOutput output) throws IOException {
            output.write(DomainServerProtocol.PARAM_SERVER_NAME);
            output.writeUTF(serverProcessName);
        }

        @Override
        public void handleRequest(final DataInput input, final ActiveOperation.ResultHandler<Boolean> resultHandler, final ManagementRequestContext<Void> context) throws IOException {
            final byte param = input.readByte();
            context.executeAsync(new ManagementRequestContext.AsyncTask<Void>() {
                @Override
                public void execute(ManagementRequestContext<Void> voidManagementRequestContext) throws Exception {
                    if(param == DomainServerProtocol.PARAM_OK) {
                        // Still in sync with the HC
                        resultHandler.done(Boolean.TRUE);
                    } else {
                        // Out of sync, set restart-required
                        resultHandler.done(Boolean.FALSE);
                    }
                }
            });
        }

    }

}

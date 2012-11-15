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

import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.remote.TransactionalProtocolOperationHandler;
import org.jboss.as.protocol.ProtocolConnectionConfiguration;
import org.jboss.as.protocol.ProtocolConnectionManager;
import org.jboss.as.protocol.ProtocolConnectionUtils;
import org.jboss.as.protocol.ProtocolMessages;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.as.protocol.mgmt.AbstractManagementRequest;
import org.jboss.as.protocol.mgmt.ActiveOperation;
import org.jboss.as.protocol.mgmt.FlushableDataOutput;
import org.jboss.as.protocol.mgmt.FutureManagementChannel;
import org.jboss.as.protocol.mgmt.ManagementChannelHandler;
import org.jboss.as.protocol.mgmt.ManagementPingRequest;
import org.jboss.as.protocol.mgmt.ManagementRequestContext;
import org.jboss.as.remoting.management.ManagementRemotingServices;
import org.jboss.as.server.ServerLogger;
import org.jboss.dmr.ModelNode;
import org.jboss.remoting3.Channel;
import org.jboss.remoting3.Connection;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.RealmCallback;
import javax.security.sasl.RealmChoiceCallback;
import java.io.DataInput;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * The connection to the host-controller. In case the channel is closed it's the host-controllers responsibility
 * to ask individual managed servers to reconnect.
 *
 * @author Emanuel Muckenhuber
 */
class HostControllerConnection extends FutureManagementChannel {

    private static final String SERVER_CHANNEL_TYPE = ManagementRemotingServices.SERVER_CHANNEL;
    private static final Charset UTF_8 = Charset.forName("UTF-8");

    private final String userName;
    private final String serverProcessName;
    private final ProtocolConnectionManager connectionManager;
    private final ManagementChannelHandler channelHandler;

    private ProtocolConnectionConfiguration configuration;

    HostControllerConnection(final String serverProcessName, final String userName, final ProtocolConnectionConfiguration configuration, final ExecutorService executorService) {
        this.userName = userName;
        this.serverProcessName = serverProcessName;
        this.configuration = configuration;
        this.channelHandler = new ManagementChannelHandler(this, executorService);
        this.connectionManager = ProtocolConnectionManager.create(configuration, this, new ReconnectTask());
    }

    ManagementChannelHandler getChannelHandler() {
        return channelHandler;
    }

    @Override
    public Channel getChannel() throws IOException {
        final Channel channel = super.getChannel();
        if(channel == null) {
            // Fail fast, don't try to await a new channel
            throw ProtocolMessages.MESSAGES.channelClosed();
        }
        return channel;
    }

    /**
     * Connect to the HC and retrieve the current model updates.
     *
     * @param controller the server controller
     * @param callback the operation completed callback
     * @return the boot operations
     * @throws IOException for any error
     */
    synchronized void openConnection(final ModelController controller, final ActiveOperation.CompletedCallback<ModelNode> callback) throws Exception {
        boolean ok = false;
        final Connection connection = connectionManager.connect();
        try {
            channelHandler.executeRequest(new ServerRegisterRequest(), null, callback);
            channelHandler.addHandlerFactory(new TransactionalProtocolOperationHandler(controller, channelHandler));
            ok = true;
        } finally {
            if(!ok) {
                connection.close();
            }
        }
    }

    /**
     * Reconnect to the HC.
     *
     * @param reconnectUri the reconnection uri
     * @param authKey the auth key
     * @return whether the server is still in sync
     * @throws IOException
     */
    synchronized boolean reConnect(final URI reconnectUri, byte[] authKey) throws IOException {
        // In case we are still connected, test the connection and see if we can reuse it
        if(connectionManager.isConnected()) {
            try {
                final Future<Long> result = channelHandler.executeRequest(ManagementPingRequest.INSTANCE, null).getResult();
                result.get(15, TimeUnit.SECONDS); // Hmm, perhaps 15 is already too much
                return true;
            } catch (Exception e) {
                ServerLogger.AS_ROOT_LOGGER.debugf(e, "failed to ping the host-controller, going to reconnect");
            }
            // Disconnect - the HC might have closed the connection without us noticing and is asking for a reconnect
            final Connection connection = connectionManager.getConnection();
            StreamUtils.safeClose(connection);
            if(connection != null) {
                try {
                    // Wait for the connection to be closed
                    connection.awaitClosed();
                } catch (InterruptedException e) {
                    throw new InterruptedIOException();
                }
            }
        }

        // Tweak the configuration with the new credentials
        final ProtocolConnectionConfiguration config = ProtocolConnectionConfiguration.copy(configuration);
        config.setCallbackHandler(createClientCallbackHandler(userName, authKey));
        config.setUri(reconnectUri);
        this.configuration = config;

        boolean ok = false;
        final Connection connection = connectionManager.connect();
        try {
            // Reconnect to the host-controller
            final ActiveOperation<Boolean, Void> result = channelHandler.executeRequest(new ServerReconnectRequest(), null);
            try {
                boolean inSync = result.getResult().get();
                ok = true;
                return inSync;
            } catch (ExecutionException e) {
                throw new IOException(e);
            } catch (InterruptedException e) {
                throw new InterruptedIOException();
            }
        } finally {
            if(!ok) {
                StreamUtils.safeClose(connection);
            }
        }
    }

    /**
     * Send the started notification
     */
    synchronized void started() {
        try {
            if(isConnected()) {
                channelHandler.executeRequest(new ServerStartedRequest(), null).getResult().await();
            }
        } catch (Exception e) {
            ServerLogger.AS_ROOT_LOGGER.debugf(e, "failed to send started notification");
        }
    }

    @Override
    public void connectionOpened(final Connection connection) throws IOException {
        final Channel channel = openChannel(connection, SERVER_CHANNEL_TYPE, configuration.getOptionMap());
        if(setChannel(channel)) {
            channel.receiveMessage(channelHandler.getReceiver());
            channel.addCloseHandler(channelHandler);
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

    /**
     * The server registration request.
     */
    private class ServerRegisterRequest extends AbstractManagementRequest<ModelNode, Void> {

        @Override
        public byte getOperationType() {
            return DomainServerProtocol.REGISTER_REQUEST;
        }

        @Override
        protected void sendRequest(final ActiveOperation.ResultHandler<ModelNode> resultHandler, final ManagementRequestContext<Void> context, final FlushableDataOutput output) throws IOException {
            // output.write(DomainServerProtocol.PARAM_SERVER_NAME);
            output.writeUTF(serverProcessName);
        }

        @Override
        public void handleRequest(DataInput input, ActiveOperation.ResultHandler<ModelNode> resultHandler, ManagementRequestContext<Void> voidManagementRequestContext) throws IOException {
            final byte param = input.readByte();
            if(param == DomainServerProtocol.PARAM_OK) {
                final ModelNode operations = new ModelNode();
                operations.readExternal(input);
                resultHandler.done(operations);
            } else {
                resultHandler.failed(new IOException());
            }
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

    public class ServerStartedRequest extends AbstractManagementRequest<Void, Void> {

        private final String message = ""; // started / failed message

        @Override
        public byte getOperationType() {
            return DomainServerProtocol.SERVER_STARTED_REQUEST;
        }

        @Override
        protected void sendRequest(ActiveOperation.ResultHandler<Void> resultHandler, ManagementRequestContext<Void> voidManagementRequestContext, FlushableDataOutput output) throws IOException {
            output.write(DomainServerProtocol.PARAM_OK); // TODO handle server start failed message
            output.writeUTF(message);
            resultHandler.done(null);
        }

        @Override
        public void handleRequest(DataInput input, ActiveOperation.ResultHandler<Void> resultHandler, ManagementRequestContext<Void> voidManagementRequestContext) throws IOException {
            //
        }

    }

    private class ReconnectTask implements ProtocolConnectionManager.ConnectTask {

        @Override
        public Connection connect() throws IOException {
            // Reconnect with a potentially new configuration
            return ProtocolConnectionUtils.connectSync(configuration);
        }

        @Override
        public ProtocolConnectionManager.ConnectionOpenHandler getConnectionOpenedHandler() {
            return HostControllerConnection.this;
        }

        @Override
        public ProtocolConnectionManager.ConnectTask connectionClosed() {
            return this;
        }

        @Override
        public void shutdown() {
            //
        }

    }

    /**
     * Create the client callback handler.
     *
     * @param userName the username
     * @param authKey the authentication key
     * @return the callback handler
     */
    static CallbackHandler createClientCallbackHandler(final String userName, final byte[] authKey) {
        return new ClientCallbackHandler(userName, authKey);
    }

    private static class ClientCallbackHandler implements CallbackHandler {

        private final String userName;
        private final byte[] authKey;

        private ClientCallbackHandler(String userName, byte[] authKey) {
            this.userName = userName;
            this.authKey = authKey;
        }

        public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
            for (Callback current : callbacks) {
                if (current instanceof RealmCallback) {
                    RealmCallback rcb = (RealmCallback) current;
                    String defaultText = rcb.getDefaultText();
                    rcb.setText(defaultText); // For now just use the realm suggested.
                } else if (current instanceof RealmChoiceCallback) {
                    throw new UnsupportedCallbackException(current, "Realm choice not currently supported.");
                } else if (current instanceof NameCallback) {
                    NameCallback ncb = (NameCallback) current;
                    ncb.setName(userName);
                } else if (current instanceof PasswordCallback) {
                    PasswordCallback pcb = (PasswordCallback) current;
                    pcb.setPassword(new String(authKey, UTF_8).toCharArray());
                } else {
                    throw new UnsupportedCallbackException(current);
                }
            }
        }
    }

}

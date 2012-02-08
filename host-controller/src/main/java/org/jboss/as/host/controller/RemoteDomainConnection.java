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

package org.jboss.as.host.controller;

import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.domain.controller.SlaveRegistrationException;
import org.jboss.as.domain.management.CallbackHandlerFactory;
import org.jboss.as.domain.management.SecurityRealm;
import org.jboss.as.host.controller.mgmt.DomainControllerProtocol;
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
import org.jboss.dmr.ModelNode;
import org.jboss.remoting3.Channel;
import org.jboss.remoting3.CloseHandler;
import org.jboss.remoting3.Connection;
import org.xnio.OptionMap;

import javax.net.ssl.SSLContext;
import javax.security.auth.callback.CallbackHandler;
import java.io.DataInput;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A connection to a remote domain controller. Once successfully connected this {@code ManagementClientChannelStrategy}
 * implementation will try to reconnect with a remote host-controller.
 *
 * @author Emanuel Muckenhuber
 */
class RemoteDomainConnection extends ManagementClientChannelStrategy {

    private static final String CHANNEL_SERVICE_TYPE = ManagementRemotingServices.DOMAIN_CHANNEL;
    private static final ReconnectPolicy reconnectPolicy = ReconnectPolicy.RECONNECT;

    private final String localHostName;
    private final ModelNode localHostInfo;
    private final SecurityRealm realm;
    private final ProtocolChannelClient.Configuration configuration;
    private final ManagementChannelHandler channelHandler;
    private final ExecutorService executorService;
    private final HostRegistrationCallback callback;

    // Try to reconnect to the remote DC
    private final AtomicBoolean reconnect = new AtomicBoolean();

    private volatile Connection connection;
    private volatile Channel channel;
    private volatile int reconnectionCount;

    RemoteDomainConnection(final String localHostName, final ModelNode localHostInfo,
                           final ProtocolChannelClient.Configuration configuration, final SecurityRealm realm,
                           final ExecutorService executorService, final HostRegistrationCallback callback) {
        this.callback = callback;
        this.localHostName = localHostName;
        this.localHostInfo = localHostInfo;
        this.configuration = configuration;
        this.realm = realm;
        this.executorService = executorService;
        this.channelHandler = new ManagementChannelHandler(this, executorService);
    }

    /**
     * Try to connect to the remote host.
     *
     * @throws IOException
     */
    protected RegistrationResult connect() throws IOException {
        final RegistrationResult result = connectSync();
        if(result.ok) {
            reconnect.set(true);
            HostControllerLogger.ROOT_LOGGER.registeredAtRemoteHostController();
        }
        return result;
    }

    /**
     * The channel handler.
     *
     * @return the channel handler
     */
    protected ManagementChannelHandler getHandler() {
        return channelHandler;
    }

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

    @Override
    public void close() throws IOException {
        if(reconnect.compareAndSet(true, false)) {
            try {
                channelHandler.executeRequest(new UnregisterModelControllerRequest(), null).getResult().await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                StreamUtils.safeClose(channel);
                StreamUtils.safeClose(connection);
            }
        }
    }

    /**
     * Connect and register at the remote domain controller.
     *
     * @throws IOException
     */
    private synchronized RegistrationResult connectSync() throws IOException {
        boolean ok = false;
        try {
            final ProtocolChannelClient client = ProtocolChannelClient.create(configuration);
            CallbackHandler callbackHandler = null;
            SSLContext sslContext = null;
            if (realm != null) {
                sslContext = realm.getSSLContext();
                CallbackHandlerFactory handlerFactory = realm.getSecretCallbackHandlerFactory();
                if (handlerFactory != null) {
                    callbackHandler = handlerFactory.getCallbackHandler(localHostName);
                }
            }
            // Connect
            connection = client.connectSync(callbackHandler, Collections.<String, String> emptyMap(), sslContext);
            connection.addCloseHandler(new CloseHandler<Connection>() {
                @Override
                public void handleClose(final Connection closed, final IOException exception) {
                    synchronized (this) {
                        if(connection == closed) {
                            connection = null;
                            connectionClosed();
                        }
                    }
                }
            });
            channel = connection.openChannel(CHANNEL_SERVICE_TYPE, OptionMap.EMPTY).get();
            channel.addCloseHandler(new CloseHandler<Channel>() {
                @Override
                public void handleClose(final Channel closed, final IOException exception) {
                    // Cancel all active operations
                    channelHandler.handleChannelClosed(closed, exception);
                    synchronized (this) {
                        if(channel == closed) {
                            channel = null;
                            connectionClosed();
                        }
                    }
                }
            });
            channel.receiveMessage(channelHandler.getReceiver());
            final RegistrationResult result;
            try {
                // Register at the remote side
                result = channelHandler.executeRequest(new RegisterHostControllerRequest(), null).getResult().get();
            } catch (Exception e) {
                if(e.getCause() instanceof IOException) {
                    throw (IOException) e;
                }
                throw new IOException(e);
            }
            ok = true;
            reconnectionCount = 0;
            return result;
        } finally {
            if(!ok) {
                StreamUtils.safeClose(connection);
                StreamUtils.safeClose(channel);
            }
        }
    }

    /**
     * Handle a connection closed event.
     */
    private void connectionClosed() {
        if(! reconnect.get()) {
            return; // Nothing to do
        }
        // Wait until the connection is closed before reconnecting
        final Connection connection = this.connection;
        if(connection != null) {
            if(channel == null) {
                connection.closeAsync();
            }
        } else {
            HostControllerLogger.ROOT_LOGGER.lostRemoteDomainConnection();
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    final ReconnectPolicy policy = reconnectPolicy;
                    for(;;) {
                        try {
                            // Wait before reconnecting
                            policy.wait(reconnectionCount++);
                            // Check if the connection was closed
                            if(! reconnect.get()) {
                                return;
                            }
                            // reconnect
                            HostControllerLogger.ROOT_LOGGER.debugf("trying to reconnect to remote host-controller");
                            final RegistrationResult result = connectSync();
                            if(result.isOK()) {
                                // Reconnected
                                HostControllerLogger.ROOT_LOGGER.reconnectedToMaster();
                                return;
                            }
                        } catch(IOException e) {
                            HostControllerLogger.ROOT_LOGGER.debugf(e, "failed to reconnect to the remote host-controller");
                        } catch(InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }
                }
            });
        }
    }

    /**
     * Reconfigure the connection URI and reconnect.
     *
     * @param connectionURI the new connection URI
     */
    protected void reconfigure(final URI connectionURI) {
        configuration.setUri(connectionURI);
        final Connection connection = this.connection;
        if(connection != null) {
            connection.closeAsync();
        }
    }

    /**
     * Apply the remote read domain model result.
     *
     * @param result the domain model result
     * @return whether it was applied successfully or not
     */
    boolean applyDomainModel(ModelNode result) {
        if(! result.hasDefined(ModelDescriptionConstants.RESULT)) {
            return false;
        }
        final List<ModelNode> bootOperations= result.get(ModelDescriptionConstants.RESULT).asList();
        return callback.applyDomainModel(bootOperations);
    }

    void registered() {
        callback.registrationComplete(channelHandler);
    }

    static class RegistrationResult {

        boolean ok;
        String message;
        SlaveRegistrationException.ErrorCode code;

        RegistrationResult(byte code, String message) {
            this(SlaveRegistrationException.ErrorCode.parseCode(code), message);
        }

        RegistrationResult(SlaveRegistrationException.ErrorCode code, String message) {
            this.message = message;
            this.code = code;
        }

        RegistrationResult() {
            ok = true;
        }

        public boolean isOK() {
            return ok;
        }

        public String getMessage() {
            return message;
        }

        public SlaveRegistrationException.ErrorCode getCode() {
            return code;
        }
    }

    static interface HostRegistrationCallback {

        /**
         * Apply the remote domain model.
         *
         * @param result the read-domain-model operation result
         * @return {@code true} if the model was applied successfully, {@code false} otherwise
         */
        boolean applyDomainModel(List<ModelNode> result);

        /**
         * Event that the registration was completed.
         *
         * @param handler the handler
         */
        void registrationComplete(ManagementChannelHandler handler);

    }

    /**
     * The host-controller registration request.
     */
    private class RegisterHostControllerRequest extends AbstractManagementRequest<RegistrationResult, Void> {

        @Override
        public byte getOperationType() {
            return DomainControllerProtocol.REGISTER_HOST_CONTROLLER_REQUEST;
        }

        @Override
        protected void sendRequest(final ActiveOperation.ResultHandler<RegistrationResult> resultHandler, final ManagementRequestContext<Void> context, final FlushableDataOutput output) throws IOException {
            output.write(DomainControllerProtocol.PARAM_HOST_ID);
            output.writeUTF(localHostName);
            localHostInfo.writeExternal(output);
        }

        @Override
        public void handleRequest(final DataInput input, final ActiveOperation.ResultHandler<RegistrationResult> resultHandler, final ManagementRequestContext<Void> context) throws IOException {
            byte param = input.readByte();
            // If it failed
            if(param != DomainControllerProtocol.PARAM_OK) {
                final byte errorCode = input.readByte();
                final String message =  input.readUTF();
                resultHandler.done(new RegistrationResult(errorCode, message));
                return;
            }
            final ModelNode domainModel = new ModelNode();
            domainModel.readExternal(input);
            context.executeAsync(new ManagementRequestContext.AsyncTask<Void>() {
                @Override
                public void execute(ManagementRequestContext<Void> voidManagementRequestContext) throws Exception {
                    // Apply the domain model
                    final boolean success = applyDomainModel(domainModel);
                    if(success) {
                        channelHandler.executeRequest(context.getOperationId(), new CompleteRegistrationRequest(DomainControllerProtocol.PARAM_OK));
                    } else {
                        channelHandler.executeRequest(context.getOperationId(), new CompleteRegistrationRequest(DomainControllerProtocol.PARAM_ERROR));
                        resultHandler.done(new RegistrationResult(SlaveRegistrationException.ErrorCode.UNKNOWN, ""));
                    }
                }
            });
        }
    }

    private class CompleteRegistrationRequest extends AbstractManagementRequest<RegistrationResult, Void> {

        private final byte outcome;
        private final String message = "yay!"; //

        private CompleteRegistrationRequest(final byte outcome) {
            this.outcome = outcome;
        }

        @Override
        public byte getOperationType() {
            return DomainControllerProtocol.COMPLETE_HOST_CONTROLLER_REGISTRATION;
        }

        @Override
        protected void sendRequest(final ActiveOperation.ResultHandler<RegistrationResult> resultHandler, final ManagementRequestContext<Void> context, final FlushableDataOutput output) throws IOException {
            output.writeByte(outcome);
            output.writeUTF(message);
        }

        @Override
        public void handleRequest(DataInput input, ActiveOperation.ResultHandler<RegistrationResult> resultHandler, ManagementRequestContext<Void> voidManagementRequestContext) throws IOException {
            final byte param = input.readByte();
            // If it failed
            if(param != DomainControllerProtocol.PARAM_OK) {
                final byte errorCode = input.readByte();
                final String message =  input.readUTF();
                resultHandler.done(new RegistrationResult(errorCode, message));
                return;
            }
            resultHandler.done(new RegistrationResult());
        }
    }

    private class UnregisterModelControllerRequest extends AbstractManagementRequest<Void, Void> {

        @Override
        public byte getOperationType() {
            return DomainControllerProtocol.UNREGISTER_HOST_CONTROLLER_REQUEST;
        }

        @Override
        protected void sendRequest(ActiveOperation.ResultHandler<Void> resultHandler, ManagementRequestContext<Void> voidManagementRequestContext, FlushableDataOutput output) throws IOException {
            output.write(DomainControllerProtocol.PARAM_HOST_ID);
            output.writeUTF(localHostName);
        }

        @Override
        public void handleRequest(DataInput input, ActiveOperation.ResultHandler<Void> resultHandler, ManagementRequestContext<Void> voidManagementRequestContext) throws IOException {
            HostControllerLogger.ROOT_LOGGER.unregisteredAtRemoteHostController();
            resultHandler.done(null);
        }

    }

}

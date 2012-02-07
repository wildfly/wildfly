/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

import java.io.DataInput;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.security.AccessController;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.client.OperationAttachments;
import org.jboss.as.controller.client.OperationMessageHandler;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.remote.TransactionalModelControllerOperationHandler;
import org.jboss.as.protocol.ProtocolChannelClient;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.as.protocol.mgmt.AbstractManagementRequest;
import org.jboss.as.protocol.mgmt.ActiveOperation;
import org.jboss.as.protocol.mgmt.FlushableDataOutput;
import org.jboss.as.protocol.mgmt.ManagementChannelAssociation;
import org.jboss.as.protocol.mgmt.ManagementChannelHandler;
import org.jboss.as.protocol.mgmt.ManagementRequestContext;
import org.jboss.as.server.ServerLogger;
import org.jboss.as.server.ServerMessages;
import org.jboss.as.repository.RemoteFileRequestAndHandler.CannotCreateLocalDirectoryException;
import org.jboss.as.repository.RemoteFileRequestAndHandler.DidNotReadEntireFileException;
import org.jboss.as.server.mgmt.domain.RemoteFileRepository.RemoteFileRepositoryExecutor;
import org.jboss.as.server.operations.ServerRestartRequiredHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.remoting3.Endpoint;
import org.jboss.threads.JBossThreadFactory;
import org.xnio.OptionMap;
import org.xnio.Options;
import org.xnio.Sequence;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.RealmCallback;
import javax.security.sasl.RealmChoiceCallback;

/**
 * Client used to interact with the local HostController.
 * The HC counterpart is ServerToHostOperationHandler
 *
 * @author John Bailey
 * @author Emanuel Muckenhuber
 * @author Kabir Khan
 */
public class HostControllerServerClient implements Service<HostControllerServerClient> {

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("host", "controller", "client");
    private static final String JBOSS_LOCAL_USER = "JBOSS-LOCAL-USER";

    private final InjectedValue<ModelController> controller = new InjectedValue<ModelController>();
    private final InjectedValue<RemoteFileRepository> remoteFileRepositoryValue = new InjectedValue<RemoteFileRepository>();
    private final InjectedValue<Endpoint> endpointInjector = new InjectedValue<Endpoint>();

    private final int port;
    private final String hostName;
    private final String serverName;
    private final String userName;
    private final String serverProcessName;
    private final byte[] authKey;
    private final ThreadFactory threadFactory = new JBossThreadFactory(new ThreadGroup("host-controller-connection-threads"), Boolean.FALSE, null, "%G - %t", null, null, AccessController.getContext());
    private final ExecutorService executor = Executors.newCachedThreadPool(threadFactory);

    private ManagementChannelAssociation handler;
    private HostControllerServerConnection connection;

    public HostControllerServerClient(final String serverName, final String serverProcessName, final String host,
                                      final int port, final byte[] authKey) {
        this.serverName = serverName;
        this.userName = "=" + serverName;
        this.hostName = host;
        this.port = port;
        this.serverProcessName = serverProcessName;
        this.authKey = authKey;
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void start(final StartContext context) throws StartException {
        final ModelController controller = this.controller.getValue();
        // Notify MSC asynchronously when the server gets registered
        context.asynchronous();
        try {
            final ProtocolChannelClient.Configuration configuration = new ProtocolChannelClient.Configuration();
            configuration.setEndpoint(endpointInjector.getValue());
            configuration.setConnectionTimeout(15000);
            configuration.setUri(new URI("remote://" + hostName + ":" + port));

            final OptionMap original = configuration.getOptionMap();
            OptionMap.Builder builder = OptionMap.builder();
            builder.addAll(original);
            builder.set(Options.SASL_DISALLOWED_MECHANISMS, Sequence.of(JBOSS_LOCAL_USER));
            configuration.setOptionMap(builder.getMap());

            final CallbackHandler callbackHandler = new ClientCallbackHandler(userName, authKey);

            connection = new HostControllerServerConnection(serverProcessName, configuration, executor);
            connection.connect(callbackHandler, new ActiveOperation.CompletedCallback<Void>() {
                @Override
                public void completed(Void result) {
                    // Once registered setup all the required channel handlers
                    final ManagementChannelHandler handler = connection.getChannelHandler();
                    handler.addHandlerFactory(new TransactionalModelControllerOperationHandler(controller, handler));
                    // Set the remote repository once registered
                    remoteFileRepositoryValue.getValue().setRemoteFileRepositoryExecutor(new RemoteFileRepositoryExecutorImpl());
                    // We finished the registration process
                    context.complete();
                    // TODO base the started message on some useful notification
                    started();
                }

                @Override
                public void failed(Exception e) {
                    context.failed(ServerMessages.MESSAGES.failedToConnectToHC(e));
                }

                @Override
                public void cancelled() {
                    context.failed(ServerMessages.MESSAGES.cancelledHCConnect());
                }
            });
            handler = connection.getChannelHandler();
        } catch (Exception e) {
            throw ServerMessages.MESSAGES.failedToConnectToHC(e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void stop(StopContext context) {
        StreamUtils.safeClose(connection);
    }

    /**
     * Reconnect to the HC.
     *
     * @param host the remote hostName
     * @param port the remote port
     * @param authKey the authKey
     */
    public synchronized void reconnect(final String host, final int port, final byte[] authKey) throws Exception {
        final HostControllerServerConnection connection = this.connection;
        final URI uri = new URI("remote://" + host + ":" + port);
        final CallbackHandler callbackHandler = new ClientCallbackHandler(userName, authKey);
        connection.reconnect(uri, callbackHandler, new ActiveOperation.CompletedCallback<Boolean>() {
            @Override
            public void completed(Boolean inSync) {
                // If we're not in sync require a restart
                if(! inSync) {
                    requireRestart();
                }
            }

            @Override
            public void failed(Exception e) {
                //
            }

            @Override
            public void cancelled() {
                //
            }
        });
    }

    /**
     * Set the restart required flag.
     */
    private synchronized void requireRestart() {
        final ModelController controller = this.controller.getValue();
        final ModelNode operation = new ModelNode();
        operation.get(ModelDescriptionConstants.OP).set(ServerRestartRequiredHandler.OPERATION_NAME);
        operation.get(ModelDescriptionConstants.OP_ADDR).setEmptyList();
        controller.execute(operation, OperationMessageHandler.logging, ModelController.OperationTransactionControl.COMMIT, OperationAttachments.EMPTY);
    }

    /**
     * Get the server name.
     *
     * @return the server name
     */
    public String getServerName(){
        return serverName;
    }

    /**
     * Signal that the server is started.
     */
    public synchronized void started() {
        final ManagementChannelAssociation handler = this.handler;
        try {
            handler.executeRequest(new ServerStartedRequest(), null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /** {@inheritDoc} */
    public synchronized HostControllerServerClient getValue() throws IllegalStateException {
        return this;
    }

    public Injector<ModelController> getServerControllerInjector() {
        return controller;
    }

    public Injector<RemoteFileRepository> getRemoteFileRepositoryInjector() {
        return remoteFileRepositoryValue;
    }

    public InjectedValue<Endpoint> getEndpointInjector() {
        return endpointInjector;
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
            // TODO update the API to better handle one-way messages
            resultHandler.done(null);
        }

        @Override
        public void handleRequest(DataInput input, ActiveOperation.ResultHandler<Void> resultHandler, ManagementRequestContext<Void> voidManagementRequestContext) throws IOException {
            //
        }

    }

    private static class GetFileRequest extends AbstractManagementRequest<File, Void> {
        private final String hash;
        private final File localDeploymentFolder;

        private GetFileRequest(final String hash, final File localDeploymentFolder) {
            this.hash = hash;
            this.localDeploymentFolder = localDeploymentFolder;
        }

        @Override
        public byte getOperationType() {
            return DomainServerProtocol.GET_FILE_REQUEST;
        }

        @Override
        protected void sendRequest(ActiveOperation.ResultHandler<File> resultHandler, ManagementRequestContext<Void> context, FlushableDataOutput output) throws IOException {
            //The root id does not matter here
            ServerToHostRemoteFileRequestAndHandler.INSTANCE.sendRequest(output, (byte)0, hash);
        }

        @Override
        public void handleRequest(DataInput input, ActiveOperation.ResultHandler<File> resultHandler, ManagementRequestContext<Void> context) throws IOException {
            try {
                File first = new File(localDeploymentFolder, hash.substring(0,2));
                File localPath = new File(first, hash.substring(2));
                ServerToHostRemoteFileRequestAndHandler.INSTANCE.handleResponse(input, localPath, ServerLogger.ROOT_LOGGER, resultHandler, context);
                resultHandler.done(null);
            } catch (CannotCreateLocalDirectoryException e) {
                resultHandler.failed(ServerMessages.MESSAGES.cannotCreateLocalDirectory(e.getDir()));
            } catch (DidNotReadEntireFileException e) {
                resultHandler.failed(ServerMessages.MESSAGES.didNotReadEntireFile(e.getMissing()));
            }
        }
    }

    private class RemoteFileRepositoryExecutorImpl implements RemoteFileRepositoryExecutor {
        public File getFile(final String relativePath, final byte repoId, final File localDeploymentFolder) {
            try {
                return handler.executeRequest(new GetFileRequest(relativePath, localDeploymentFolder), null).getResult().get();
            } catch (Exception e) {
                throw ServerMessages.MESSAGES.failedToGetFileFromRemoteRepository(e);
            }
        }
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
                    pcb.setPassword(new String(authKey).toCharArray());
                } else {
                    throw new UnsupportedCallbackException(current);
                }
            }
        }
    }

}

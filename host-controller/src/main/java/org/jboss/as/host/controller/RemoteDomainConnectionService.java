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

package org.jboss.as.host.controller;

import static org.jboss.as.host.controller.HostControllerLogger.ROOT_LOGGER;
import static org.jboss.as.host.controller.HostControllerMessages.MESSAGES;

import java.io.DataInput;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.security.AccessController;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.security.auth.callback.CallbackHandler;
import javax.security.sasl.SaslException;

import org.jboss.as.controller.HashUtil;
import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.Operation;
import org.jboss.as.controller.client.OperationMessageHandler;
import org.jboss.as.controller.remote.ExistingChannelModelControllerClient;
import org.jboss.as.controller.remote.TransactionalModelControllerOperationHandler;
import org.jboss.as.domain.controller.SlaveRegistrationException;
import org.jboss.as.domain.controller.SlaveRegistrationException.ErrorCode;
import org.jboss.as.domain.management.CallbackHandlerFactory;
import org.jboss.as.domain.management.security.SecretIdentityService;
import org.jboss.as.domain.management.security.SecurityRealmService;
import org.jboss.as.host.controller.mgmt.DomainControllerProtocol;
import org.jboss.as.host.controller.mgmt.DomainRemoteFileRequestAndHandler;
import org.jboss.as.process.protocol.Connection.ClosedCallback;
import org.jboss.as.protocol.ProtocolChannelClient;
import org.jboss.as.protocol.mgmt.AbstractManagementRequest;
import org.jboss.as.protocol.mgmt.AbstractMessageHandler;
import org.jboss.as.protocol.mgmt.ActiveOperation;
import org.jboss.as.protocol.mgmt.FlushableDataOutput;
import org.jboss.as.protocol.mgmt.ManagementChannelReceiver;
import org.jboss.as.protocol.mgmt.ManagementRequestContext;
import org.jboss.as.remoting.management.ManagementRemotingServices;
import org.jboss.as.repository.HostFileRepository;
import org.jboss.as.repository.RemoteFileRequestAndHandler.CannotCreateLocalDirectoryException;
import org.jboss.as.repository.RemoteFileRequestAndHandler.DidNotReadEntireFileException;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.remoting3.Channel;
import org.jboss.remoting3.CloseHandler;
import org.jboss.remoting3.Connection;
import org.jboss.remoting3.Endpoint;
import org.jboss.threads.AsyncFuture;
import org.jboss.threads.AsyncFutureTask;
import org.jboss.threads.JBossThreadFactory;
import org.xnio.OptionMap;


/**
 * Establishes the connection from a slave {@link org.jboss.as.domain.controller.DomainController} to the master
 * {@link org.jboss.as.domain.controller.DomainController}
 *
 * @author Kabir Khan
 */
public class RemoteDomainConnectionService implements MasterDomainControllerClient, Service<MasterDomainControllerClient>, ClosedCallback {

    private final ModelController controller;
    private final InetAddress host;
    private final int port;
    private final String name;
    private final RemoteFileRepository remoteFileRepository;

    private volatile ProtocolChannelClient channelClient;
    /** Used to invoke ModelController ops on the master */
    private volatile ModelControllerClient masterProxy;
    private final AtomicBoolean shutdown = new AtomicBoolean();
    private volatile Channel channel;
    private volatile AbstractMessageHandler handler;
    private final ThreadFactory threadFactory = new JBossThreadFactory(new ThreadGroup("domain-connection-threads"), Boolean.FALSE, null, "%G - %t", null, null, AccessController.getContext());
    private final ExecutorService executor = Executors.newCachedThreadPool(threadFactory);
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicBoolean registered = new AtomicBoolean(false);
    private final FutureClient futureClient = new FutureClient();
    private final InjectedValue<Endpoint> endpointInjector = new InjectedValue<Endpoint>();
    private final InjectedValue<CallbackHandlerFactory> callbackFactoryInjector = new InjectedValue<CallbackHandlerFactory>();

    private RemoteDomainConnectionService(final ModelController controller, final String name, final InetAddress host, final int port, final RemoteFileRepository remoteFileRepository){
        this.controller = controller;
        this.name = name;
        this.host = host;
        this.port = port;
        this.remoteFileRepository = remoteFileRepository;
        remoteFileRepository.setRemoteFileRepositoryExecutor(remoteFileRepositoryExecutor);
    }

    public static Future<MasterDomainControllerClient> install(final ServiceTarget serviceTarget, final ModelController controller,
                                                                  final String localHostName, final String remoteDcHost, final int remoteDcPort,
                                                                  final String securityRealm, final RemoteFileRepository remoteFileRepository) {
        RemoteDomainConnectionService service;
        try {
            service = new RemoteDomainConnectionService(
                    controller,
                    localHostName,
                    InetAddress.getByName(remoteDcHost),
                    remoteDcPort,
                    remoteFileRepository);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        ServiceBuilder builder = serviceTarget.addService(MasterDomainControllerClient.SERVICE_NAME, service)
                .addDependency(ManagementRemotingServices.MANAGEMENT_ENDPOINT, Endpoint.class, service.endpointInjector)
                .setInitialMode(ServiceController.Mode.ACTIVE);

        if (securityRealm != null) {
            ServiceName callbackHandlerService = SecurityRealmService.BASE_SERVICE_NAME.append(securityRealm).append(SecretIdentityService.SERVICE_SUFFIX);
            builder.addDependency(callbackHandlerService, CallbackHandlerFactory.class, service.callbackFactoryInjector);
        }

        builder.install();
        return service.futureClient;
    }

    /** {@inheritDoc} */
    public void register() {
        IllegalStateException ise = null;
        boolean connected = false;
        //This takes about 30 seconds should be enough to start up master if booted at the same time
        final long timeout = 30000;
        final long endTime = System.currentTimeMillis() + timeout;
        int retries = 0;
        while (!connected) {
            try {
               connect();
               connected = true;
               break;
            }
            catch (IllegalStateException e) {
                Throwable cause = e;
                while ((cause = cause.getCause()) != null) {
                    if (cause instanceof SaslException) {
                        throw MESSAGES.authenticationFailureUnableToConnect(cause);
                    }
                }

                if (System.currentTimeMillis() > endTime) {
                    throw MESSAGES.connectionToMasterTimeout(e, retries, timeout);
                }
                ise = e;
                try {
                    ReconnectPolicy.CONNECT.wait(retries);
                } catch (InterruptedException ie) {
                    throw MESSAGES.connectionToMasterInterrupted();
                }
            } catch (HostAlreadyExistsException e) {
                throw new IllegalStateException(e.getMessage());
            }
        }

        this.connected.set(true);
        registered.set(true);
    }

    private synchronized void connect() {

        // txOperationHandler = new TransactionalModelControllerOperationHandler(executor, controller);
        ProtocolChannelClient client;
        ProtocolChannelClient.Configuration configuration = new ProtocolChannelClient.Configuration();
        //Reusing the endpoint here after a disconnect does not seem to work once something has gone down, so try our own
        //configuration.setEndpoint(endpointInjector.getValue());
        configuration.setEndpointName("endpoint");
        configuration.setUriScheme("remote");

        final Connection connection;
        try {
            configuration.setUri(new URI("remote://" + host.getHostAddress() + ":" + port));
            client = ProtocolChannelClient.create(configuration);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        this.handler = new TransactionalModelControllerOperationHandler(controller, executor);

        try {
            CallbackHandler handler = null;
            CallbackHandlerFactory handlerFactory = callbackFactoryInjector.getOptionalValue();
            if (handlerFactory != null) {
                handler = handlerFactory.getCallbackHandler(name);
            }
            connection = client.connectSync(handler);
            this.channelClient = client;

            channel = connection.openChannel(ManagementRemotingServices.DOMAIN_CHANNEL, OptionMap.EMPTY).get();
            channel.addCloseHandler(new CloseHandler<Channel>() {
                public void handleClose(final Channel closed, final IOException exception) {
                    connectionClosed();
                }
            });
            channel.receiveMessage(ManagementChannelReceiver.createDelegating(this.handler));

            masterProxy = new ExistingChannelModelControllerClient(channel, executor);
        } catch (IOException e) {
            ROOT_LOGGER.cannotConnect(host.getHostAddress(), port);
            throw new IllegalStateException(e);
        }

        SlaveRegistrationException error = null;
        try {
            error = new RegisterModelControllerRequest().executeForResult(handler, channel, null);
        } catch (Exception e) {
            ROOT_LOGGER.errorRetrievingDomainModel(host.getHostAddress(), port, e.getLocalizedMessage());
            throw new IllegalStateException(e);
        }

        if (error != null) {
            if (error.getErrorCode() == ErrorCode.HOST_ALREADY_EXISTS) {
                throw new HostAlreadyExistsException(error.getErrorMessage());
            }
            throw new IllegalStateException(error.getErrorMessage());
        }
        HostControllerLogger.ROOT_LOGGER.registeredAtRemoteHostController();
    }

    /** {@inheritDoc} */
    public synchronized void unregister() {
        if (!registered.get()) {
            return;
        }
        try {
            new UnregisterModelControllerRequest().executeForResult(handler, channel, null);
            registered.set(false);
            HostControllerLogger.ROOT_LOGGER.unregisteredAtRemoteHostController();
        } catch (Exception e) {
            ROOT_LOGGER.debugf(e, "Error unregistering from master");
        }
        finally {
            channelClient.close();
        }
    }

    /** {@inheritDoc} */
    public synchronized HostFileRepository getRemoteFileRepository() {
        return remoteFileRepository;
    }

    @Override
    public ModelNode execute(ModelNode operation) throws IOException {
        return execute(operation, OperationMessageHandler.logging);
    }

    @Override
    public ModelNode execute(Operation operation) throws IOException {
        return masterProxy.execute(operation, OperationMessageHandler.logging);
    }

    @Override
    public ModelNode execute(ModelNode operation, OperationMessageHandler messageHandler) throws IOException {
        return masterProxy.execute(operation, messageHandler);
    }

    @Override
    public ModelNode execute(Operation operation, OperationMessageHandler messageHandler) throws IOException {
        return masterProxy.execute(operation, messageHandler);
    }

    @Override
    public AsyncFuture<ModelNode> executeAsync(ModelNode operation, OperationMessageHandler messageHandler) {
        return masterProxy.executeAsync(operation, messageHandler);
    }

    @Override
    public AsyncFuture<ModelNode> executeAsync(Operation operation, OperationMessageHandler messageHandler) {
        return masterProxy.executeAsync(operation, messageHandler);
    }

    @Override
    public void close() throws IOException {
        throw MESSAGES.closeShouldBeManagedByService();
    }


    /** {@inheritDoc} */
    @Override
    public synchronized void start(StartContext context) throws StartException {
        futureClient.setClient(this);
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void stop(StopContext context) {
        shutdown.set(true);
        if (channelClient != null) {
            unregister();
        }
    }

    @Override
    public void connectionClosed() {

        if (!connected.get()) {
            ROOT_LOGGER.nullReconnectInfo();
            return;
        }

        final AbstractMessageHandler handler = this.handler;
        if(handler != null) {
            // discard all active operations
            handler.shutdownNow();
        }

        if (!shutdown.get()) {
            //The remote host went down, try reconnecting
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                    }

                    int count = 0;
                    while (!shutdown.get()) {
                        ROOT_LOGGER.debug("Attempting reconnection to master...");
                        try {
                            connect();
                            ROOT_LOGGER.reconnectedToMaster();
                            break;
                        } catch (Exception e) {
                        }
                        try {
                            ReconnectPolicy.RECONNECT.wait(++count);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }
                }
            }).start();
        }
    }

    /** {@inheritDoc} */
    @Override
    public synchronized MasterDomainControllerClient getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    private abstract class RegistryRequest<T> extends AbstractManagementRequest<T, Void> {

    }

    private class RegisterModelControllerRequest extends RegistryRequest<SlaveRegistrationException> {

        @Override
        public byte getOperationType() {
            return DomainControllerProtocol.REGISTER_HOST_CONTROLLER_REQUEST;
        }

        @Override
        protected void sendRequest(ActiveOperation.ResultHandler<SlaveRegistrationException> resultHandler, ManagementRequestContext<Void> voidManagementRequestContext, FlushableDataOutput output) throws IOException {
            output.write(DomainControllerProtocol.PARAM_HOST_ID);
            output.writeUTF(name);
        }

        @Override
        public void handleRequest(DataInput input, ActiveOperation.ResultHandler<SlaveRegistrationException> resultHandler, ManagementRequestContext<Void> voidManagementRequestContext) throws IOException {
            byte status = input.readByte();
            if (status == DomainControllerProtocol.PARAM_OK) {
                resultHandler.done(null);
            } else {
                resultHandler.done(SlaveRegistrationException.parse(input.readUTF()));
            }
        }
    }

    private class UnregisterModelControllerRequest extends RegistryRequest<Void> {

        @Override
        public byte getOperationType() {
            return DomainControllerProtocol.UNREGISTER_HOST_CONTROLLER_REQUEST;
        }

        @Override
        protected void sendRequest(ActiveOperation.ResultHandler<Void> resultHandler, ManagementRequestContext<Void> voidManagementRequestContext, FlushableDataOutput output) throws IOException {
            output.write(DomainControllerProtocol.PARAM_HOST_ID);
            output.writeUTF(name);
        }

        @Override
        public void handleRequest(DataInput input, ActiveOperation.ResultHandler<Void> resultHandler, ManagementRequestContext<Void> voidManagementRequestContext) throws IOException {
            resultHandler.done(null);
        }

    }

    private class GetFileRequest extends RegistryRequest<File> {
        private final byte rootId;
        private final String filePath;
        private final HostFileRepository localFileRepository;

        private GetFileRequest(final byte rootId, final String filePath, final HostFileRepository localFileRepository) {
            this.rootId = rootId;
            this.filePath = filePath;
            this.localFileRepository = localFileRepository;
        }

        @Override
        public byte getOperationType() {
            return DomainControllerProtocol.GET_FILE_REQUEST;
        }

        @Override
        protected void sendRequest(ActiveOperation.ResultHandler<File> resultHandler, ManagementRequestContext<Void> context, FlushableDataOutput output) throws IOException {
            output.write(DomainControllerProtocol.PARAM_HOST_ID);
            output.writeUTF(name);
            DomainRemoteFileRequestAndHandler.INSTANCE.sendRequest(output, rootId, filePath);
        }

        @Override
        public void handleRequest(DataInput input, ActiveOperation.ResultHandler<File> resultHandler, ManagementRequestContext<Void> context) throws IOException {
            final File localPath;
            switch (rootId) {
                case DomainControllerProtocol.PARAM_ROOT_ID_FILE: {
                    localPath = localFileRepository.getFile(filePath);
                    break;
                }
                case DomainControllerProtocol.PARAM_ROOT_ID_CONFIGURATION: {
                    localPath = localFileRepository.getConfigurationFile(filePath);
                    break;
                }
                case DomainControllerProtocol.PARAM_ROOT_ID_DEPLOYMENT: {
                    byte[] hash = HashUtil.hexStringToByteArray(filePath);
                    localPath = localFileRepository.getDeploymentRoot(hash);
                    break;
                }
                default: {
                    localPath = null;
                }
            }
            try {
                DomainRemoteFileRequestAndHandler.INSTANCE.handleResponse(input, localPath, ROOT_LOGGER, resultHandler, context);
            } catch (CannotCreateLocalDirectoryException e) {
                throw MESSAGES.cannotCreateLocalDirectory(e.getDir());
            } catch (DidNotReadEntireFileException e) {
                throw MESSAGES.didNotReadEntireFile(e.getMissing());
            }
        }
    }

    static class RemoteFileRepository implements HostFileRepository {
        private final HostFileRepository localFileRepository;
        private volatile RemoteFileRepositoryExecutor remoteFileRepositoryExecutor;

        RemoteFileRepository(final HostFileRepository localFileRepository) {
            this.localFileRepository = localFileRepository;
        }

        @Override
        public final File getFile(String relativePath) {
            return getFile(relativePath, DomainControllerProtocol.PARAM_ROOT_ID_FILE);
        }

        @Override
        public final File getConfigurationFile(String relativePath) {
            return getFile(relativePath, DomainControllerProtocol.PARAM_ROOT_ID_CONFIGURATION);
        }

        @Override
        public final File[] getDeploymentFiles(byte[] deploymentHash) {
            String hex = deploymentHash == null ? "" : HashUtil.bytesToHexString(deploymentHash);
            return getFile(hex, DomainControllerProtocol.PARAM_ROOT_ID_DEPLOYMENT).listFiles();
        }

        @Override
        public File getDeploymentRoot(byte[] deploymentHash) {
            String hex = deploymentHash == null ? "" : HashUtil.bytesToHexString(deploymentHash);
            return getFile(hex, DomainControllerProtocol.PARAM_ROOT_ID_DEPLOYMENT);
        }

        private File getFile(final String relativePath, final byte repoId) {
            return remoteFileRepositoryExecutor.getFile(relativePath, repoId, localFileRepository);
        }

        private void setRemoteFileRepositoryExecutor(RemoteFileRepositoryExecutor remoteFileRepositoryExecutor) {
            this.remoteFileRepositoryExecutor = remoteFileRepositoryExecutor;
        }

        @Override
        public void deleteDeployment(byte[] deploymentHash) {
            localFileRepository.deleteDeployment(deploymentHash);
        }
    }

    private static interface RemoteFileRepositoryExecutor {
        File getFile(final String relativePath, final byte repoId, HostFileRepository localFileRepository);
    }

    private RemoteFileRepositoryExecutor remoteFileRepositoryExecutor = new RemoteFileRepositoryExecutor() {
        public File getFile(final String relativePath, final byte repoId, HostFileRepository localFileRepository) {
            try {
                return new GetFileRequest(repoId, relativePath, localFileRepository).executeForResult(handler, channel, null);
            } catch (Exception e) {
                throw MESSAGES.failedToGetFileFromRemoteRepository(e);
            }
        }
    };

    private class FutureClient extends AsyncFutureTask<MasterDomainControllerClient>{

        protected FutureClient() {
            super(null);
        }

        private void setClient(MasterDomainControllerClient client) {
            super.setResult(client);
        }
    }

    private static class HostAlreadyExistsException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        public HostAlreadyExistsException(String msg) {
            super(msg);
        }

    }

}

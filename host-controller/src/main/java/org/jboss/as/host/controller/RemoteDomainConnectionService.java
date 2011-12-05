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

import static org.jboss.as.process.protocol.ProtocolUtils.expectHeader;

import java.io.BufferedOutputStream;
import java.io.DataInput;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
import org.jboss.as.domain.controller.FileRepository;
import org.jboss.as.domain.controller.operations.SlaveRegistrationError;
import org.jboss.as.domain.controller.operations.SlaveRegistrationError.ErrorCode;
import org.jboss.as.domain.management.CallbackHandlerFactory;
import org.jboss.as.domain.management.security.SecretIdentityService;
import org.jboss.as.domain.management.security.SecurityRealmService;
import org.jboss.as.host.controller.mgmt.DomainControllerProtocol;
import org.jboss.as.process.protocol.Connection.ClosedCallback;
import org.jboss.as.protocol.ProtocolChannelClient;
import org.jboss.as.protocol.mgmt.FlushableDataOutput;
import org.jboss.as.protocol.mgmt.ManagementChannel;
import org.jboss.as.protocol.mgmt.ManagementChannelFactory;
import org.jboss.as.protocol.mgmt.ManagementClientChannelStrategy;
import org.jboss.as.protocol.mgmt.ManagementRequest;
import org.jboss.as.protocol.mgmt.ManagementResponseHandler;
import org.jboss.as.remoting.management.ManagementRemotingServices;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
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
import org.jboss.remoting3.Endpoint;
import org.jboss.threads.AsyncFuture;
import org.jboss.threads.AsyncFutureTask;


/**
 * Establishes the connection from a slave {@link org.jboss.as.domain.controller.DomainController} to the master
 * {@link org.jboss.as.domain.controller.DomainController}
 *
 * @author Kabir Khan
 */
public class RemoteDomainConnectionService implements MasterDomainControllerClient, Service<MasterDomainControllerClient>, ClosedCallback {

    private static final Logger log = Logger.getLogger("org.jboss.as.domain.controller");
    private final ModelController controller;
    private final InetAddress host;
    private final int port;
    private final String name;
    private final RemoteFileRepository remoteFileRepository;

    private volatile ProtocolChannelClient<ManagementChannel> channelClient;
    /** Used to invoke ModelController ops on the master */
    private volatile ModelControllerClient masterProxy;
    /** Handler for transactional operations */
    private volatile TransactionalModelControllerOperationHandler txOperationHandler;
    private final AtomicBoolean shutdown = new AtomicBoolean();
    private volatile ManagementChannel channel;
    private final ExecutorService executor = Executors.newCachedThreadPool();
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
                        throw new IllegalStateException("Unable to connect due to authentication failure.", cause);
                    }
                }

                if (System.currentTimeMillis() > endTime) {
                    throw new IllegalStateException("Could not connect to master in " + retries + "attempts within  " + timeout + " ms", e);
                }
                ise = e;
                try {
                    ReconnectPolicy.CONNECT.wait(retries);
                } catch (InterruptedException ie) {
                    throw new IllegalStateException("Interrupted while trying to connect to master");
                }
            } catch (HostAlreadyExistsException e) {
                throw new IllegalStateException(e.getMessage());
            }
        }

        this.connected.set(true);
        registered.set(true);
    }

    private synchronized void connect() {
//        if (this.channelClient != null) {
//            try {
//                new UnregisterModelControllerRequest().executeForResult(executor, ManagementClientChannelStrategy.create(channel));
//            } catch (Exception e) {
//            }
//
//            this.channelClient.close();
//            this.channelClient = null;
//        }

        txOperationHandler = new TransactionalModelControllerOperationHandler(executor, controller);
        ProtocolChannelClient<ManagementChannel> client;
        ProtocolChannelClient.Configuration<ManagementChannel> configuration = new ProtocolChannelClient.Configuration<ManagementChannel>();
        //Reusing the endpoint here after a disconnect does not seem to work once something has gone down, so try our own
        //configuration.setEndpoint(endpointInjector.getValue());
        configuration.setEndpointName("endpoint");
        configuration.setUriScheme("remote");

        try {
            configuration.setUri(new URI("remote://" + host.getHostAddress() + ":" + port));
            configuration.setChannelFactory(new ManagementChannelFactory(txOperationHandler));
            client = ProtocolChannelClient.create(configuration);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        try {
            CallbackHandler handler = null;
            CallbackHandlerFactory handlerFactory = callbackFactoryInjector.getOptionalValue();
            if (handlerFactory != null) {
                handler = handlerFactory.getCallbackHandler(name);
            }

            client.connect(handler);
            this.channelClient = client;

            ManagementChannel channel = client.openChannel(ManagementRemotingServices.DOMAIN_CHANNEL);
            this.channel = channel;

            channel.addCloseHandler(new CloseHandler<Channel>() {
                public void handleClose(final Channel closed, final IOException exception) {
                    connectionClosed();
                }
            });

            channel.startReceiving();

            masterProxy = new ExistingChannelModelControllerClient(channel);
        } catch (IOException e) {
            log.warnf("Could not connect to remote domain controller %s:%d", host.getHostAddress(), port);
            throw new IllegalStateException(e);
        }

        SlaveRegistrationError error = null;
        try {
            error = new RegisterModelControllerRequest().executeForResult(executor, ManagementClientChannelStrategy.create(channel));
        } catch (Exception e) {
            log.warnf("Error retrieving domain model from remote domain controller %s:%d: %s", host.getHostAddress(), port, e.getMessage());
            throw new IllegalStateException(e);
        }

        if (error != null) {
            if (error.getErrorCode() == ErrorCode.HOST_ALREADY_EXISTS) {
                throw new HostAlreadyExistsException(error.getErrorMessage());
            }
            throw new IllegalStateException(error.getErrorMessage());
        }

    }

    /** {@inheritDoc} */
    public synchronized void unregister() {
        if (!registered.get()) {
            return;
        }
        try {
            new UnregisterModelControllerRequest().executeForResult(executor, ManagementClientChannelStrategy.create(channel));
            registered.set(false);
        } catch (Exception e) {
            log.debugf(e, "Error unregistering from master");
        }
        finally {
            channelClient.close();
        }
    }

    /** {@inheritDoc} */
    public synchronized FileRepository getRemoteFileRepository() {
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
        throw new UnsupportedOperationException("Close should be managed by the service");
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
            log.error("Null reconnect info, cannot try to reconnect");
            return;
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
                        log.debug("Attempting reconnection to master...");
                        try {
                            connect();
                            log.info("Reconnected to master");
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

    private abstract class RegistryRequest<T> extends ManagementRequest<T>{

    }

    private class RegisterModelControllerRequest extends RegistryRequest<SlaveRegistrationError> {

        RegisterModelControllerRequest() {
        }

        @Override
        protected byte getRequestCode() {
            return DomainControllerProtocol.REGISTER_HOST_CONTROLLER_REQUEST;
        }

        /** {@inheritDoc} */
        @Override
        protected void writeRequest(final int protocolVersion, final FlushableDataOutput output) throws IOException {
            output.write(DomainControllerProtocol.PARAM_HOST_ID);
            output.writeUTF(name);
        }

        protected ManagementResponseHandler<SlaveRegistrationError> getResponseHandler() {
            return new ManagementResponseHandler<SlaveRegistrationError>() {
                @Override
                protected SlaveRegistrationError readResponse(DataInput input) throws IOException {
                    byte status = input.readByte();
                    if (status == DomainControllerProtocol.PARAM_OK) {
                        return null;
                    } else {
                        //return input.readUTF();
                        return SlaveRegistrationError.parse(input.readUTF());
                    }
                }
            };
        }
    }

    private class UnregisterModelControllerRequest extends RegistryRequest<Void> {
        @Override
        protected byte getRequestCode() {
            return DomainControllerProtocol.UNREGISTER_HOST_CONTROLLER_REQUEST;
        }

        /** {@inheritDoc} */
        @Override
        protected void writeRequest(final int protocolVersion, final FlushableDataOutput output) throws IOException {
            output.write(DomainControllerProtocol.PARAM_HOST_ID);
            output.writeUTF(name);
        }

        protected ManagementResponseHandler<Void> getResponseHandler() {
            return ManagementResponseHandler.EMPTY_RESPONSE;
        }
    }

    private class GetFileRequest extends RegistryRequest<File> {
        private final byte rootId;
        private final String filePath;
        private final FileRepository localFileRepository;

        private GetFileRequest(final byte rootId, final String filePath, final FileRepository localFileRepository) {
            this.rootId = rootId;
            this.filePath = filePath;
            this.localFileRepository = localFileRepository;
        }

        @Override
        public final byte getRequestCode() {
            return DomainControllerProtocol.GET_FILE_REQUEST;
        }

        @Override
        protected final void writeRequest(final int protocolVersion, final FlushableDataOutput output) throws IOException {
            super.writeRequest(protocolVersion, output);
            log.debugf("Requesting files for path %s", filePath);
            output.writeByte(DomainControllerProtocol.PARAM_ROOT_ID);
            output.writeByte(rootId);
            output.writeByte(DomainControllerProtocol.PARAM_FILE_PATH);
            output.writeUTF(filePath);
        }

        protected ManagementResponseHandler<File> getResponseHandler() {
            return new ManagementResponseHandler<File>() {
                @Override
                protected final File readResponse(final DataInput input) throws IOException {
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
                    expectHeader(input, DomainControllerProtocol.PARAM_NUM_FILES);
                    int numFiles = input.readInt();
                    log.debugf("Received %d files for %s", numFiles, localPath);
                    switch (numFiles) {
                        case -1: { // Not found on DC
                            break;
                        }
                        case 0: { // Found on DC, but was an empty dir
                            if (!localPath.mkdirs()) {
                                throw new IOException("Unable to create local directory: " + localPath);
                            }
                            break;
                        }
                        default: { // Found on DC
                            for (int i = 0; i < numFiles; i++) {
                                expectHeader(input, DomainControllerProtocol.FILE_START);
                                expectHeader(input, DomainControllerProtocol.PARAM_FILE_PATH);
                                final String path = input.readUTF();
                                expectHeader(input, DomainControllerProtocol.PARAM_FILE_SIZE);
                                final long length = input.readLong();
                                log.debugf("Received file [%s] of length %d", path, length);
                                final File file = new File(localPath, path);
                                if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
                                    throw new IOException("Unable to create local directory " + localPath.getParent());
                                }
                                long totalRead = 0;
                                OutputStream fileOut = null;
                                try {
                                    fileOut = new BufferedOutputStream(new FileOutputStream(file));
                                    final byte[] buffer = new byte[8192];
                                    while (totalRead < length) {
                                        int len = Math.min((int) (length - totalRead), buffer.length);
                                        input.readFully(buffer, 0, len);
                                        fileOut.write(buffer, 0, len);
                                        totalRead += len;
                                    }
                                } finally {
                                    if (fileOut != null) {
                                        fileOut.close();
                                    }
                                }
                                if (totalRead != length) {
                                    throw new IOException("Did not read the entire file. Missing: " + (length - totalRead));
                                }

                                expectHeader(input, DomainControllerProtocol.FILE_END);
                            }
                        }
                    }
                    return localPath;
                }
            };
        }
    }

    static class RemoteFileRepository implements FileRepository {
        private final FileRepository localFileRepository;
        private volatile RemoteFileRepositoryExecutor remoteFileRepositoryExecutor;

        RemoteFileRepository(final FileRepository localFileRepository) {
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
    }

    private static interface RemoteFileRepositoryExecutor {
        File getFile(final String relativePath, final byte repoId, FileRepository localFileRepository);
    }

    private RemoteFileRepositoryExecutor remoteFileRepositoryExecutor = new RemoteFileRepositoryExecutor() {
        public File getFile(final String relativePath, final byte repoId, FileRepository localFileRepository) {
            try {
                return new GetFileRequest(repoId, relativePath, localFileRepository).executeForResult(executor, ManagementClientChannelStrategy.create(channel));
            } catch (Exception e) {
                throw new RuntimeException("Failed to get file from remote repository", e);
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

        public HostAlreadyExistsException(String msg) {
            super(msg);
        }

    }
}

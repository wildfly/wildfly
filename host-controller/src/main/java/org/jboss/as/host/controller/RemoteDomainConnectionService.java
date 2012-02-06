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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DOMAIN_MODEL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT_MAJOR_VERSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT_MINOR_VERSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PRODUCT_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PRODUCT_VERSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RELEASE_CODENAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RELEASE_VERSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.host.controller.HostControllerLogger.ROOT_LOGGER;
import static org.jboss.as.host.controller.HostControllerMessages.MESSAGES;

import java.io.DataInput;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.security.AccessController;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

import javax.security.sasl.SaslException;

import org.jboss.as.controller.HashUtil;
import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.Operation;
import org.jboss.as.controller.client.OperationAttachments;
import org.jboss.as.controller.client.OperationMessageHandler;
import org.jboss.as.controller.remote.ExistingChannelModelControllerClient;
import org.jboss.as.controller.remote.TransactionalModelControllerOperationHandler;
import org.jboss.as.domain.controller.LocalHostControllerInfo;
import org.jboss.as.domain.controller.SlaveRegistrationException;
import org.jboss.as.domain.controller.operations.ApplyRemoteMasterDomainModelHandler;
import org.jboss.as.domain.management.SecurityRealm;
import org.jboss.as.domain.management.security.SecurityRealmService;
import org.jboss.as.host.controller.mgmt.DomainControllerProtocol;
import org.jboss.as.host.controller.mgmt.DomainRemoteFileRequestAndHandler;
import org.jboss.as.protocol.ProtocolChannelClient;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.as.protocol.mgmt.AbstractManagementRequest;
import org.jboss.as.protocol.mgmt.ActiveOperation;
import org.jboss.as.protocol.mgmt.FlushableDataOutput;
import org.jboss.as.protocol.mgmt.ManagementChannelHandler;
import org.jboss.as.protocol.mgmt.ManagementRequestContext;
import org.jboss.as.protocol.mgmt.ProtocolUtils;
import org.jboss.as.remoting.management.ManagementRemotingServices;
import org.jboss.as.repository.HostFileRepository;
import org.jboss.as.repository.RemoteFileRequestAndHandler.CannotCreateLocalDirectoryException;
import org.jboss.as.repository.RemoteFileRequestAndHandler.DidNotReadEntireFileException;
import org.jboss.as.version.ProductConfig;
import org.jboss.as.version.Version;
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
import org.jboss.remoting3.Endpoint;
import org.jboss.threads.AsyncFuture;
import org.jboss.threads.AsyncFutureTask;
import org.jboss.threads.JBossThreadFactory;

/**
 * Establishes the connection from a slave {@link org.jboss.as.domain.controller.DomainController} to the master
 * {@link org.jboss.as.domain.controller.DomainController}
 *
 * @author Kabir Khan
 */
public class RemoteDomainConnectionService implements MasterDomainControllerClient, Service<MasterDomainControllerClient> {

    private static final ModelNode APPLY_DOMAIN_MODEL = new ModelNode();
    static {
        APPLY_DOMAIN_MODEL.get(OP).set(ApplyRemoteMasterDomainModelHandler.OPERATION_NAME);
        //FIXME this makes the op work after boot (i.e. slave connects to restarted master), but does not make the slave resync the servers
        APPLY_DOMAIN_MODEL.get(OPERATION_HEADERS, "execute-for-coordinator").set(true);
        APPLY_DOMAIN_MODEL.get(OP_ADDR).setEmptyList();
        APPLY_DOMAIN_MODEL.protect();
    }

    private final ModelController controller;
    private final ProductConfig productConfig;
    private final LocalHostControllerInfo localHostInfo;
    private final RemoteFileRepository remoteFileRepository;

    /** Used to invoke ModelController ops on the master */
    private volatile ModelControllerClient masterProxy;

    private final FutureClient futureClient = new FutureClient();
    private final InjectedValue<Endpoint> endpointInjector = new InjectedValue<Endpoint>();
    private final InjectedValue<SecurityRealm> securityRealmInjector = new InjectedValue<SecurityRealm>();

    private final ThreadFactory threadFactory = new JBossThreadFactory(new ThreadGroup("domain-connection-threads"), Boolean.FALSE, null, "%G - %t", null, null, AccessController.getContext());
    private final ExecutorService executor = Executors.newCachedThreadPool(threadFactory);

    private RemoteDomainConnection connection;
    private ManagementChannelHandler handler;

    private RemoteDomainConnectionService(final ModelController controller, final LocalHostControllerInfo localHostControllerInfo, final ProductConfig productConfig, final RemoteFileRepository remoteFileRepository){
        this.controller = controller;
        this.productConfig = productConfig;
        this.localHostInfo = localHostControllerInfo;
        this.remoteFileRepository = remoteFileRepository;
        remoteFileRepository.setRemoteFileRepositoryExecutor(remoteFileRepositoryExecutor);
    }

    public static Future<MasterDomainControllerClient> install(final ServiceTarget serviceTarget, final ModelController controller,
                                                               final LocalHostControllerInfo localHostControllerInfo, final ProductConfig productConfig,
                                                               final String securityRealm, final RemoteFileRepository remoteFileRepository) {
        RemoteDomainConnectionService service;
        service = new RemoteDomainConnectionService(controller, localHostControllerInfo, productConfig, remoteFileRepository);
        ServiceBuilder builder = serviceTarget.addService(MasterDomainControllerClient.SERVICE_NAME, service)
                .addDependency(ManagementRemotingServices.MANAGEMENT_ENDPOINT, Endpoint.class, service.endpointInjector)
                .setInitialMode(ServiceController.Mode.ACTIVE);

        if (securityRealm != null) {
            ServiceName realmName = SecurityRealmService.BASE_SERVICE_NAME.append(securityRealm);
            builder.addDependency(realmName, SecurityRealm.class, service.securityRealmInjector);
        }

        builder.install();
        return service.futureClient;
    }

    /** {@inheritDoc} */
    public synchronized void register() throws IOException {
        boolean connected = false;
        //This takes about 30 seconds should be enough to start up master if booted at the same time
        final long timeout = 30000;
        final long endTime = System.currentTimeMillis() + timeout;
        int retries = 0;
        while (!connected) {
            RemoteDomainConnection.RegistrationResult result = null;
            try {
                // Try to connect to the domain controller
                result = connection.connect();
                connected = true;
            } catch (IOException e) {
                Throwable cause = e;
                while ((cause = cause.getCause()) != null) {
                    if (cause instanceof SaslException) {
                        throw MESSAGES.authenticationFailureUnableToConnect(cause);
                    }
                }
                if (System.currentTimeMillis() > endTime) {
                    throw MESSAGES.connectionToMasterTimeout(e, retries, timeout);
                }
                try {
                    HostControllerLogger.ROOT_LOGGER.cannotConnect(localHostInfo.getRemoteDomainControllerHost(), localHostInfo.getRemoteDomainControllertPort());
                    ReconnectPolicy.CONNECT.wait(retries);
                } catch (InterruptedException ie) {
                    throw MESSAGES.connectionToMasterInterrupted();
                }
            }
            // If it's not ok
            if(result != null && ! result.isOK()) {
                switch (result.getCode()) {
                    case HOST_ALREADY_EXISTS:
                        throw new HostAlreadyExistsException(result.getMessage());
                    default:
                        throw new IOException(new SlaveRegistrationException(result.getCode(), result.getMessage()).marshal());
                }
            }
        }
        if(connected) {
            // Setup the transaction protocol handler
            handler.addHandlerFactory(new TransactionalModelControllerOperationHandler(controller, handler));
            // Use the existing channel strategy
            masterProxy = ExistingChannelModelControllerClient.createAndAdd(handler);
        }
    }

    /** {@inheritDoc} */
    public synchronized void unregister() {
        StreamUtils.safeClose(connection);
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
        final RemoteDomainConnection connection;
        final ManagementChannelHandler handler;
        try {

            // Include additional local host information when registering at the DC
            final ModelNode hostInfo = createLocalHostHostInfo(localHostInfo, productConfig);

            // Gather the required information to connect to the remote DC
            final ProtocolChannelClient.Configuration configuration = new ProtocolChannelClient.Configuration();
            configuration.setUri(new URI("remote://" + ProtocolUtils.formatPossibleIpv6Address(localHostInfo.getRemoteDomainControllerHost()) + ":" + localHostInfo.getRemoteDomainControllertPort()));
            configuration.setEndpoint(endpointInjector.getValue());

            final SecurityRealm realm = securityRealmInjector.getOptionalValue();
            // Create the remote domain channel strategy
            connection = new RemoteDomainConnection(localHostInfo.getLocalHostName(), hostInfo, configuration, realm,
                                                    executor, new RemoteDomainConnection.HostRegistrationCallback() {
                @Override
                public boolean applyDomainModel(final List<ModelNode> bootOperations) {
                    // Apply the model..
                    return applyRemoteDomainModel(bootOperations);
                }

                @Override
                public void registrationComplete(ManagementChannelHandler handler) {
                    //
                }
            });
            // Setup the management channel handler
            handler = connection.getHandler();
        } catch (Exception e) {
            throw new StartException(e);
        } finally {
            futureClient.setClient(this);
        }
        this.connection = connection;
        this.handler = handler;
    }

    /**
     * Apply the remote domain model to the local host controller.
     *
     * @param bootOperations the result of the remote read-domain-model op
     * @return {@code true} if the model was applied successfully, {@code false} otherwise
     */
    private boolean applyRemoteDomainModel(final List<ModelNode> bootOperations) {
        final ModelNode result;
        try {
            // Create the apply-domain-model operation
            final ModelNode operation = APPLY_DOMAIN_MODEL.clone();
            operation.get(DOMAIN_MODEL).set(bootOperations);
            // Execute the operation
            result = controller.execute(operation, OperationMessageHandler.logging, ModelController.OperationTransactionControl.COMMIT, OperationAttachments.EMPTY);
        } catch (Exception e) {
            return false;
        }
        // If it did not success, don't register it at the DC
        return SUCCESS.equals(result.get(OUTCOME).asString());
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void stop(StopContext context) {
        StreamUtils.safeClose(connection);
    }

    /** {@inheritDoc} */
    @Override
    public synchronized MasterDomainControllerClient getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    private class GetFileRequest extends AbstractManagementRequest<File, Void> {
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
            output.writeUTF(localHostInfo.getLocalHostName());
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
                return handler.executeRequest(new GetFileRequest(repoId, relativePath, localFileRepository), null).getResult().get();
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

    private static class HostAlreadyExistsException extends IOException {

        private static final long serialVersionUID = 1L;

        public HostAlreadyExistsException(String msg) {
            super(msg);
        }

    }

    /**
     * Create the metadata which gets send to the DC when registering.
     *
     * @param hostInfo the local host info
     * @param productConfig the product config
     * @return the host info
     */
    static ModelNode createLocalHostHostInfo(final LocalHostControllerInfo hostInfo, final ProductConfig productConfig) {
        final ModelNode info = new ModelNode();
        info.get(NAME).set(hostInfo.getLocalHostName());
        info.get(RELEASE_VERSION).set(Version.AS_VERSION);
        info.get(RELEASE_CODENAME).set(Version.AS_RELEASE_CODENAME);
        info.get(MANAGEMENT_MAJOR_VERSION).set(Version.MANAGEMENT_MAJOR_VERSION);
        info.get(MANAGEMENT_MINOR_VERSION).set(Version.MANAGEMENT_MINOR_VERSION);
        final String productName = productConfig.getProductName();
        final String productVersion = productConfig.getProductVersion();
        if(productName != null) {
            info.get(PRODUCT_NAME).set(productName);
        }
        if(productVersion != null) {
            info.get(PRODUCT_VERSION).set(productVersion);
        }
        return info;
    }

}

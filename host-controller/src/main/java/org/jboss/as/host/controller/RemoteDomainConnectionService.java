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

import static java.security.AccessController.doPrivileged;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DOMAIN_MODEL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.host.controller.HostControllerLogger.ROOT_LOGGER;
import static org.jboss.as.host.controller.HostControllerMessages.MESSAGES;

import java.io.DataInput;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.ssl.SSLHandshakeException;
import javax.security.sasl.SaslException;

import org.jboss.as.controller.CurrentOperationIdHolder;
import org.jboss.as.controller.HashUtil;
import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ProxyController;
import org.jboss.as.controller.ProxyOperationAddressTranslator;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.client.MessageSeverity;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.Operation;
import org.jboss.as.controller.client.OperationAttachments;
import org.jboss.as.controller.client.OperationMessageHandler;
import org.jboss.as.controller.client.impl.ExistingChannelModelControllerClient;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.remote.RemoteProxyController;
import org.jboss.as.controller.remote.TransactionalProtocolOperationHandler;
import org.jboss.as.domain.controller.DomainController;
import org.jboss.as.domain.controller.LocalHostControllerInfo;
import org.jboss.as.domain.controller.SlaveRegistrationException;
import org.jboss.as.domain.controller.operations.ApplyExtensionsHandler;
import org.jboss.as.domain.controller.operations.ApplyMissingDomainModelResourcesHandler;
import org.jboss.as.domain.controller.operations.ApplyRemoteMasterDomainModelHandler;
import org.jboss.as.domain.controller.operations.PullDownDataForServerConfigOnSlaveHandler;
import org.jboss.as.domain.controller.operations.coordination.DomainControllerLockIdUtils;
import org.jboss.as.domain.management.SecurityRealm;
import org.jboss.as.host.controller.discovery.DiscoveryOption;
import org.jboss.as.host.controller.ignored.IgnoredDomainResourceRegistry;
import org.jboss.as.host.controller.mgmt.DomainControllerProtocol;
import org.jboss.as.host.controller.mgmt.DomainRemoteFileRequestAndHandler;
import org.jboss.as.host.controller.mgmt.HostControllerRegistrationHandler;
import org.jboss.as.host.controller.mgmt.HostInfo;
import org.jboss.as.network.NetworkUtils;
import org.jboss.as.protocol.ProtocolChannelClient;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.as.protocol.mgmt.AbstractManagementRequest;
import org.jboss.as.protocol.mgmt.ActiveOperation;
import org.jboss.as.protocol.mgmt.FlushableDataOutput;
import org.jboss.as.protocol.mgmt.ManagementChannelHandler;
import org.jboss.as.protocol.mgmt.ManagementRequestContext;
import org.jboss.as.remoting.management.ManagementRemotingServices;
import org.jboss.as.repository.HostFileRepository;
import org.jboss.as.repository.RemoteFileRequestAndHandler.CannotCreateLocalDirectoryException;
import org.jboss.as.repository.RemoteFileRequestAndHandler.DidNotReadEntireFileException;
import org.jboss.as.version.ProductConfig;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.remoting3.Endpoint;
import org.jboss.remoting3.RemotingOptions;
import org.jboss.threads.AsyncFuture;
import org.jboss.threads.AsyncFutureTask;
import org.jboss.threads.JBossThreadFactory;
import org.wildfly.security.manager.GetAccessControlContextAction;
import org.wildfly.security.manager.WildFlySecurityManager;
import org.xnio.OptionMap;
import org.xnio.Options;

/**
 * Establishes the connection from a slave {@link org.jboss.as.domain.controller.DomainController} to the master
 * {@link org.jboss.as.domain.controller.DomainController}
 *
 * @author Kabir Khan
 */
public class RemoteDomainConnectionService implements MasterDomainControllerClient, Service<MasterDomainControllerClient> {

    public static final String DOMAIN_CONNECTION_ID = "domain-connection-id";

    private static final int CONNECTION_TIMEOUT_DEFAULT = 30000;
    private static final String CONNECTION_TIMEOUT_PROPERTY = "jboss.host.domain.connection.timeout";
    private static final int CONNECTION_TIMEOUT = getSystemProperty(CONNECTION_TIMEOUT_PROPERTY, CONNECTION_TIMEOUT_DEFAULT);

    private static final ModelNode APPLY_EXTENSIONS = new ModelNode();
    private static final ModelNode APPLY_DOMAIN_MODEL = new ModelNode();
    static {
        APPLY_EXTENSIONS.get(OP).set(ApplyExtensionsHandler.OPERATION_NAME);
        APPLY_EXTENSIONS.get(OPERATION_HEADERS, "execute-for-coordinator").set(true);
        APPLY_EXTENSIONS.get(OP_ADDR).setEmptyList();
        APPLY_EXTENSIONS.protect();

        APPLY_DOMAIN_MODEL.get(OP).set(ApplyRemoteMasterDomainModelHandler.OPERATION_NAME);
        //FIXME this makes the op work after boot (i.e. slave connects to restarted master), but does not make the slave resync the servers
        APPLY_DOMAIN_MODEL.get(OPERATION_HEADERS, "execute-for-coordinator").set(true);
        APPLY_DOMAIN_MODEL.get(OP_ADDR).setEmptyList();
        APPLY_DOMAIN_MODEL.protect();
    }

    private final ExtensionRegistry extensionRegistry;
    private final ModelController controller;
    private final ProductConfig productConfig;
    private final LocalHostControllerInfo localHostInfo;
    private final RemoteFileRepository remoteFileRepository;
    private final IgnoredDomainResourceRegistry ignoredDomainResourceRegistry;
    private final HostControllerRegistrationHandler.OperationExecutor operationExecutor;
    private final DomainController domainController;
    private final HostControllerEnvironment hostControllerEnvironment;
    private final RunningMode runningMode;

    /** Used to invoke ModelController ops on the master */
    private volatile ModelControllerClient masterProxy;
    private volatile TransactionalDomainControllerClient txMasterProxy;

    private final FutureClient futureClient = new FutureClient();
    private final InjectedValue<Endpoint> endpointInjector = new InjectedValue<Endpoint>();
    private final InjectedValue<SecurityRealm> securityRealmInjector = new InjectedValue<SecurityRealm>();
    private final InjectedValue<ServerInventory> serverInventoryInjector = new InjectedValue<ServerInventory>();

    private ExecutorService executor;
    private ScheduledExecutorService scheduledExecutorService;

    private ManagementChannelHandler handler;
    private volatile RemoteDomainConnection connection;

    private RemoteDomainConnectionService(final ModelController controller, final ExtensionRegistry extensionRegistry,
                                          final LocalHostControllerInfo localHostControllerInfo, final ProductConfig productConfig,
                                          final RemoteFileRepository remoteFileRepository,
                                          final IgnoredDomainResourceRegistry ignoredDomainResourceRegistry,
                                          final HostControllerRegistrationHandler.OperationExecutor operationExecutor,
                                          final DomainController domainController,
                                          final HostControllerEnvironment hostControllerEnvironment,
                                          final RunningMode runningMode){
        this.controller = controller;
        this.extensionRegistry = extensionRegistry;
        this.productConfig = productConfig;
        this.localHostInfo = localHostControllerInfo;
        this.remoteFileRepository = remoteFileRepository;
        remoteFileRepository.setRemoteFileRepositoryExecutor(remoteFileRepositoryExecutor);
        this.ignoredDomainResourceRegistry = ignoredDomainResourceRegistry;
        this.operationExecutor = operationExecutor;
        this.domainController = domainController;
        this.hostControllerEnvironment = hostControllerEnvironment;
        this.runningMode = runningMode;
    }

    public static Future<MasterDomainControllerClient> install(final ServiceTarget serviceTarget, final ModelController controller, final ExtensionRegistry extensionRegistry,
                                                               final LocalHostControllerInfo localHostControllerInfo, final ProductConfig productConfig,
                                                               final String securityRealm, final RemoteFileRepository remoteFileRepository,
                                                               final IgnoredDomainResourceRegistry ignoredDomainResourceRegistry,
                                                               final HostControllerRegistrationHandler.OperationExecutor operationExecutor,
                                                               final DomainController domainController,
                                                               final HostControllerEnvironment hostControllerEnvironment,
                                                               final RunningMode currentRunningMode) {
        RemoteDomainConnectionService service = new RemoteDomainConnectionService(controller, extensionRegistry, localHostControllerInfo,
                productConfig, remoteFileRepository, ignoredDomainResourceRegistry, operationExecutor, domainController,
                hostControllerEnvironment, currentRunningMode);
        ServiceBuilder<MasterDomainControllerClient> builder = serviceTarget.addService(MasterDomainControllerClient.SERVICE_NAME, service)
                .addDependency(ManagementRemotingServices.MANAGEMENT_ENDPOINT, Endpoint.class, service.endpointInjector)
                .addDependency(ServerInventoryService.SERVICE_NAME, ServerInventory.class, service.serverInventoryInjector)
                .setInitialMode(ServiceController.Mode.ACTIVE);

        if (securityRealm != null) {
            SecurityRealm.ServiceUtil.addDependency(builder, service.securityRealmInjector, securityRealm, false);
        }

        builder.install();
        return service.futureClient;
    }

    /** {@inheritDoc} */
    public synchronized void register() throws IOException {
        boolean connected = false;
        List<DiscoveryOption> discoveryOptions = localHostInfo.getRemoteDomainControllerDiscoveryOptions();
        // Loop through discovery options
        for (Iterator<DiscoveryOption> i = discoveryOptions.iterator(); i.hasNext(); ) {
           DiscoveryOption discoveryOption = i.next();
           final long timeout = CONNECTION_TIMEOUT;
           final long endTime = System.currentTimeMillis() + timeout;
           int retries = 0;
           String host = null;
           int port = -1;
           try {
               // Determine the remote DC host and port to use
               discoveryOption.discover();
               host = discoveryOption.getRemoteDomainControllerHost();
               port = discoveryOption.getRemoteDomainControllerPort();
               connection.setUri(new URI("remote://" + NetworkUtils.formatPossibleIpv6Address(host) + ":" + port));

               while (!connected) {
                   try {
                       // Try to connect to the domain controller
                       connection.connect();
                       connected = true;
                   } catch (IOException e) {
                       Throwable cause = e;
                       HostControllerLogger.ROOT_LOGGER.debugf(e, "failed to connect to %s:%d", host, port);
                       while ((cause = cause.getCause()) != null) {
                           if (cause instanceof SaslException) {
                               throw MESSAGES.authenticationFailureUnableToConnect(cause);
                           } else if (cause instanceof SSLHandshakeException) {
                               throw MESSAGES.sslFailureUnableToConnect(cause);
                           } else if (cause instanceof SlaveRegistrationException) {
                               throw new IOException(cause);
                           }
                       }
                       if (System.currentTimeMillis() > endTime) {
                           throw MESSAGES.connectionToMasterTimeout(e, retries, timeout);
                       }
                       try {
                           HostControllerLogger.ROOT_LOGGER.cannotConnect(host, port, e);
                           ReconnectPolicy.CONNECT.wait(retries);
                           retries++;
                       } catch (InterruptedException ie) {
                           throw MESSAGES.connectionToMasterInterrupted();
                       }
                   }
               }
               if(connected) {
                   // Setup the transaction protocol handler
                   handler.addHandlerFactory(new TransactionalProtocolOperationHandler(controller, handler));
                   // Use the existing channel strategy
                   masterProxy = ExistingChannelModelControllerClient.createAndAdd(handler);
                   txMasterProxy = new TransactionalDomainControllerClient(handler);
                   break;
               }
           } catch (Exception e) {
               if (i.hasNext()) {
                   HostControllerLogger.ROOT_LOGGER.tryingAnotherDiscoveryOption(e);
               } else {
                   // All discovery options have been exhausted
                   HostControllerLogger.ROOT_LOGGER.noDiscoveryOptionsLeft(e);
                   throw MESSAGES.discoveryOptionsFailureUnableToConnect(e);
               }
           }
        }
    }

    /** {@inheritDoc} */
    public synchronized void unregister() {
        StreamUtils.safeClose(connection);
    }

    @Override
    public void fetchDomainWideConfiguration() {
        try {
            //TODO implement fetchDomainWideConfiguration
            throw new UnsupportedOperationException();
        } finally {
            StreamUtils.safeClose(connection);
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
    public void pullDownDataForUpdatedServerConfigAndApplyToModel(OperationContext context, String serverName, String serverGroupName, String socketBindingGroupName) throws OperationFailedException {
        ModelNode op = new ModelNode();
        op.get(OP).set(PullDownDataForServerConfigOnSlaveHandler.OPERATION_NAME);
        op.get(OP_ADDR).setEmptyList();
        op.get(SERVER).set(IgnoredNonAffectedServerGroupsUtil.createServerConfigInfo(serverName, serverGroupName, socketBindingGroupName).toModelNode());

        Integer domainControllerLock = context.getAttachment(DomainControllerLockIdUtils.DOMAIN_CONTROLLER_LOCK_ID_ATTACHMENT);
        if (domainControllerLock != null) {
            op.get(OPERATION_HEADERS, DomainControllerLockIdUtils.DOMAIN_CONTROLLER_LOCK_ID).set(domainControllerLock);
        }
        op.get(OPERATION_HEADERS, DomainControllerLockIdUtils.SLAVE_CONTROLLER_LOCK_ID).set(CurrentOperationIdHolder.getCurrentOperationID());

        ModelNode result = txMasterProxy.executeTransactional(context, op);
        if (result.get(FAILURE_DESCRIPTION).isDefined()) {
            throw new OperationFailedException(result.get(FAILURE_DESCRIPTION).asString());
        }

        ApplyMissingDomainModelResourcesHandler applyMissingDomainModelResourcesHandler = new ApplyMissingDomainModelResourcesHandler(domainController, hostControllerEnvironment, localHostInfo, ignoredDomainResourceRegistry);
        ModelNode applyMissingResourcesOp = ApplyMissingDomainModelResourcesHandler.createPulledMissingDataOperation(result.get(RESULT));
        context.addStep(applyMissingResourcesOp, applyMissingDomainModelResourcesHandler, OperationContext.Stage.MODEL, true);
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

            ThreadFactory threadFactory = new JBossThreadFactory(new ThreadGroup("domain-connection-threads"), Boolean.FALSE, null, "%G - %t", null, null, doPrivileged(GetAccessControlContextAction.getInstance()));
            this.executor = Executors.newCachedThreadPool(threadFactory);
            ThreadFactory scheduledThreadFactory = new JBossThreadFactory(new ThreadGroup("domain-connection-pinger-threads"), Boolean.TRUE, null, "%G - %t", null, null, doPrivileged(GetAccessControlContextAction.getInstance()));
            this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(scheduledThreadFactory);

            // Include additional local host information when registering at the DC
            final ModelNode hostInfo = HostInfo.createLocalHostHostInfo(localHostInfo, productConfig, ignoredDomainResourceRegistry, ReadRootResourceHandler.grabDomainResource(operationExecutor).getChildren(HOST).iterator().next());
            final OptionMap options = OptionMap.builder().set(RemotingOptions.HEARTBEAT_INTERVAL, 15000)
                    .set(Options.READ_TIMEOUT, 45000)
                    .set(RemotingOptions.RECEIVE_WINDOW_SIZE, ProtocolChannelClient.Configuration.WINDOW_SIZE).getMap();

            // Gather the required information to connect to the remote DC
            final ProtocolChannelClient.Configuration configuration = new ProtocolChannelClient.Configuration();
            // The URI will be set accordingly when looping through discovery options when registering with
            // or reconnecting to the remote DC.
            configuration.setEndpoint(endpointInjector.getValue());
            configuration.setOptionMap(options);

            final SecurityRealm realm = securityRealmInjector.getOptionalValue();
            // Create the remote domain channel strategy
            connection = new RemoteDomainConnection(localHostInfo.getLocalHostName(), hostInfo, configuration, realm,
                    localHostInfo.getRemoteDomainControllerUsername(),
                    localHostInfo.getRemoteDomainControllerDiscoveryOptions(), executor, scheduledExecutorService,
                    new RemoteDomainConnection.HostRegistrationCallback() {

                @Override
                public ModelNode resolveSubsystemVersions(ModelNode extensions) {
                    return resolveSubsystems(extensions.asList());
                }

                        @Override
                public boolean applyDomainModel(final List<ModelNode> bootOperations) {
                    // Apply the model..
                    return applyRemoteDomainModel(bootOperations);
                }

                @Override
                public void registrationComplete(ManagementChannelHandler handler) {
                    //
                }
            }, runningMode);
            // Setup the management channel handler
            handler = connection.getChannelHandler();
        } catch (Exception e) {
            throw new StartException(e);
        } finally {
            futureClient.setClient(this);
        }
        this.connection = connection;
        this.handler = handler;
    }

    /**
     * Resolve the subsystem versions.
     *
     * @param extensions the extensions to install
     * @return the subsystem versions
     */
    private ModelNode resolveSubsystems(final List<ModelNode> extensions) {

        final List<ModelNode> bootOperations = new ArrayList<ModelNode>();
        for (final ModelNode extension : extensions) {
            final ModelNode e = new ModelNode();
            e.get("domain-resource-address").add(EXTENSION, extension.asString());
            bootOperations.add(e);
        }
        final ModelNode operation = APPLY_EXTENSIONS.clone();
        operation.get(DOMAIN_MODEL).set(bootOperations);
        final ModelNode result = controller.execute(operation, OperationMessageHandler.logging, ModelController.OperationTransactionControl.COMMIT, OperationAttachments.EMPTY);
        if (!SUCCESS.equals(result.get(OUTCOME).asString())) {
            throw HostControllerMessages.MESSAGES.failedToAddExtensions(result.get(FAILURE_DESCRIPTION));
        }
        final ModelNode subsystems = new ModelNode();
        for (final ModelNode extension : extensions) {
            extensionRegistry.recordSubsystemVersions(extension.asString(), subsystems);
        }
        return subsystems;
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
            HostControllerLogger.DOMAIN_LOGGER.failedToApplyDomainConfig(e);
            return false;
        }
        // If it did not success, don't register it at the DC
        String outcome = result.get(OUTCOME).asString();
        boolean success = SUCCESS.equals(outcome);
        if (!success) {
            ModelNode failureDesc = result.hasDefined(FAILURE_DESCRIPTION) ? result.get(FAILURE_DESCRIPTION) : new ModelNode();
            HostControllerLogger.DOMAIN_LOGGER.failedToApplyDomainConfig(outcome, failureDesc);
        }
        return success;
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void stop(final StopContext context) {
        Thread executorShutdown = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    StreamUtils.safeClose(connection);
                    scheduledExecutorService.shutdownNow();
                } finally {
                    try {
                        executor.shutdown();
                    } finally {
                        context.complete();
                    }
                }
            }
        }, RemoteDomainConnectionService.class.getSimpleName() + " ExecutorService Shutdown Thread");
        executorShutdown.start();
        context.asynchronous();
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
            final File root = getDeploymentRoot(deploymentHash);
            return root.listFiles();
        }

        @Override
        public File getDeploymentRoot(byte[] deploymentHash) {
            String hex = deploymentHash == null ? "" : HashUtil.bytesToHexString(deploymentHash);
            final File file = localFileRepository.getDeploymentRoot(deploymentHash);
            if(! file.exists()) {
                return getFile(hex, DomainControllerProtocol.PARAM_ROOT_ID_DEPLOYMENT);
            }
            return file;
        }

        private File getFile(final String relativePath, final byte repoId) {
            return remoteFileRepositoryExecutor.getFile(relativePath, repoId, localFileRepository);
        }

        void setRemoteFileRepositoryExecutor(RemoteFileRepositoryExecutor remoteFileRepositoryExecutor) {
            this.remoteFileRepositoryExecutor = remoteFileRepositoryExecutor;
        }

        @Override
        public void deleteDeployment(byte[] deploymentHash) {
            localFileRepository.deleteDeployment(deploymentHash);
        }
    }

    interface RemoteFileRepositoryExecutor {
        File getFile(final String relativePath, final byte repoId, HostFileRepository localFileRepository);
    }

    private final RemoteFileRepositoryExecutor remoteFileRepositoryExecutor = new RemoteFileRepositoryExecutor() {
        public File getFile(final String relativePath, final byte repoId, HostFileRepository localFileRepository) {
            if(connection.isConnected()) {
                try {
                    return handler.executeRequest(new GetFileRequest(repoId, relativePath, localFileRepository), null).getResult().get();
                } catch (Exception e) {
                    throw MESSAGES.failedToGetFileFromRemoteRepository(e);
                }
            } else {
                return localFileRepository.getFile(relativePath);
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

    private static int getSystemProperty(final String name, final int defaultValue) {
        final String value = WildFlySecurityManager.getPropertyPrivileged(name, null);
        try {
            return value == null ? defaultValue : Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    private static class ReadRootResourceHandler implements OperationStepHandler {
        private Resource resource;

        static Resource grabDomainResource(HostControllerRegistrationHandler.OperationExecutor executor) {
            ReadRootResourceHandler handler = new ReadRootResourceHandler();
            executor.execute(new ModelNode(), OperationMessageHandler.DISCARD, ModelController.OperationTransactionControl.COMMIT, null, handler);
            return handler.resource;
        }

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            resource = context.readResourceFromRoot(PathAddress.EMPTY_ADDRESS);
            context.stepCompleted();
        }
    }

    /**
     * The tx handling code is copied from ProxyStepHandler
     *
     */
    private static class TransactionalDomainControllerClient {

        private final RemoteProxyController remoteProxy;

        public TransactionalDomainControllerClient(ManagementChannelHandler handler) {
            remoteProxy = RemoteProxyController.create(handler, PathAddress.EMPTY_ADDRESS, ProxyOperationAddressTranslator.NOOP, false);

        }

        private ModelNode executeTransactional(OperationContext context, ModelNode operation) {
            OperationMessageHandler messageHandler = new DelegatingMessageHandler(context);

            final AtomicReference<ModelController.OperationTransaction> txRef = new AtomicReference<ModelController.OperationTransaction>();
            final AtomicReference<ModelNode> preparedResultRef = new AtomicReference<ModelNode>();
            final AtomicReference<ModelNode> finalResultRef = new AtomicReference<ModelNode>();
            final ProxyController.ProxyOperationControl proxyControl = new ProxyController.ProxyOperationControl() {

                @Override
                public void operationPrepared(ModelController.OperationTransaction transaction, ModelNode result) {
                    txRef.set(transaction);
                    preparedResultRef.set(result);
                }

                @Override
                public void operationFailed(ModelNode response) {
                    finalResultRef.set(response);
                }

                @Override
                public void operationCompleted(ModelNode response) {
                    finalResultRef.set(response);
                }
            };
            remoteProxy.execute(operation, messageHandler, proxyControl, new DelegatingOperationAttachments(context));

            ModelNode finalResult = finalResultRef.get();
            if (finalResult != null) {
                // operation failed before it could commit
                return finalResult;
            }
            context.addStep(new OperationStepHandler() {
                @Override
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                    completeRemoteTransaction(context, operation, txRef, preparedResultRef, finalResultRef);
                }
            }, OperationContext.Stage.MODEL);

            return preparedResultRef.get();
        }

        private void completeRemoteTransaction(OperationContext context, ModelNode operation,
                final AtomicReference<ModelController.OperationTransaction> txRef,
                final AtomicReference<ModelNode> preparedResultRef, final AtomicReference<ModelNode> finalResultRef) {

            boolean completeStepCalled = false;
            try {
                context.completeStep(new OperationContext.ResultHandler() {
                    @Override
                    public void handleResult(OperationContext.ResultAction resultAction, OperationContext context, ModelNode operation) {
                        boolean txCompleted = false;
                        try {
                            ModelController.OperationTransaction tx = txRef.get();
                            try {
                                if (resultAction == OperationContext.ResultAction.KEEP) {
                                    tx.commit();
                                } else {
                                    tx.rollback();
                                }
                            } finally {
                                txCompleted = true;
                            }

                        } finally {
                            // Ensure the remote side gets a transaction outcome if
                            // we can't commit/rollback above
                            if (!txCompleted && txRef.get() != null) {
                                txRef.get().rollback();
                            }
                        }
                    }
                });

                completeStepCalled = true;

            } finally {
                // Ensure the remote side gets a transaction outcome if we can't
                // call completeStep above
                if (!completeStepCalled && txRef.get() != null) {
                    txRef.get().rollback();
                }
            }
        }
    }

    private static class DelegatingMessageHandler implements OperationMessageHandler {

        private final OperationContext context;

        DelegatingMessageHandler(final OperationContext context) {
            this.context = context;
        }

        @Override
        public void handleReport(MessageSeverity severity, String message) {
            context.report(severity, message);
        }
    }

    private static class DelegatingOperationAttachments implements OperationAttachments {

        private final OperationContext context;
        private DelegatingOperationAttachments(final OperationContext context) {
            this.context = context;
        }

        @Override
        public boolean isAutoCloseStreams() {
            return false;
        }

        @Override
        public List<InputStream> getInputStreams() {
            int count = context.getAttachmentStreamCount();
            List<InputStream> result = new ArrayList<InputStream>(count);
            for (int i = 0; i < count; i++) {
                result.add(context.getAttachmentStream(i));
            }
            return result;
        }

        @Override
        public void close() throws IOException {
            //
        }
    }
}

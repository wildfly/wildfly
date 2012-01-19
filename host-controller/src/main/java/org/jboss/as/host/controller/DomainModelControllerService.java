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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROFILE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RUNNING_SERVER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.host.controller.HostControllerLogger.DOMAIN_LOGGER;
import static org.jboss.as.host.controller.HostControllerLogger.ROOT_LOGGER;
import static org.jboss.as.host.controller.HostControllerMessages.MESSAGES;

import java.io.IOException;
import java.security.AccessController;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

import javax.security.auth.callback.CallbackHandler;

import org.jboss.as.controller.AbstractControllerService;
import org.jboss.as.controller.BootContext;
import org.jboss.as.controller.ControlledProcessState;
import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.ProxyController;
import org.jboss.as.controller.ProxyOperationAddressTranslator;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.client.helpers.domain.ServerStatus;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.persistence.ConfigurationPersistenceException;
import org.jboss.as.controller.persistence.ConfigurationPersister;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.remote.RemoteProxyController;
import org.jboss.as.domain.controller.DomainController;
import org.jboss.as.domain.controller.DomainModelUtil;
import org.jboss.as.domain.controller.LocalHostControllerInfo;
import org.jboss.as.domain.controller.SlaveRegistrationException;
import org.jboss.as.domain.controller.UnregisteredHostChannelRegistry;
import org.jboss.as.domain.controller.descriptions.DomainDescriptionProviders;
import org.jboss.as.domain.controller.operations.coordination.PrepareStepHandler;
import org.jboss.as.host.controller.RemoteDomainConnectionService.RemoteFileRepository;
import org.jboss.as.host.controller.mgmt.MasterDomainControllerOperationHandlerService;
import org.jboss.as.host.controller.mgmt.ServerToHostOperationHandlerFactoryService;
import org.jboss.as.host.controller.operations.HttpManagementAddHandler;
import org.jboss.as.host.controller.operations.LocalHostControllerInfoImpl;
import org.jboss.as.host.controller.operations.NativeManagementAddHandler;
import org.jboss.as.host.controller.operations.StartServersHandler;
import org.jboss.as.process.CommandLineConstants;
import org.jboss.as.process.ExitCodes;
import org.jboss.as.process.ProcessControllerClient;
import org.jboss.as.process.ProcessInfo;
import org.jboss.as.process.ProcessMessageHandler;
import org.jboss.as.remoting.EndpointService;
import org.jboss.as.remoting.management.ManagementChannelRegistryService;
import org.jboss.as.remoting.management.ManagementRemotingServices;
import org.jboss.as.repository.ContentRepository;
import org.jboss.as.repository.HostFileRepository;
import org.jboss.as.repository.LocalFileRepository;
import org.jboss.as.server.BootstrapListener;
import org.jboss.as.server.RuntimeExpressionResolver;
import org.jboss.as.server.services.security.AbstractVaultReader;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.remoting3.Channel;
import org.jboss.remoting3.CloseHandler;
import org.jboss.threads.JBossThreadFactory;

/**
 * Creates the service that acts as the {@link org.jboss.as.controller.ModelController} for a Host Controller process.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class DomainModelControllerService extends AbstractControllerService implements DomainController, UnregisteredHostChannelRegistry {

    public static final ServiceName SERVICE_NAME = HostControllerService.HC_SERVICE_NAME.append("model", "controller");

    private HostControllerConfigurationPersister hostControllerConfigurationPersister;
    private final HostControllerEnvironment environment;
    private final HostRunningModeControl runningModeControl;
    private final LocalHostControllerInfoImpl hostControllerInfo;
    private final HostFileRepository localFileRepository;
    private final RemoteFileRepository remoteFileRepository;
    private final InjectedValue<ProcessControllerConnectionService> injectedProcessControllerConnection = new InjectedValue<ProcessControllerConnectionService>();
    private final Map<String, ProxyController> hostProxies;
    private final Map<String, ProxyController> serverProxies;
    private final PrepareStepHandler prepareStepHandler;
    private final BootstrapListener bootstrapListener;
    private ManagementResourceRegistration modelNodeRegistration;

    private final Map<String, Channel> unregisteredHostChannels = new HashMap<String, Channel>();
    private final Map<String, ProxyCreatedCallback> proxyCreatedCallbacks = new HashMap<String, ProxyCreatedCallback>();
    // TODO look into using the controller executor
    final ThreadFactory threadFactory = new JBossThreadFactory(new ThreadGroup("proxy-threads"), Boolean.FALSE, null, "%G - %t", null, null, AccessController.getContext());
    private final ExecutorService proxyExecutor = Executors.newCachedThreadPool(threadFactory);
    private final AbstractVaultReader vaultReader;
    private final ContentRepository contentRepository;
    private final ExtensionRegistry extensionRegistry;
    private final ControlledProcessState processState;

    private volatile ServerInventory serverInventory;


    public static ServiceController<ModelController> addService(final ServiceTarget serviceTarget,
                                                            final HostControllerEnvironment environment,
                                                            final HostRunningModeControl runningModeControl,
                                                            final ControlledProcessState processState,
                                                            final BootstrapListener bootstrapListener) {
        final Map<String, ProxyController> hostProxies = new ConcurrentHashMap<String, ProxyController>();
        final Map<String, ProxyController> serverProxies = new ConcurrentHashMap<String, ProxyController>();
        final LocalHostControllerInfoImpl hostControllerInfo = new LocalHostControllerInfoImpl(processState);
        final AbstractVaultReader vaultReader = service(AbstractVaultReader.class);
        ROOT_LOGGER.debugf("Using VaultReader %s", vaultReader);
        final ContentRepository contentRepository = ContentRepository.Factory.create(environment.getDomainDeploymentDir());
        final PrepareStepHandler prepareStepHandler = new PrepareStepHandler(hostControllerInfo, contentRepository, hostProxies, serverProxies);
        DomainModelControllerService service = new DomainModelControllerService(environment, runningModeControl, processState,
                hostControllerInfo, contentRepository, hostProxies, serverProxies, prepareStepHandler, vaultReader, bootstrapListener);
        return serviceTarget.addService(SERVICE_NAME, service)
                .addDependency(HostControllerService.HC_EXECUTOR_SERVICE_NAME, ExecutorService.class, service.getExecutorServiceInjector())
                .addDependency(ProcessControllerConnectionService.SERVICE_NAME, ProcessControllerConnectionService.class, service.injectedProcessControllerConnection)
                .setInitialMode(ServiceController.Mode.ACTIVE)
                .install();
    }

    private DomainModelControllerService(final HostControllerEnvironment environment,
                                         final HostRunningModeControl runningModeControl,
                                         final ControlledProcessState processState,
                                         final LocalHostControllerInfoImpl hostControllerInfo,
                                         final ContentRepository contentRepository,
                                         final Map<String, ProxyController> hostProxies,
                                         final Map<String, ProxyController> serverProxies,
                                         final PrepareStepHandler prepareStepHandler,
                                         final AbstractVaultReader vaultReader,
                                         final BootstrapListener bootstrapListener) {
        super(ProcessType.HOST_CONTROLLER, runningModeControl, null, processState,
                DomainDescriptionProviders.ROOT_PROVIDER, prepareStepHandler, new RuntimeExpressionResolver(vaultReader));
        this.environment = environment;
        this.runningModeControl = runningModeControl;
        this.processState = processState;
        this.hostControllerInfo = hostControllerInfo;
        this.localFileRepository = new LocalFileRepository(environment.getDomainBaseDir(), environment.getDomainDeploymentDir(), environment.getDomainConfigurationDir());

        this.remoteFileRepository = new RemoteFileRepository(localFileRepository);
        this.contentRepository = contentRepository;
        this.hostProxies = hostProxies;
        this.serverProxies = serverProxies;
        this.prepareStepHandler = prepareStepHandler;
        this.vaultReader = vaultReader;
        this.bootstrapListener = bootstrapListener;
        this.extensionRegistry = new ExtensionRegistry(ProcessType.HOST_CONTROLLER, runningModeControl);
    }

    @Override
    public LocalHostControllerInfo getLocalHostInfo() {
        return hostControllerInfo;
    }

    @Override
    public void registerRemoteHost(ProxyController hostControllerClient) throws SlaveRegistrationException {
        if (!hostControllerInfo.isMasterDomainController()) {
            throw SlaveRegistrationException.forHostIsNotMaster();
        }

        if (runningModeControl.getRunningMode() == RunningMode.ADMIN_ONLY) {
            throw SlaveRegistrationException.forMasterInAdminOnlyMode(runningModeControl.getRunningMode());
        }
        PathAddress pa = hostControllerClient.getProxyNodeAddress();
        PathElement pe = pa.getElement(0);
        ProxyController existingController = modelNodeRegistration.getProxyController(pa);

        if (existingController != null || hostControllerInfo.getLocalHostName().equals(pe.getValue())){
            throw SlaveRegistrationException.forHostAlreadyExists(pe.getValue());
        }
        modelNodeRegistration.registerProxyController(pe, hostControllerClient);
        hostProxies.put(pe.getValue(), hostControllerClient);

        DOMAIN_LOGGER.registeredRemoteSlaveHost(pe.getValue());
    }

    @Override
    public void unregisterRemoteHost(String id) {
        unregisteredHostChannels.remove(id);
        if (hostProxies.remove(id) != null) {
            DOMAIN_LOGGER.unregisteredRemoteSlaveHost(id);
        }
        modelNodeRegistration.unregisterProxyController(PathElement.pathElement(HOST, id));
    }

    @Override
    public void registerRunningServer(ProxyController serverControllerClient) {
        PathAddress pa = serverControllerClient.getProxyNodeAddress();
        PathElement pe = pa.getElement(1);
        if (modelNodeRegistration.getProxyController(pa) != null) {
            throw MESSAGES.serverNameAlreadyRegistered(pe.getValue());
        }
        ROOT_LOGGER.registeringServer(pe.getValue());
        ManagementResourceRegistration hostRegistration = modelNodeRegistration.getSubModel(PathAddress.pathAddress(PathElement.pathElement(HOST)));
        hostRegistration.registerProxyController(pe, serverControllerClient);
        serverProxies.put(pe.getValue(), serverControllerClient);
    }

    @Override
    public void unregisterRunningServer(String serverName) {
        PathAddress pa = PathAddress.pathAddress(PathElement.pathElement(HOST, hostControllerInfo.getLocalHostName()));
        PathElement pe = PathElement.pathElement(RUNNING_SERVER, serverName);
        ROOT_LOGGER.unregisteringServer(serverName);
        ManagementResourceRegistration hostRegistration = modelNodeRegistration.getSubModel(pa);
        hostRegistration.unregisterProxyController(pe);
        serverProxies.remove(serverName);
    }

    @Override
    public ModelNode getProfileOperations(String profileName) {
        ModelNode operation = new ModelNode();

        operation.get(OP).set(DESCRIBE);
        operation.get(OP_ADDR).set(PathAddress.pathAddress(PathElement.pathElement(PROFILE, profileName)).toModelNode());

        ModelNode rsp = getValue().execute(operation, null, null, null);
        if (!rsp.hasDefined(OUTCOME) || !SUCCESS.equals(rsp.get(OUTCOME).asString())) {
            ModelNode msgNode = rsp.get(FAILURE_DESCRIPTION);
            String msg = msgNode.isDefined() ? msgNode.toString() : MESSAGES.failedProfileOperationsRetrieval();
            throw new RuntimeException(msg);
        }
        return rsp.require(RESULT);
    }

    @Override
    public HostFileRepository getLocalFileRepository() {
        return localFileRepository;
    }

    @Override
    public HostFileRepository getRemoteFileRepository() {
        if (hostControllerInfo.isMasterDomainController()) {
            throw MESSAGES.cannotAccessRemoteFileRepository();
        }
        return remoteFileRepository;
    }

    @Override
    public void start(StartContext context) throws StartException {
        final ExecutorService executorService = getExecutorServiceInjector().getValue();
        this.hostControllerConfigurationPersister = new HostControllerConfigurationPersister(environment, hostControllerInfo, executorService, extensionRegistry);
        setConfigurationPersister(hostControllerConfigurationPersister);
        prepareStepHandler.setExecutorService(executorService);
        super.start(context);
    }

    @Override
    protected void initModel(Resource rootResource, ManagementResourceRegistration rootRegistration) {
        DomainModelUtil.updateCoreModel(rootResource, environment);
        HostModelUtil.createHostRegistry(rootRegistration, hostControllerConfigurationPersister, environment, runningModeControl,
                localFileRepository, hostControllerInfo, new DelegatingServerInventory(), remoteFileRepository, contentRepository,
                this, this, extensionRegistry,vaultReader, processState);
        this.modelNodeRegistration = rootRegistration;
    }

    // See superclass start. This method is invoked from a separate non-MSC thread after start. So we can do a fair
    // bit of stuff
    @Override
    protected void boot(final BootContext context) throws ConfigurationPersistenceException {

        final ServiceTarget serviceTarget = context.getServiceTarget();
        try {
            super.boot(hostControllerConfigurationPersister.load()); // This parses the host.xml and invokes all ops

            final RunningMode currentRunningMode = runningModeControl.getRunningMode();

            // Install the core remoting endpoint and listener
            ManagementRemotingServices.installRemotingEndpoint(serviceTarget, ManagementRemotingServices.MANAGEMENT_ENDPOINT,
                    hostControllerInfo.getLocalHostName(), EndpointService.EndpointType.MANAGEMENT, null, null);

            // Now we know our management interface configuration. Install the server inventory
            ManagementChannelRegistryService.addService(serviceTarget);
            Future<ServerInventory> inventoryFuture = ServerInventoryService.install(serviceTarget, this, runningModeControl, environment,
                    hostControllerInfo.getNativeManagementInterface(), hostControllerInfo.getNativeManagementPort());

            if (!hostControllerInfo.isMasterDomainController() && !environment.isUseCachedDc()) {
                serverInventory = getFuture(inventoryFuture);

                if (hostControllerInfo.getRemoteDomainControllerHost() != null) {
                    Future<MasterDomainControllerClient> clientFuture = RemoteDomainConnectionService.install(serviceTarget,
                            getValue(),
                            hostControllerInfo.getLocalHostName(),
                            hostControllerInfo.getRemoteDomainControllerHost(),
                            hostControllerInfo.getRemoteDomainControllertPort(),
                            hostControllerInfo.getRemoteDomainControllerSecurityRealm(),
                            remoteFileRepository);
                    MasterDomainControllerClient masterDomainControllerClient = getFuture(clientFuture);
                    //Registers us with the master and gets down the master copy of the domain model to our DC
                    //TODO make sure that the RDCS checks env.isUseCachedDC, and if true falls through to that
                    try {
                        masterDomainControllerClient.register();
                    } catch (IllegalStateException e) {
                        //We could not connect to the host
                        ROOT_LOGGER.cannotConnectToMaster(e);
                        System.exit(ExitCodes.HOST_CONTROLLER_ABORT_EXIT_CODE);
                    }
                } else if (currentRunningMode != RunningMode.ADMIN_ONLY) {
                        //We could not connect to the host
                        ROOT_LOGGER.noDomainControllerConfigurationProvided(currentRunningMode,
                                CommandLineConstants.ADMIN_ONLY, RunningMode.ADMIN_ONLY);
                        System.exit(ExitCodes.HOST_CONTROLLER_ABORT_EXIT_CODE);
                }

            } else {
                // TODO look at having LocalDomainControllerAdd do this, using Stage.IMMEDIATE for the steps
                // parse the domain.xml and load the steps
                ConfigurationPersister domainPersister = hostControllerConfigurationPersister.getDomainPersister();
                super.boot(domainPersister.load());

                ManagementRemotingServices.installManagementChannelServices(serviceTarget, ManagementRemotingServices.MANAGEMENT_ENDPOINT,
                        new MasterDomainControllerOperationHandlerService(this, this),
                        DomainModelControllerService.SERVICE_NAME, ManagementRemotingServices.DOMAIN_CHANNEL, null, null);
                serverInventory = getFuture(inventoryFuture);
            }

            // TODO look into adding some of these services in the handlers, but ON-DEMAND.
            // Then here just add some simple service that demands them

            ServerToHostOperationHandlerFactoryService.install(serviceTarget, ServerInventoryService.SERVICE_NAME, proxyExecutor, localFileRepository);

            NativeManagementAddHandler.installNativeManagementServices(serviceTarget, hostControllerInfo, null, null);

            if (hostControllerInfo.getHttpManagementInterface() != null) {
                HttpManagementAddHandler.installHttpManagementServices(currentRunningMode, serviceTarget, hostControllerInfo, environment, null);
            }

            if (currentRunningMode == RunningMode.NORMAL) {
                startServers();
            }

            // TODO when to call hostControllerConfigurationPersister.successful boot? Look into this for standalone as well; may be broken now
        } finally {
            try {
                finishBoot();
            } finally {
                bootstrapListener.tick();
            }
        }
    }

    private <T> T getFuture(Future<T> future) {
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private void startServers() {
        ModelNode addr = new ModelNode();
        addr.add(HOST, hostControllerInfo.getLocalHostName());
        ModelNode op = Util.getEmptyOperation(StartServersHandler.OPERATION_NAME, addr);

        getValue().execute(op, null, null, null);
    }


    @Override
    public void stop(StopContext context) {
        serverInventory = null;
        super.stop(context);
    }


    @Override
    public void stopLocalHost() {
        stopLocalHost(0);
    }

    @Override
    public void stopLocalHost(int exitCode) {
        final ProcessControllerClient client = injectedProcessControllerConnection.getValue().getClient();
        try {
            client.shutdown(exitCode);
        } catch (IOException e) {
            throw MESSAGES.errorClosingDownHost(e);
        }
    }

    @Override
    public synchronized void registerChannel(final String hostName, final Channel channel, final ProxyCreatedCallback callback) {

        /* Disable this as part of the REM3-121 workarounds
        PathAddress addr = PathAddress.pathAddress(PathElement.pathElement(HOST, hostName));
        if (modelNodeRegistration.getProxyController(addr) != null) {
            throw new IllegalArgumentException("There is already a registered slave named '" + hostName + "'");
        }
        */
        if (unregisteredHostChannels.containsKey(hostName)) {
            throw MESSAGES.hostNameAlreadyConnected(hostName);
        }
        unregisteredHostChannels.put(hostName, channel);
        proxyCreatedCallbacks.put(hostName, callback);
        channel.addCloseHandler(new CloseHandler<Channel>() {
            public void handleClose(final Channel closed, final IOException exception) {
                unregisteredHostChannels.remove(hostName);
                proxyCreatedCallbacks.remove(hostName);
            }
        });
    }

    @Override
    public synchronized ProxyController popChannelAndCreateProxy(final String hostName) {
        final Channel channel = unregisteredHostChannels.remove(hostName);
        if (channel == null) {
            throw MESSAGES.noChannelForHost(hostName);
        }
        channel.addCloseHandler(new CloseHandler<Channel>() {
            public void handleClose(final Channel closed, final IOException exception) {
                unregisterRemoteHost(hostName);
            }
        });
        final PathAddress addr = PathAddress.pathAddress(PathElement.pathElement(ModelDescriptionConstants.HOST, hostName));
        RemoteProxyController proxy = RemoteProxyController.create(proxyExecutor, addr, ProxyOperationAddressTranslator.HOST, channel);
        ProxyCreatedCallback callback = proxyCreatedCallbacks.remove(hostName);
        if (callback != null) {
            callback.proxyCreated(proxy);
        }
        return proxy;
    }

    private class DelegatingServerInventory implements ServerInventory {
        public void serverCommunicationRegistered(String serverProcessName, Channel channel, ProxyCreatedCallback callback) {
            serverInventory.serverCommunicationRegistered(serverProcessName, channel, callback);
        }

        public void serverProcessAdded(String processName) {
            serverInventory.serverProcessAdded(processName);
        }

        public void serverStartFailed(String serverProcessName) {
            serverInventory.serverStartFailed(serverProcessName);
        }

        public void serverProcessStopped(String serverProcessName) {
            serverInventory.serverProcessStopped(serverProcessName);
        }

        public String getServerProcessName(String serverName) {
            return serverInventory.getServerProcessName(serverName);
        }

        public String getProcessServerName(String processName) {
            return serverInventory.getProcessServerName(processName);
        }

        public void processInventory(Map<String, ProcessInfo> processInfos) {
            serverInventory.processInventory(processInfos);
        }

        public Map<String, ProcessInfo> determineRunningProcesses() {
            return serverInventory.determineRunningProcesses();
        }

        public Map<String, ProcessInfo> determineRunningProcesses(boolean serversOnly) {
            return serverInventory.determineRunningProcesses(serversOnly);
        }

        public ServerStatus determineServerStatus(String serverName) {
            return serverInventory.determineServerStatus(serverName);
        }

        public int hashCode() {
            return serverInventory.hashCode();
        }

        public ServerStatus startServer(String serverName, ModelNode domainModel) {
            return serverInventory.startServer(serverName, domainModel);
        }

        public void reconnectServer(String serverName, ModelNode domainModel, boolean running) {
            serverInventory.reconnectServer(serverName, domainModel, running);
        }

        public ServerStatus restartServer(String serverName, int gracefulTimeout, ModelNode domainModel) {
            return serverInventory.restartServer(serverName, gracefulTimeout, domainModel);
        }

        public ServerStatus stopServer(String serverName, int gracefulTimeout) {
            return serverInventory.stopServer(serverName, gracefulTimeout);
        }

        public CallbackHandler getServerCallbackHandler() {
            return serverInventory.getServerCallbackHandler();
        }

        public boolean equals(Object obj) {
            return serverInventory.equals(obj);
        }

        public String toString() {
            return serverInventory.toString();
        }

        @Override
        public void stopServers(int gracefulTimeout) {
            serverInventory.stopServers(gracefulTimeout);
        }

        @Override
        public void connectionFinished() {
            serverInventory.connectionFinished();
        }

        @Override
        public void serverProcessStarted(String processName) {
            serverInventory.serverProcessStarted(processName);
        }

        @Override
        public void serverProcessRemoved(String processName) {
            serverInventory.serverProcessRemoved(processName);
        }

        @Override
        public void operationFailed(String processName, ProcessMessageHandler.OperationType type) {
            serverInventory.operationFailed(processName, type);
        }
    }

    private static <S> S service(final Class<S> service) {
        final ServiceLoader<S> serviceLoader = ServiceLoader.load(service);
        final Iterator<S> it = serviceLoader.iterator();
        if (it.hasNext())
            return it.next();
        return null;
    }
}

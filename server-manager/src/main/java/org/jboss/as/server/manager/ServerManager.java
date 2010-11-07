/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

/**
 *
 */
package org.jboss.as.server.manager;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.as.domain.client.api.HostUpdateResult;
import org.jboss.as.domain.client.api.ServerIdentity;
import org.jboss.as.domain.client.api.ServerStatus;
import org.jboss.as.domain.client.impl.HostUpdateApplierResponse;
import org.jboss.as.domain.controller.DomainController;
import org.jboss.as.domain.controller.DomainControllerImpl;
import org.jboss.as.domain.controller.FileRepository;
import org.jboss.as.domain.controller.mgmt.DomainControllerClientOperationHandler;
import org.jboss.as.model.AbstractHostModelUpdate;
import org.jboss.as.model.AbstractServerModelUpdate;
import org.jboss.as.model.DomainModel;
import org.jboss.as.model.HostModel;
import org.jboss.as.model.ManagementElement;
import org.jboss.as.model.RemoteDomainControllerElement;
import org.jboss.as.model.ServerElement;
import org.jboss.as.model.ServerGroupDeploymentElement;
import org.jboss.as.model.ServerModel;
import org.jboss.as.model.UpdateFailedException;
import org.jboss.as.model.UpdateResultHandlerResponse;
import org.jboss.as.model.socket.InterfaceElement;
import org.jboss.as.process.ProcessInfo;
import org.jboss.as.process.ProcessManagerClient;
import org.jboss.as.process.ProcessMessageHandler;
import org.jboss.as.protocol.ProtocolClient;
import org.jboss.as.server.ServerState;
import org.jboss.as.domain.controller.mgmt.DomainControllerOperationHandler;
import org.jboss.as.server.manager.mgmt.ManagementCommunicationService;
import org.jboss.as.server.manager.mgmt.ManagementCommunicationServiceInjector;
import org.jboss.as.server.manager.mgmt.ManagementOperationHandlerService;
import org.jboss.as.server.manager.mgmt.ServerManagerOperationHandler;
import org.jboss.as.server.manager.mgmt.ServerToServerManagerOperationHandler;
import org.jboss.as.services.net.NetworkInterfaceBinding;
import org.jboss.as.services.net.NetworkInterfaceService;
import org.jboss.as.threads.ThreadFactoryService;
import org.jboss.logging.Logger;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.BatchServiceBuilder;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.msc.service.ServiceActivatorContextImpl;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistryException;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.staxmapper.XMLMapper;

import javax.net.SocketFactory;

/**
 * A ServerManager.
 *
 * @author Brian Stansberry
 * @author Kabir Khan
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public class ServerManager {
    private static final Logger log = Logger.getLogger("org.jboss.as.server.manager");

    static final ServiceName SERVICE_NAME_BASE = ServiceName.JBOSS.append("server", "manager");

    private final ServerManagerEnvironment environment;
    private final StandardElementReaderRegistrar extensionRegistrar;
    private final FileRepository fileRepository;
    private final ModelManager modelManager;
    private final ServiceContainer serviceContainer = ServiceContainer.Factory.create();
    private final AtomicBoolean serversStarted = new AtomicBoolean();
    private final AtomicBoolean stopping = new AtomicBoolean();
    private final Map<String, ManagedServer> servers = new HashMap<String, ManagedServer>();

    private DomainControllerConnection domainControllerConnection;
    private InetSocketAddress managementSocketAddress;
    private ProcessManagerClient processManagerClient;
    private FallbackRepository remoteBackedRepository;

    /**
     * The auth code of the server manager itself.
     */
    private final byte[] authCode;

    public ServerManager(ServerManagerEnvironment environment, final byte[] authCode) {
        this.authCode = authCode;
        if (environment == null) {
            throw new IllegalArgumentException("bootstrapConfig is null");
        }
        this.environment = environment;
        extensionRegistrar = StandardElementReaderRegistrar.Factory.getRegistrar();
        modelManager = new ModelManager(environment, extensionRegistrar);
        fileRepository = new LocalFileRepository(environment);
    }

    public String getName() {
        return getHostModel().getName();
    }

    public Map<ServerIdentity, ServerStatus> getServerStatuses() {
        Map<ServerIdentity, ServerStatus> result = new HashMap<ServerIdentity, ServerStatus>();
        for (ServerElement se : getHostModel().getServers()) {
            ServerIdentity id = new ServerIdentity(getName(), se.getServerGroup(), se.getName());
            ServerStatus status = determineServerStatus(se);
            result.put(id, status);
        }
        return result;
    }

    private ServerStatus determineServerStatus(String serverName) {
        return determineServerStatus(getHostModel().getServer(serverName));
    }

    private ServerStatus determineServerStatus(ServerElement se) {
        ServerStatus status;
        if (se == null) {
            status = ServerStatus.DOES_NOT_EXIST;
        }
        else {
            ManagedServer client = servers.get(ManagedServer.getServerProcessName(se.getName()));
            if (client == null) {
                status = se.isStart() ? ServerStatus.STOPPED : ServerStatus.DISABLED;
            }
            else {
                switch (client.getState()) {
                    case AVAILABLE:
                    case BOOTING:
                    case STARTING:
                        status = ServerStatus.STARTING;
                        break;
                    case FAILED:
                    case MAX_FAILED:
                        status = ServerStatus.FAILED;
                        break;
                    case STARTED:
                        status = ServerStatus.STARTED;
                        break;
                    case STOPPING:
                        status = ServerStatus.STOPPING;
                        break;
                    case STOPPED:
                        status = ServerStatus.STOPPED;
                        break;
                    default:
                        throw new IllegalStateException("Unexpected state " + client.getState());
                }
            }
        }
        return status;
    }

    public ServerModel getServerModel(String serverName) {
        ManagedServer client = servers.get(ManagedServer.getServerProcessName(serverName));
        if (client == null) {
            log.debugf("Received getServerModel request for unknown server %s", serverName);
            return null;
        }
        return client.getServerModel();
    }

    /**
     * Starts the ServerManager. This brings this ServerManager to the point where
     * it has processed its own configuration file, registered with the DomainControllerImpl
     * (including starting one if the host configuration specifies that),
     * obtained the domain configuration, and launched any systems needed to make
     * this process manageable by remote clients.
     */
    public void start() throws IOException {

        modelManager.start();

        // TODO set up logging for this process based on config in Host

        //Start listening for server communication on our socket
        //launchDirectServerCommunicationHandler();

        // Start communication with the ProcessManager. This also
        // creates a daemon thread to keep this process alive
        launchProcessManagerSlave();

        final BatchBuilder batchBuilder = serviceContainer.batchBuilder();
        batchBuilder.addListener(new AbstractServiceListener<Object>() {
            @Override
            public void serviceFailed(ServiceController<?> serviceController, StartException reason) {
                log.errorf(reason, "Service [%s] failed.", serviceController.getName());
            }
        });

        final ServiceActivatorContext serviceActivatorContext = new ServiceActivatorContextImpl(batchBuilder);

        // Always activate the management port
        activateManagementCommunication(serviceActivatorContext);

        if (getHostModel().getLocalDomainControllerElement() != null) {
            activateLocalDomainController(serviceActivatorContext);
        } else {
            activateRemoteDomainControllerConnection(serviceActivatorContext);
        }

        // Last but not least the server manager service
        final ManagementElement managementElement = getHostModel().getManagementElement();
        final ServerManagerService serverManagerService = new ServerManagerService(this);
        batchBuilder.addService(SERVICE_NAME_BASE, serverManagerService)
            .addDependency(NetworkInterfaceService.JBOSS_NETWORK_INTERFACE.append(managementElement.getInterfaceName()), NetworkInterfaceBinding.class, serverManagerService.getManagementInterfaceInjector())
            .addInjection(serverManagerService.getManagementPortInjector(), managementElement.getPort())
            .addDependency(DomainControllerConnection.SERVICE_NAME, DomainControllerConnection.class, serverManagerService.getDomainControllerConnectionInjector())
            .setInitialMode(ServiceController.Mode.ACTIVE);

        try {
            batchBuilder.install();
        } catch (ServiceRegistryException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * The connection from a server to SM was closed
     */
    public void connectionClosed(String processName) {
        if (stopping.get())
            return;

        ManagedServer server = servers.get(processName);

        if (server == null) {
            log.errorf("No server called %s with a closed connection", processName);
            return;
        }

        ServerState state = server.getState();
        if (state == ServerState.STOPPED || state == ServerState.STOPPING || state == ServerState.MAX_FAILED) {
            log.debugf("Ignoring closed connection for server %s in the %s state", processName, state);
            return;
        }
    }

    public List<HostUpdateResult<?>> applyHostUpdates(final List<AbstractHostModelUpdate<?>> updates) {

        List<HostUpdateApplierResponse> hostResults = getModelManager().applyHostModelUpdates(updates);
        boolean allowOverallRollback = true; // FIXME make allowOverallRollback configurable
        return applyUpdatesToServers(updates, hostResults, allowOverallRollback);
    }

    public List<UpdateResultHandlerResponse<?>> applyUpdatesToServer(final ServerIdentity server, final List<AbstractServerModelUpdate<?>> updates, final boolean allowOverallRollback) {

        ManagedServer client = servers.get(ManagedServer.getServerProcessName(server.getServerName()));
        List<UpdateResultHandlerResponse<?>> responses;
        if (client == null) {
            // TODO better handle disappearance of server
            responses = new ArrayList<UpdateResultHandlerResponse<?>>();
            UpdateResultHandlerResponse<?> failure = UpdateResultHandlerResponse.createFailureResponse(new IllegalStateException("unknown host " + server.getHostName()));
            for (int i = 0; i < updates.size(); i++) {
                responses.add(failure);
            }
        }
        else {
            responses = client.applyUpdates(updates, allowOverallRollback);
        }
        return responses;
    }

    public List<UpdateResultHandlerResponse<?>> applyServerUpdates(final String serverName, final List<AbstractServerModelUpdate<?>> updates,
            boolean allowOverallRollback) {
        final ManagedServer server = servers.get(ManagedServer.getServerProcessName(serverName));
        if(server == null) {
            log.debugf("Cannot apply updates to unknown server %s", serverName);
            UpdateResultHandlerResponse<?> urhr = UpdateResultHandlerResponse.createFailureResponse(new UpdateFailedException("No server available with name " + serverName));
            int size = updates.size();
            List<UpdateResultHandlerResponse<?>> list = new ArrayList<UpdateResultHandlerResponse<?>>(size);
            for (int i = 0; i < size; i++) {
                list.add(urhr);
            }
            return list;
        }
        return server.applyUpdates(updates, allowOverallRollback);
    }

    /**
     * Callback for when we receive the SERVER_AVAILABLE message from a Server
     *
     * @param serverName the name of the server
     */
    void availableServer(String serverName) {
        try {
            ManagedServer server = servers.get(serverName);
            if (server == null) {
                log.errorf("No server called %s available", serverName);
                return;
            }
            checkState(server, ServerState.BOOTING);

            server.setState(ServerState.AVAILABLE);
            log.infof("Sending config to server %s", serverName);
            server.startServerProcess();
            server.setState(ServerState.STARTING);
        } catch (IOException e) {
            log.errorf(e, "Could not start server %s", serverName);
        }
    }

    /**
     * Callback for when we receive the SHUTDOWN message from PM
     */
    public void stop() {
        if (stopping.getAndSet(true)) {
            return;
        }

        log.info("Stopping ServerManager");
        if(domainControllerConnection != null) {
            domainControllerConnection.unregister();
        }
        serviceContainer.shutdown();
        // FIXME stop any local DomainControllerImpl, stop other internal SM services
    }

    /**
     * Callback for when we receive the
     * {@link ServerToServerManagerProtocolCommand#SERVER_STOPPED}
     * message from a Server
     *
     * @param serverName the name of the server
     */
    void stoppedServer(String serverName) {
        if (stopping.get())
            return;

        ManagedServer server = servers.get(serverName);
        if (server == null) {
            log.errorf("No server called %s exists for stop", serverName);
            return;
        }
        checkState(server, ServerState.STOPPING);

        try {
            processManagerClient.stopProcess(serverName);
        } catch (IOException e) {
            if (stopping.get())
                return;
            log.errorf(e, "Could not stop server %s in PM", serverName);
        }
        try {
            processManagerClient.removeProcess(serverName);
        } catch (IOException e) {
            if (stopping.get())
                return;
            log.errorf(e, "Could not stop server %s", serverName);
        }
    }

    /**
     * Callback for when we receive the
     * {@link ServerToServerManagerProtocolCommand#SERVER_STARTED}
     * message from a Server
     *
     * @param serverName the name of the server
     */
    void startedServer(String serverName) {
        ManagedServer server = servers.get(serverName);
        if (server == null) {
            log.errorf("No server called %s exists for start", serverName);
            return;
        }
        checkState(server, ServerState.STARTING);
        server.setState(ServerState.STARTED);
    }

    /**
     * Callback for when we receive the
     * {@link ServerToServerManagerProtocolCommand#SERVER_START_FAILED}
     * message from a Server
     *
     * @param serverName the name of the server
     */
    void failedStartServer(String serverName) {
        ManagedServer server = servers.get(serverName);
        if (server == null) {
            log.errorf("No server called %s exists", serverName);
            return;
        }
        checkState(server, ServerState.STARTING);
        server.setState(ServerState.FAILED);
    }

    /**
     * Callback for when we receive the
     * {@link ServerToServerManagerProtocolCommand#SERVER_RECONNECT_STATUS}
     * message from a Server
     *
     * @param serverName the name of the server
     * @param state the server's state
     */
    void reconnectedServer(String serverName, ServerState state) {
        ManagedServer server = servers.get(serverName);
        if (server == null) {
            log.errorf("No server found for reconnected server %s", serverName);
            return;
        }

        server.setState(state);

        if (state.isRestartOnReconnect()) {
            try {
                server.startServerProcess();
            } catch (IOException e) {
                log.errorf(e, "Could not start reconnected server %s", server.getServerProcessName());
            }
        }
    }

    /**
     * Handles a notification from the ProcessManager that a server has
     * gone down.
     *
     * @param downServerName the process name of the server.
     */
    public void downServer(String downServerName) {
        ManagedServer server = servers.get(downServerName);
        if (server == null) {
            log.errorf("No server called %s exists", downServerName);
            return;
        }

        if (environment.isRestart() && server.getState() == ServerState.BOOTING && environment.getServerManagerPort() == 0) {
            //If this was a restarted SM and a server went down while we were down, PM will send the DOWN message. If the port
            //is 0, it will be different following a restart so remove and re-add the server with the new port here
            try {
                server.removeServerProcess();
                server.addServerProcess();
            } catch (IOException e) {
                log.errorf("Error removing and adding process %s", downServerName);
                return;
            }
            try {
                server.startServerProcess();
            } catch (IOException e) {
                // AutoGenerated
                throw new RuntimeException(e);
            }

        } else {
            server.setState(ServerState.FAILED);
        }
    }

    private void launchProcessManagerSlave() throws IOException {
        final ProtocolClient.Configuration configuration = new ProtocolClient.Configuration();
        configuration.setReadExecutor(Executors.newCachedThreadPool());
        configuration.setServerAddress(new InetSocketAddress(environment.getProcessManagerAddress(), environment.getProcessManagerPort().intValue()));
        configuration.setThreadFactory(Executors.defaultThreadFactory());
        configuration.setSocketFactory(SocketFactory.getDefault());
        processManagerClient = ProcessManagerClient.connect(configuration, authCode, new ProcessMessageHandler() {
            public void handleProcessAdded(final ProcessManagerClient client, final String processName) {
            }

            public void handleProcessStarted(final ProcessManagerClient client, final String processName) {
            }

            public void handleProcessStopped(final ProcessManagerClient client, final String processName, final long uptimeMillis) {
            }

            public void handleProcessRemoved(final ProcessManagerClient client, final String processName) {
            }

            public void handleProcessInventory(final ProcessManagerClient client, final Map<String, ProcessInfo> inventory) {
                // TODO: reconcile our server list against the PM inventory
            }

            public void handleConnectionShutdown(final ProcessManagerClient client) {
            }

            public void handleConnectionFailure(final ProcessManagerClient client, final IOException cause) {
            }

            public void handleConnectionFinished(final ProcessManagerClient client) {
            }
        });
        processManagerClient.requestProcessInventory();
    }

    private void activateLocalDomainController(final ServiceActivatorContext serviceActivatorContext) {
        try {
            final BatchBuilder batchBuilder = serviceActivatorContext.getBatchBuilder();

            final XMLMapper mapper = XMLMapper.Factory.create();
            extensionRegistrar.registerStandardDomainReaders(mapper);

            final DomainControllerImpl domainController = new DomainControllerImpl();

            batchBuilder.addService(DomainController.SERVICE_NAME, domainController)
                .addInjection(domainController.getXmlMapperInjector(), mapper)
                .addInjection(domainController.getDomainConfigDirInjector(), environment.getDomainConfigurationDir())
                .addInjection(domainController.getDomainDeploymentsDirInjector(), environment.getDomainDeploymentDir())
                .addDependency(SERVICE_NAME_BASE.append("executor"), ScheduledExecutorService.class, domainController.getScheduledExecutorServiceInjector());

            final DomainControllerOperationHandler domainControllerOperationHandler = new DomainControllerOperationHandler();
            batchBuilder.addService(DomainControllerOperationHandler.SERVICE_NAME, domainControllerOperationHandler)
                .addDependency(DomainController.SERVICE_NAME, DomainController.class, domainControllerOperationHandler.getDomainControllerInjector())
                .addDependency(SERVICE_NAME_BASE.append("executor"), ScheduledExecutorService.class, domainControllerOperationHandler.getExecutorServiceInjector())
                .addDependency(SERVICE_NAME_BASE.append("thread-factory"), ThreadFactory.class, domainControllerOperationHandler.getThreadFactoryInjector())
                .addInjection(domainControllerOperationHandler.getLocalFileRepositoryInjector(), fileRepository)
                .addDependency(ManagementCommunicationService.SERVICE_NAME, ManagementCommunicationService.class, new ManagementCommunicationServiceInjector(domainControllerOperationHandler));

            final DomainControllerClientOperationHandler domainControllerClientOperationHandler = new DomainControllerClientOperationHandler();
            batchBuilder.addService(DomainControllerClientOperationHandler.SERVICE_NAME, domainControllerClientOperationHandler)
                .addDependency(DomainController.SERVICE_NAME, DomainController.class, domainControllerClientOperationHandler.getDomainControllerInjector())
                .addDependency(ManagementCommunicationService.SERVICE_NAME, ManagementCommunicationService.class, new ManagementCommunicationServiceInjector(domainControllerClientOperationHandler));

            batchBuilder.addService(DomainControllerConnection.SERVICE_NAME, new LocalDomainControllerConnection(ServerManager.this, domainController, fileRepository))
                .addDependency(DomainController.SERVICE_NAME);

        } catch (Exception e) {
            throw new RuntimeException("Exception starting local domain controller", e);
        }
    }

    private void activateRemoteDomainControllerConnection(final ServiceActivatorContext serviceActivatorContext) {
        final BatchBuilder batchBuilder = serviceActivatorContext.getBatchBuilder();

        final DomainControllerConnectionService domainControllerClientService = new DomainControllerConnectionService(this, fileRepository, 10L);
        final BatchServiceBuilder<DomainControllerConnection> serviceBuilder = batchBuilder.addService(DomainControllerConnectionService.SERVICE_NAME, domainControllerClientService)
            .addListener(new AbstractServiceListener<DomainControllerConnection>() {
                @Override
                public void serviceFailed(ServiceController<? extends DomainControllerConnection> serviceController, StartException reason) {
                    log.error("Failed to register with domain controller.", reason);
                }
            })
            .addAliases(DomainControllerConnection.SERVICE_NAME)
            .setInitialMode(ServiceController.Mode.ACTIVE);

        HostModel hostConfig = getHostModel();
        final RemoteDomainControllerElement remoteDomainControllerElement = hostConfig.getRemoteDomainControllerElement();
        final InetAddress hostAddress;
        try {
            hostAddress = InetAddress.getByName(remoteDomainControllerElement.getHost());
        } catch (UnknownHostException e) {
            throw new RuntimeException("Failed to get remote domain controller address", e);
        }
        serviceBuilder.addInjection(domainControllerClientService.getDomainControllerAddressInjector(), hostAddress);
        serviceBuilder.addInjection(domainControllerClientService.getDomainControllerPortInjector(), remoteDomainControllerElement.getPort());

        final ManagementElement managementElement = hostConfig.getManagementElement();
        serviceBuilder.addDependency(NetworkInterfaceService.JBOSS_NETWORK_INTERFACE.append(managementElement.getInterfaceName()), NetworkInterfaceBinding.class, domainControllerClientService.getLocalManagementInterfaceInjector());
        serviceBuilder.addInjection(domainControllerClientService.getLocalManagementPortInjector(), managementElement.getPort());
        serviceBuilder.addDependency(SERVICE_NAME_BASE.append("executor"), ScheduledExecutorService.class, domainControllerClientService.getExecutorServiceInjector());
        serviceBuilder.addDependency(SERVICE_NAME_BASE.append("thread-factory"), ThreadFactory.class, domainControllerClientService.getThreadFactoryInjector());
    }

    private void activateManagementCommunication(final ServiceActivatorContext serviceActivatorContext) {
        final BatchBuilder batchBuilder = serviceActivatorContext.getBatchBuilder();

        HostModel hostConfig = getHostModel();
        final ManagementElement managementElement = hostConfig.getManagementElement();
        if(managementElement == null) {
            throw new IllegalStateException("null management configuration");
        }
        final Set<InterfaceElement> hostInterfaces = hostConfig.getInterfaces();
        if(hostInterfaces != null) {
            for(InterfaceElement interfaceElement : hostInterfaces) {
                if(interfaceElement.getName().equals(managementElement.getInterfaceName())) {
                    interfaceElement.activate(serviceActivatorContext);
                    break;
                }
            }
        }

        // Add the executor
        final ServiceName threadFactoryServiceName = SERVICE_NAME_BASE.append("thread-factory");
        batchBuilder.addService(threadFactoryServiceName, new ThreadFactoryService());
        final ServiceName executorServiceName = SERVICE_NAME_BASE.append("executor");

        /**
         * Replace below with fixed ScheduledThreadPoolService
         */
        final InjectedValue<ThreadFactory> threadFactoryValue = new InjectedValue<ThreadFactory>();
        batchBuilder.addService(executorServiceName, new Service<ScheduledExecutorService>() {
            private ScheduledExecutorService executorService;
            public synchronized void start(StartContext context) throws StartException {
                executorService = Executors.newScheduledThreadPool(20, threadFactoryValue.getValue());
            }

            public synchronized void stop(StopContext context) {
                executorService.shutdown();
            }

            public synchronized ScheduledExecutorService getValue() throws IllegalStateException {
                return executorService;
            }
        }).addDependency(threadFactoryServiceName, ThreadFactory.class, threadFactoryValue);

        //  Add the management communication service
        final ManagementCommunicationService managementCommunicationService = new ManagementCommunicationService();
        batchBuilder.addService(ManagementCommunicationService.SERVICE_NAME, managementCommunicationService)
            .addDependency(NetworkInterfaceService.JBOSS_NETWORK_INTERFACE.append(managementElement.getInterfaceName()), NetworkInterfaceBinding.class, managementCommunicationService.getInterfaceInjector())
            .addInjection(managementCommunicationService.getPortInjector(), managementElement.getPort())
            .addDependency(executorServiceName, ExecutorService.class, managementCommunicationService.getExecutorServiceInjector())
            .addDependency(threadFactoryServiceName, ThreadFactory.class, managementCommunicationService.getThreadFactoryInjector())
            .setInitialMode(ServiceController.Mode.ACTIVE);

        //  Add the DC to server manager operation handler
        final ManagementOperationHandlerService<ServerManagerOperationHandler> operationHandlerService
                = new ManagementOperationHandlerService<ServerManagerOperationHandler>(new ServerManagerOperationHandler(this));
            batchBuilder.addService(ManagementCommunicationService.SERVICE_NAME.append("server", "manager"), operationHandlerService)
                .addDependency(ManagementCommunicationService.SERVICE_NAME, ManagementCommunicationService.class, new ManagementCommunicationServiceInjector(operationHandlerService));

        //  Add the server to server manager operation handler
        final ManagementOperationHandlerService<ServerToServerManagerOperationHandler> serverOperationHandlerService
                = new ManagementOperationHandlerService<ServerToServerManagerOperationHandler>(new ServerToServerManagerOperationHandler(this));
            batchBuilder.addService(ManagementCommunicationService.SERVICE_NAME.append("server", "to", "server", "manager"), serverOperationHandlerService)
                .addDependency(ManagementCommunicationService.SERVICE_NAME, ManagementCommunicationService.class,  new ManagementCommunicationServiceInjector(serverOperationHandlerService));
    }

    void setDomainControllerConnection(final DomainControllerConnection domainControllerConnection) {
        this.domainControllerConnection = domainControllerConnection;

        // By having a remote repo as a secondary content will be synced only if needed
        FallbackRepository repository = new FallbackRepository(fileRepository, domainControllerConnection.getRemoteFileRepository());
        modelManager.setFileRepository(repository);
        this.remoteBackedRepository = repository;
    }

    void setManagementSocketAddress(InetSocketAddress managementSocketAddress) {
        this.managementSocketAddress = managementSocketAddress;
    }

    public ModelManager getModelManager() {
        return modelManager;
    }

    protected DomainModel getDomainModel() {
        return modelManager.getDomainModel();
    }

    protected HostModel getHostModel() {
        return modelManager.getHostModel();
    }

    /**
     * Set the domain for the server manager.  If this is the first time the domain has been set on this instance it will
     * also invoke the server launch process.
     *
     * @param domain The domain configuration
     */
    public void setDomain(final DomainModel domain) {
        modelManager.setDomainModel(domain);
    }

    public ManagedServer getServer(String name) {
        return servers.get(name);
    }

    private void checkState(ManagedServer server, ServerState expected) {
        ServerState state = server.getState();
        if (state != expected) {
            log.warnf("Server %s is not in the expected %s state: %s" , server.getServerProcessName(), expected, state);
        }
    }

    public Map<String, ManagedServer> getServers() {
        synchronized (servers) {
            return Collections.unmodifiableMap(servers);
        }
    }

    private void synchronizeDeployments() {
        DomainModel domainConfig = getDomainModel();
        Set<String> serverGroupNames = domainConfig.getServerGroupNames();
        for (ServerElement server : getHostModel().getServers()) {
            String serverGroupName = server.getServerGroup();
            if (serverGroupNames.remove(serverGroupName)) {
                for (ServerGroupDeploymentElement deployment : domainConfig.getServerGroup(serverGroupName).getDeployments()) {
                    // Force a sync
                    remoteBackedRepository.getDeploymentFiles(deployment.getSha1Hash());
                }
            }
        }
    }

    void startServers() {
        if(serversStarted.compareAndSet(false, true)) {
            if (!environment.isRestart()) {
                synchronizeDeployments();
                HostModel hostConfig = getHostModel();
                for (ServerElement serverEl : hostConfig.getServers()) {
                    // TODO take command line input on what servers to start
                    if (serverEl.isStart()) {
                        String serverName = serverEl.getName();
                        log.info("Starting server " + serverName);
                        try {
                            startServer(serverName, managementSocketAddress);
                        } catch (IOException e) {
                            // FIXME handle failure to start server
                            log.error("Failed to start server " + serverName, e);
                        }
                    }
                    else log.info("Server " + serverEl.getName() + " is configured to not be started");
                }
            } else {
                // FIXME -- this got dropped in the move to an update-based boot
                // handle it properly
//                reconnectServers();
            }
        }
    }

    public ServerStatus startServer(String serverName) {
        try {
            String processName = ManagedServer.getServerProcessName(serverName);
            ManagedServer server = servers.get(processName);
            boolean canStart = true;
            if (server != null) {
                if (server.getState() != ServerState.STOPPED) {
                    log.warnf("Received request to start server %s but it is not stopped; server state is ", serverName, server.getState());
                    canStart = false;
                }
                else {
                    server.removeServerProcess();
                    servers.remove(processName);
                }
            }
            if (canStart) {
                startServer(serverName, managementSocketAddress);
            }
        }
        catch (Exception e) {
            log.errorf(e, "Failed to start server %s", serverName);
        }

        ServerStatus status = determineServerStatus(serverName);
        // FIXME total hack; set up some sort of notification scheme
        for (int i = 0; i < 50; i++) {
            status = determineServerStatus(serverName);
            if (status == ServerStatus.STARTING) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            else {
                break;
            }
        }

        return status;
    }

    public ServerStatus stopServer(String serverName, long gracefulTimeout) {
        try {
            String processName = ManagedServer.getServerProcessName(serverName);
            ManagedServer server = servers.get(processName);
            if (server != null) {
                if (gracefulTimeout > -1) {
                    // FIXME implement gracefulShutdown
                    //server.gracefulShutdown(gracefulTimeout);
                    // FIXME figure out how/when server.removeServerProcess() && servers.remove(processName) happens

                    // Workaround until the above is fixed
                    log.warnf("Graceful shutdown of server %s was requested but is not presently supported. " +
                              "Falling back to rapid shutdown.", serverName);
                    server.stopServerProcess();
                    server.removeServerProcess();
                    servers.remove(processName);
                }
                else {
                    server.stopServerProcess();
                    server.removeServerProcess();
                    servers.remove(processName);
                }
            }
        }
        catch (Exception e) {
            log.errorf(e, "Failed to stop server %s", serverName);
        }

        return determineServerStatus(serverName);
    }

    public ServerStatus restartServer(String serverName, long gracefulTimeout) {
        stopServer(serverName, gracefulTimeout);
        return startServer(serverName);
    }

    private void startServer(String serverName, InetSocketAddress managementSocket) throws IOException {
        ManagedServer server = new ManagedServer(serverName, getDomainModel(), getHostModel(), environment, processManagerClient, managementSocket);
        servers.put(server.getServerProcessName(), server);
        server.addServerProcess();
        server.startServerProcess();
    }

    private List<HostUpdateResult<?>> applyUpdatesToServers(final List<AbstractHostModelUpdate<?>> updates,
                                                              final List<HostUpdateApplierResponse> hostResults,
                                                              final boolean allowOverallRollback) {
        List<HostUpdateResult<?>> result;
        Map<AbstractHostModelUpdate<?>, AbstractServerModelUpdate<?>> serverByHost = new HashMap<AbstractHostModelUpdate<?>, AbstractServerModelUpdate<?>>();
        Map<AbstractServerModelUpdate<?>, HostUpdateResult<Object>> resultsByUpdate = new HashMap<AbstractServerModelUpdate<?>, HostUpdateResult<Object>>();
        for (int i = 0; i < updates.size(); i++) {
            AbstractHostModelUpdate<?> hostUpdate = updates.get(i);
            AbstractServerModelUpdate<?> serverUpdate = hostUpdate.getServerModelUpdate();
            if (serverUpdate != null) {
                serverByHost.put(hostUpdate, serverUpdate);
                resultsByUpdate.put(serverUpdate, new HostUpdateResult<Object>());
            }
        }
        Map<ServerIdentity, List<AbstractServerModelUpdate<?>>> updatesByServer =
                getUpdatesByServer(updates, hostResults, serverByHost);

        // TODO Add param to configure pushing out concurrently
        for (Map.Entry<ServerIdentity, List<AbstractServerModelUpdate<?>>> entry : updatesByServer.entrySet()) {
            ServerIdentity server = entry.getKey();
            List<AbstractServerModelUpdate<?>> serverUpdates = entry.getValue();
            // Push them out
            List<UpdateResultHandlerResponse<?>> rsps = applyUpdatesToServer(server, serverUpdates,
                    allowOverallRollback);
            for (int i = 0; i < serverUpdates.size(); i++) {
                UpdateResultHandlerResponse<?> rsp = rsps.get(i);
                AbstractServerModelUpdate<?> serverUpdate = entry.getValue().get(i);
                HostUpdateResult<Object> hur = resultsByUpdate.get(serverUpdate);

                if (rsp.isCancelled()) {
                    hur = hur.newWithAddedCancellation(server);
                } else if (rsp.isTimedOut()) {
                    hur = hur.newWithAddedTimeout(server);
                } else if (rsp.isRolledBack()) {
                    hur = hur.newWithAddedRollback(server);
                } else if (rsp.getFailureResult() != null) {
                    hur = hur.newWithAddedFailure(server, rsp.getFailureResult());
                } else {
                    hur = hur.newWithAddedResult(server, rsp.getSuccessResult());
                }
                resultsByUpdate.put(serverUpdate, hur);
            }
        }

        result = new ArrayList<HostUpdateResult<?>>();
        for (AbstractHostModelUpdate<?> hostUpdate : updates) {
            AbstractServerModelUpdate<?> serverUpdate = serverByHost.get(hostUpdate);
            HostUpdateResult<?> hur = resultsByUpdate.get(serverUpdate);
            if (hur == null) {
                // Update did not impact servers
                hur = new HostUpdateResult<Object>();
            }
            result.add(hur);
        }
        return result;
    }

    private Map<ServerIdentity, List<AbstractServerModelUpdate<?>>> getUpdatesByServer(
            final List<AbstractHostModelUpdate<?>> hostUpdates,
            final List<HostUpdateApplierResponse> hostResults,
            final Map<AbstractHostModelUpdate<?>, AbstractServerModelUpdate<?>> serverByHost) {

        Map<ServerIdentity, List<AbstractServerModelUpdate<?>>> result = new HashMap<ServerIdentity, List<AbstractServerModelUpdate<?>>>();

        for (int i = 0; i < hostResults.size(); i++) {
            HostUpdateApplierResponse domainResult = hostResults.get(i);
            AbstractHostModelUpdate<?> domainUpdate = hostUpdates.get(i);
            AbstractServerModelUpdate<?> serverUpdate = serverByHost.get(domainUpdate);
            for (ServerIdentity server : domainResult.getServers()) {
                List<AbstractServerModelUpdate<?>> serverList = result.get(server);
                if (serverList == null) {
                    serverList = new ArrayList<AbstractServerModelUpdate<?>>();
                    result.put(server, serverList);
                }
                serverList.add(serverUpdate);
            }
        }
        return result;
    }
}

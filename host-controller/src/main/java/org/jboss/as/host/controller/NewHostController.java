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
package org.jboss.as.host.controller;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DOMAIN_CONTROLLER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.JVM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.LOCAL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAMESPACES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PORT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_OPERATION_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_OPERATION_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SCHEMA_LOCATIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_PORT_OFFSET;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.START;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSTEM_PROPERTY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.SocketFactory;

import org.jboss.as.controller.BasicModelController;
import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.NewOperationContextImpl;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.common.CommonProviders;
import org.jboss.as.controller.operations.common.InterfaceAddHandler;
import org.jboss.as.controller.operations.common.InterfaceRemoveHandler;
import org.jboss.as.controller.operations.common.ManagementSocketAddHandler;
import org.jboss.as.controller.operations.common.NamespaceAddHandler;
import org.jboss.as.controller.operations.common.NamespaceRemoveHandler;
import org.jboss.as.controller.operations.common.SchemaLocationAddHandler;
import org.jboss.as.controller.operations.common.SchemaLocationRemoveHandler;
import org.jboss.as.controller.operations.global.GlobalOperationHandlers;
import org.jboss.as.controller.operations.global.WriteAttributeHandlers;
import org.jboss.as.controller.persistence.ConfigurationPersistenceException;
import org.jboss.as.controller.persistence.NewConfigurationPersister;
import org.jboss.as.controller.registry.AttributeAccess.Storage;
import org.jboss.as.controller.registry.ModelNodeRegistration;
import org.jboss.as.domain.client.api.HostUpdateResult;
import org.jboss.as.domain.client.api.ServerIdentity;
import org.jboss.as.domain.client.api.ServerStatus;
import org.jboss.as.domain.client.impl.HostUpdateApplierResponse;
import org.jboss.as.domain.controller.DomainController;
import org.jboss.as.domain.controller.DomainControllerImpl;
import org.jboss.as.domain.controller.FileRepository;
import org.jboss.as.domain.controller.mgmt.DomainControllerClientOperationHandler;
import org.jboss.as.domain.controller.mgmt.DomainControllerOperationHandler;
import org.jboss.as.host.controller.descriptions.HostDescriptionProviders;
import org.jboss.as.host.controller.mgmt.ManagementCommunicationService;
import org.jboss.as.host.controller.mgmt.ManagementCommunicationServiceInjector;
import org.jboss.as.host.controller.mgmt.ManagementOperationHandlerService;
import org.jboss.as.host.controller.mgmt.NewHostControllerOperationHandler;
import org.jboss.as.host.controller.mgmt.NewServerToHostControllerOperationHandler;
import org.jboss.as.host.controller.operations.LocalDomainControllerAddHandler;
import org.jboss.as.host.controller.operations.LocalDomainControllerRemoveHandler;
import org.jboss.as.host.controller.operations.ServerAddHandler;
import org.jboss.as.host.controller.operations.ServerRemoveHandler;
import org.jboss.as.host.controller.operations.SpecifiedInterfaceAddHandler;
import org.jboss.as.host.controller.operations.SpecifiedInterfaceRemoveHandler;
import org.jboss.as.model.AbstractHostModelUpdate;
import org.jboss.as.model.AbstractServerModelUpdate;
import org.jboss.as.model.DomainModel;
import org.jboss.as.model.HostModel;
import org.jboss.as.model.ManagementElement;
import org.jboss.as.model.RemoteDomainControllerElement;
import org.jboss.as.model.ServerGroupDeploymentElement;
import org.jboss.as.model.ServerModel;
import org.jboss.as.model.UpdateFailedException;
import org.jboss.as.model.UpdateResultHandlerResponse;
import org.jboss.as.process.ProcessControllerClient;
import org.jboss.as.process.ProcessInfo;
import org.jboss.as.process.ProcessMessageHandler;
import org.jboss.as.protocol.ProtocolClient;
import org.jboss.as.server.ServerState;
import org.jboss.as.server.operations.SystemPropertyAddHandler;
import org.jboss.as.server.operations.SystemPropertyRemoveHandler;
import org.jboss.as.server.services.net.NetworkInterfaceBinding;
import org.jboss.as.server.services.net.NetworkInterfaceService;
import org.jboss.as.threads.ThreadFactoryService;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;
import org.jboss.logging.Logger;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.msc.service.ServiceActivatorContextImpl;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.staxmapper.XMLMapper;

/**
 * A HostController.
 *
 * @author Brian Stansberry
 * @author Kabir Khan
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public class NewHostController extends BasicModelController {
    private static final Logger log = Logger.getLogger("org.jboss.as.host.controller");

    static final ServiceName SERVICE_NAME_BASE = ServiceName.JBOSS.append("host", "controller");

    private final HostControllerEnvironment environment;
    private final StandardElementReaderRegistrar extensionRegistrar;
    private final FileRepository fileRepository;
    private final ModelManager modelManager;
    private final ServiceContainer serviceContainer = ServiceContainer.Factory.create();
    private final AtomicBoolean serversStarted = new AtomicBoolean();
    private final AtomicBoolean stopping = new AtomicBoolean();
    private final Map<String, NewManagedServer> servers = new HashMap<String, NewManagedServer>();

    private DomainControllerConnection domainControllerConnection;
    private InetSocketAddress managementSocketAddress;
    private ProcessControllerClient processControllerClient;
    private FallbackRepository remoteBackedRepository;

    //New detyped fields
    private final NewConfigurationPersister configurationPersister;

    /**
     * The auth code of the host controller itself.
     */
    private final byte[] authCode;

    public NewHostController(final HostControllerEnvironment environment, final byte[] authCode, final NewConfigurationPersister configurationPersister) {
        super(configurationPersister, HostDescriptionProviders.ROOT_PROVIDER);
        this.authCode = authCode;
        if (environment == null) {
            throw new IllegalArgumentException("bootstrapConfig is null");
        }
        this.environment = environment;
        extensionRegistrar = StandardElementReaderRegistrar.Factory.getRegistrar();
        modelManager = new ModelManager(environment, extensionRegistrar);
        fileRepository = new LocalFileRepository(environment);

        this.configurationPersister = configurationPersister;

        createCoreModel();

        // Register the operation handlers
        ModelNodeRegistration root = getRegistry();
        // Global operations
        root.registerOperationHandler(READ_RESOURCE_OPERATION, GlobalOperationHandlers.READ_RESOURCE, CommonProviders.READ_RESOURCE_PROVIDER, true);
        root.registerOperationHandler(READ_ATTRIBUTE_OPERATION, GlobalOperationHandlers.READ_ATTRIBUTE, CommonProviders.READ_ATTRIBUTE_PROVIDER, true);
        root.registerOperationHandler(READ_RESOURCE_DESCRIPTION_OPERATION, GlobalOperationHandlers.READ_RESOURCE_DESCRIPTION, CommonProviders.READ_RESOURCE_DESCRIPTION_PROVIDER, true);
        root.registerOperationHandler(READ_CHILDREN_NAMES_OPERATION, GlobalOperationHandlers.READ_CHILDREN_NAMES, CommonProviders.READ_CHILDREN_NAMES_PROVIDER, true);
        root.registerOperationHandler(READ_OPERATION_NAMES_OPERATION, GlobalOperationHandlers.READ_OPERATION_NAMES, CommonProviders.READ_OPERATION_NAMES_PROVIDER, true);
        root.registerOperationHandler(READ_OPERATION_DESCRIPTION_OPERATION, GlobalOperationHandlers.READ_OPERATION_DESCRIPTION, CommonProviders.READ_OPERATION_PROVIDER, true);
        root.registerOperationHandler(WRITE_ATTRIBUTE_OPERATION, GlobalOperationHandlers.WRITE_ATTRIBUTE, CommonProviders.WRITE_ATTRIBUTE_PROVIDER, true);

        // Other root resource operations
        root.registerOperationHandler(NamespaceAddHandler.OPERATION_NAME, NamespaceAddHandler.INSTANCE, NamespaceAddHandler.INSTANCE, false);
        root.registerOperationHandler(NamespaceRemoveHandler.OPERATION_NAME, NamespaceRemoveHandler.INSTANCE, NamespaceRemoveHandler.INSTANCE, false);
        root.registerOperationHandler(SchemaLocationAddHandler.OPERATION_NAME, SchemaLocationAddHandler.INSTANCE, SchemaLocationAddHandler.INSTANCE, false);
        root.registerOperationHandler(SchemaLocationRemoveHandler.OPERATION_NAME, SchemaLocationRemoveHandler.INSTANCE, SchemaLocationRemoveHandler.INSTANCE, false);
        root.registerOperationHandler(SystemPropertyAddHandler.OPERATION_NAME, SystemPropertyAddHandler.INSTANCE, SystemPropertyAddHandler.INSTANCE, false);
        root.registerOperationHandler(SystemPropertyRemoveHandler.OPERATION_NAME, SystemPropertyRemoveHandler.INSTANCE, SystemPropertyRemoveHandler.INSTANCE, false);
        root.registerOperationHandler(ManagementSocketAddHandler.OPERATION_NAME, ManagementSocketAddHandler.INSTANCE, ManagementSocketAddHandler.INSTANCE, false);
        //root.registerOperationHandler(ManagementSocketRemoveHandler.OPERATION_NAME, ManagementSocketRemoveHandler.INSTANCE, ManagementSocketRemoveHandler.INSTANCE, false);
        root.registerOperationHandler(LocalDomainControllerAddHandler.OPERATION_NAME, LocalDomainControllerAddHandler.INSTANCE, LocalDomainControllerAddHandler.INSTANCE, false);
        root.registerOperationHandler(LocalDomainControllerRemoveHandler.OPERATION_NAME, LocalDomainControllerRemoveHandler.INSTANCE, LocalDomainControllerRemoveHandler.INSTANCE, false);
        //root.registerOperationHandler(ReadConfigAsXmlHandler.READ_CONFIG_AS_XML, ReadConfigAsXmlHandler.INSTANCE, ReadConfigAsXmlHandler.INSTANCE, false);

        //TODO register the rest of the root operations

        //interface operations
        ModelNodeRegistration interfaces = root.registerSubModel(PathElement.pathElement(INTERFACE), HostDescriptionProviders.INTERFACE_PROVIDER);
        interfaces.registerOperationHandler(InterfaceAddHandler.OPERATION_NAME, SpecifiedInterfaceAddHandler.INSTANCE, SpecifiedInterfaceAddHandler.INSTANCE, false);
        interfaces.registerOperationHandler(InterfaceRemoveHandler.OPERATION_NAME, SpecifiedInterfaceRemoveHandler.INSTANCE, SpecifiedInterfaceRemoveHandler.INSTANCE, false);

        //server operations
        ModelNodeRegistration servers = root.registerSubModel(PathElement.pathElement(SERVER), HostDescriptionProviders.SERVER_PROVIDER);
        servers.registerOperationHandler(ServerAddHandler.OPERATION_NAME, ServerAddHandler.INSTANCE, ServerAddHandler.INSTANCE, false);
        servers.registerOperationHandler(ServerRemoveHandler.OPERATION_NAME, ServerRemoveHandler.INSTANCE, ServerRemoveHandler.INSTANCE, false);
        servers.registerReadWriteAttribute(START, null, new WriteAttributeHandlers.ModelTypeValidatingHandler(ModelType.BOOLEAN), Storage.CONFIGURATION);
        servers.registerReadWriteAttribute(SOCKET_BINDING_GROUP, null, WriteAttributeHandlers.WriteAttributeOperationHandler.INSTANCE, Storage.CONFIGURATION);
        servers.registerReadWriteAttribute(SOCKET_BINDING_PORT_OFFSET, null, new WriteAttributeHandlers.IntRangeValidatingHandler(1), Storage.CONFIGURATION);

        //TODO register the rest of the server values

    }

    private void createCoreModel() {
        ModelNode root = getModel();
        root.get(NAMESPACES).setEmptyList();
        root.get(SCHEMA_LOCATIONS).setEmptyList();
        root.get(EXTENSION);
        root.get(PATH);
        root.get(SYSTEM_PROPERTY);
        root.get(MANAGEMENT);
        root.get(SERVER);
        root.get(DOMAIN_CONTROLLER);
        root.get(INTERFACE);
        root.get(JVM);
        root.get(DEPLOYMENT);
    }


    public String getName() {
        return getOldHostModel().getName();
    }

    public Map<ServerIdentity, ServerStatus> getServerStatuses() {
        final Map<ServerIdentity, ServerStatus> result = new HashMap<ServerIdentity, ServerStatus>();
        //TODO find other place for hostName
        final String hostName = getOldHostModel().getName();
        for (final Property svr : getHostModel().get(SERVER).asPropertyList()) {
            final ModelNode server = svr.getValue();
            final String serverName = server.require(NAME).asString();
            final ServerIdentity id = new ServerIdentity(hostName, server.require(GROUP).asString(), server.require(NAME).asString());
            final ServerStatus status = determineServerStatus(server);
            result.put(id, status);
        }
        return result;
    }

    private ServerStatus determineServerStatus(final String serverName) {
        return determineServerStatus(getHostModel().get(SERVER, serverName));
    }

    private ServerStatus determineServerStatus(final ModelNode server) {
        ServerStatus status;
        if (!server.isDefined()) {
            status = ServerStatus.DOES_NOT_EXIST;
        }
        else {
            final NewManagedServer client = servers.get(NewManagedServer.getServerProcessName(server.require(NAME).asString()));
            if (client == null) {
                status = server.require(START).asBoolean() ? ServerStatus.STOPPED : ServerStatus.DISABLED;
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

    public ServerModel getServerModel(final String serverName) {
        final NewManagedServer client = servers.get(NewManagedServer.getServerProcessName(serverName));
        if (client == null) {
            log.debugf("Received getServerModel request for unknown server %s", serverName);
            return null;
        }
        return client.getServerModel();
    }

    /**
     * Starts the HostController. This brings this HostController to the point where
     * it has processed its own configuration file, registered with the DomainControllerImpl
     * (including starting one if the host configuration specifies that),
     * obtained the domain configuration, and launched any systems needed to make
     * this process manageable by remote clients.
     */
    public void start() throws IOException {

        try {
            //TODO delete this, just here to help attaching a debugger
            System.out.println("Hardcoded 1s sleep...");
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        modelManager.start();

        List<ModelNode> hostModelUpdates = parseHostXml();
        for (ModelNode update : hostModelUpdates) {
            try {
                System.out.println("update " + update);

                execute(update);
            } catch (OperationFailedException e) {
                throw new RuntimeException(e.getFailureDescription().asString());
            }
        }

        System.out.println("=== Parsed model \n " + getModel());


        // TODO set up logging for this process based on config in Host

        //Start listening for server communication on our socket
        //launchDirectServerCommunicationHandler();

        // Start communication with the ProcessController. This also
        // creates a daemon thread to keep this process alive
        launchProcessControllerSlave();

        final BatchBuilder batchBuilder = serviceContainer.batchBuilder();
        batchBuilder.addListener(new AbstractServiceListener<Object>() {
            @Override
            public void serviceFailed(final ServiceController<?> serviceController, final StartException reason) {
                log.errorf(reason, "Service [%s] failed.", serviceController.getName());
            }
        });

        final ServiceActivatorContext serviceActivatorContext = new ServiceActivatorContextImpl(batchBuilder, serviceContainer);


        // Always activate the management port
        activateManagementCommunication(serviceActivatorContext);
        activateDomainControllerConnection(serviceActivatorContext);

        // Last but not least the host controller service
        final NewHostControllerService hostControllerService = new NewHostControllerService(this);

        batchBuilder.addService(SERVICE_NAME_BASE, hostControllerService)
            .addDependency(NetworkInterfaceService.JBOSS_NETWORK_INTERFACE.append(getHostModel().get(MANAGEMENT, INTERFACE).asString()), NetworkInterfaceBinding.class, hostControllerService.getManagementInterfaceInjector())
            .addInjection(hostControllerService.getManagementPortInjector(), getHostModel().get(MANAGEMENT, PORT).asInt())
            .addDependency(DomainControllerConnection.SERVICE_NAME, DomainControllerConnection.class, hostControllerService.getDomainControllerConnectionInjector())
            .setInitialMode(ServiceController.Mode.ACTIVE)
            .install();

        batchBuilder.install();
    }

    /**
     * The connection from a server to HC was closed
     */
    public void connectionClosed(final String processName) {
        if (stopping.get())
            return;

        final NewManagedServer server = servers.get(processName);

        if (server == null) {
            log.errorf("No server called %s with a closed connection", processName);
            return;
        }

        final ServerState state = server.getState();
        if (state == ServerState.STOPPED || state == ServerState.STOPPING || state == ServerState.MAX_FAILED) {
            log.debugf("Ignoring closed connection for server %s in the %s state", processName, state);
            return;
        }
    }


    public void execute(final ModelNode request, final Queue<ModelNode> responseQueue) {
        // FIXME implemenet execute(ModelNode, Queue<ModelNode)
        throw new UnsupportedOperationException("implement me");
    }

    // FIXME remove: replace with execute(ModelNode, Queue<ModelNode>)
    public List<HostUpdateResult<?>> applyHostUpdates(final List<AbstractHostModelUpdate<?>> updates) {

        final List<HostUpdateApplierResponse> hostResults = getModelManager().applyHostModelUpdates(updates);
        final boolean allowOverallRollback = true; // FIXME make allowOverallRollback configurable
        return applyUpdatesToServers(updates, hostResults, allowOverallRollback);
    }

    public List<UpdateResultHandlerResponse<?>> applyUpdatesToServer(final ServerIdentity server, final List<AbstractServerModelUpdate<?>> updates, final boolean allowOverallRollback) {

        final NewManagedServer client = servers.get(ManagedServer.getServerProcessName(server.getServerName()));
        List<UpdateResultHandlerResponse<?>> responses;
        if (client == null) {
            // TODO better handle disappearance of server
            responses = new ArrayList<UpdateResultHandlerResponse<?>>();
            final UpdateResultHandlerResponse<?> failure = UpdateResultHandlerResponse.createFailureResponse(new IllegalStateException("unknown host " + server.getHostName()));
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
            final boolean allowOverallRollback) {
        final NewManagedServer server = servers.get(ManagedServer.getServerProcessName(serverName));
        if(server == null) {
            log.debugf("Cannot apply updates to unknown server %s", serverName);
            final UpdateResultHandlerResponse<?> urhr = UpdateResultHandlerResponse.createFailureResponse(new UpdateFailedException("No server available with name " + serverName));
            final int size = updates.size();
            final List<UpdateResultHandlerResponse<?>> list = new ArrayList<UpdateResultHandlerResponse<?>>(size);
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
    void availableServer(final String serverName) {
        try {
            final NewManagedServer server = servers.get(serverName);
            if (server == null) {
                log.errorf("No server called %s available", serverName);
                return;
            }
            checkState(server, ServerState.BOOTING);

            server.setState(ServerState.AVAILABLE);
            log.infof("Sending config to server %s", serverName);
            server.startServerProcess();
            server.setState(ServerState.STARTING);
        } catch (final IOException e) {
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

        log.info("Stopping HostController");
        if(domainControllerConnection != null) {
            domainControllerConnection.unregister();
        }
        serviceContainer.shutdown();
        // FIXME stop any local DomainControllerImpl, stop other internal HC services
    }

    /**
     * Callback for when we receive the
     * {@link ServerToHostControllerProtocolCommand#SERVER_STOPPED}
     * message from a Server
     *
     * @param serverName the name of the server
     */
    void stoppedServer(final String serverName) {
        if (stopping.get())
            return;

        final NewManagedServer server = servers.get(serverName);
        if (server == null) {
            log.errorf("No server called %s exists for stop", serverName);
            return;
        }
        checkState(server, ServerState.STOPPING);

        try {
            processControllerClient.stopProcess(serverName);
        } catch (final IOException e) {
            if (stopping.get())
                return;
            log.errorf(e, "Could not stop server %s in PM", serverName);
        }
        try {
            processControllerClient.removeProcess(serverName);
        } catch (final IOException e) {
            if (stopping.get())
                return;
            log.errorf(e, "Could not stop server %s", serverName);
        }
    }

    /**
     * Callback for when we receive the
     * {@link ServerToHostControllerProtocolCommand#SERVER_STARTED}
     * message from a Server
     *
     * @param serverName the name of the server
     */
    void startedServer(final String serverName) {
        final NewManagedServer server = servers.get(serverName);
        if (server == null) {
            log.errorf("No server called %s exists for start", serverName);
            return;
        }
        checkState(server, ServerState.STARTING);
        server.setState(ServerState.STARTED);
    }

    /**
     * Callback for when we receive the
     * {@link ServerToHostControllerProtocolCommand#SERVER_START_FAILED}
     * message from a Server
     *
     * @param serverName the name of the server
     */
    void failedStartServer(final String serverName) {
        final NewManagedServer server = servers.get(serverName);
        if (server == null) {
            log.errorf("No server called %s exists", serverName);
            return;
        }
        checkState(server, ServerState.STARTING);
        server.setState(ServerState.FAILED);
    }

    /**
     * Callback for when we receive the
     * {@link ServerToHostControllerProtocolCommand#SERVER_RECONNECT_STATUS}
     * message from a Server
     *
     * @param serverName the name of the server
     * @param state the server's state
     */
    void reconnectedServer(final String serverName, final ServerState state) {
        final NewManagedServer server = servers.get(serverName);
        if (server == null) {
            log.errorf("No server found for reconnected server %s", serverName);
            return;
        }

        server.setState(state);

        if (state.isRestartOnReconnect()) {
            try {
                server.startServerProcess();
            } catch (final IOException e) {
                log.errorf(e, "Could not start reconnected server %s", server.getServerProcessName());
            }
        }
    }

    /**
     * Handles a notification from the ProcessController that a server has
     * gone down.
     *
     * @param downServerName the process name of the server.
     */
    public void downServer(final String downServerName) {
        final NewManagedServer server = servers.get(downServerName);
        if (server == null) {
            log.errorf("No server called %s exists", downServerName);
            return;
        }

        if (environment.isRestart() && server.getState() == ServerState.BOOTING && environment.getHostControllerPort() == 0) {
            //If this was a restarted HC and a server went down while we were down, process controller will send the DOWN message. If the port
            //is 0, it will be different following a restart so remove and re-add the server with the new port here
            try {
                server.removeServerProcess();
                server.addServerProcess();
            } catch (final IOException e) {
                log.errorf("Error removing and adding process %s", downServerName);
                return;
            }
            try {
                server.startServerProcess();
            } catch (final IOException e) {
                // AutoGenerated
                throw new RuntimeException(e);
            }

        } else {
            server.setState(ServerState.FAILED);
        }
    }

    private void launchProcessControllerSlave() throws IOException {
        final ProtocolClient.Configuration configuration = new ProtocolClient.Configuration();
        configuration.setReadExecutor(Executors.newCachedThreadPool());
        configuration.setServerAddress(new InetSocketAddress(environment.getProcessControllerAddress(), environment.getProcessControllerPort().intValue()));
        configuration.setThreadFactory(Executors.defaultThreadFactory());
        configuration.setSocketFactory(SocketFactory.getDefault());
        processControllerClient = ProcessControllerClient.connect(configuration, authCode, new ProcessMessageHandler() {
            @Override
            public void handleProcessAdded(final ProcessControllerClient client, final String processName) {
            }

            @Override
            public void handleProcessStarted(final ProcessControllerClient client, final String processName) {
            }

            @Override
            public void handleProcessStopped(final ProcessControllerClient client, final String processName, final long uptimeMillis) {
            }

            @Override
            public void handleProcessRemoved(final ProcessControllerClient client, final String processName) {
            }

            @Override
            public void handleProcessInventory(final ProcessControllerClient client, final Map<String, ProcessInfo> inventory) {
                // TODO: reconcile our server list against the process controller inventory
            }

            @Override
            public void handleConnectionShutdown(final ProcessControllerClient client) {
            }

            @Override
            public void handleConnectionFailure(final ProcessControllerClient client, final IOException cause) {
            }

            @Override
            public void handleConnectionFinished(final ProcessControllerClient client) {
            }
        });
        processControllerClient.requestProcessInventory();
    }

    private void activateDomainControllerConnection(final ServiceActivatorContext serviceActivatorContext) {
        if (getHostModel().get(DOMAIN_CONTROLLER, LOCAL).isDefined()) {
            activateLocalDomainController(serviceActivatorContext);
        } else {
            activateRemoteDomainControllerConnection(serviceActivatorContext);
        }
    }


    private void activateLocalDomainController(final ServiceActivatorContext serviceActivatorContext) {
        try {
            final ServiceTarget serviceTarget = serviceActivatorContext.getServiceTarget();

            final XMLMapper mapper = XMLMapper.Factory.create();
            extensionRegistrar.registerStandardDomainReaders(mapper);

            final DomainControllerImpl domainController = new DomainControllerImpl();

            serviceTarget.addService(DomainController.SERVICE_NAME, domainController)
                .addInjection(domainController.getXmlMapperInjector(), mapper)
                .addInjection(domainController.getDomainConfigDirInjector(), environment.getDomainConfigurationDir())
                .addInjection(domainController.getDomainDeploymentsDirInjector(), environment.getDomainDeploymentDir())
                .addDependency(SERVICE_NAME_BASE.append("executor"), ScheduledExecutorService.class, domainController.getScheduledExecutorServiceInjector())
                .install();

            final DomainControllerOperationHandler domainControllerOperationHandler = new DomainControllerOperationHandler();
            serviceTarget.addService(DomainControllerOperationHandler.SERVICE_NAME, domainControllerOperationHandler)
                .addDependency(DomainController.SERVICE_NAME, DomainController.class, domainControllerOperationHandler.getDomainControllerInjector())
                .addDependency(SERVICE_NAME_BASE.append("executor"), ScheduledExecutorService.class, domainControllerOperationHandler.getExecutorServiceInjector())
                .addDependency(SERVICE_NAME_BASE.append("thread-factory"), ThreadFactory.class, domainControllerOperationHandler.getThreadFactoryInjector())
                .addInjection(domainControllerOperationHandler.getLocalFileRepositoryInjector(), fileRepository)
                .addDependency(ManagementCommunicationService.SERVICE_NAME, ManagementCommunicationService.class, new ManagementCommunicationServiceInjector(domainControllerOperationHandler))
                .install();

            final DomainControllerClientOperationHandler domainControllerClientOperationHandler = new DomainControllerClientOperationHandler();
            serviceTarget.addService(DomainControllerClientOperationHandler.SERVICE_NAME, domainControllerClientOperationHandler)
                .addDependency(DomainController.SERVICE_NAME, DomainController.class, domainControllerClientOperationHandler.getDomainControllerInjector())
                .addDependency(ManagementCommunicationService.SERVICE_NAME, ManagementCommunicationService.class, new ManagementCommunicationServiceInjector(domainControllerClientOperationHandler))
                .install();

            serviceTarget.addService(DomainControllerConnection.SERVICE_NAME, new NewLocalDomainControllerConnection(NewHostController.this, domainController, fileRepository))
                .addDependency(DomainController.SERVICE_NAME)
                .install();

        } catch (final Exception e) {
            throw new RuntimeException("Exception starting local domain controller", e);
        }
    }

    private void activateRemoteDomainControllerConnection(final ServiceActivatorContext serviceActivatorContext) {

        if (true) {
            throw new RuntimeException("Not ported to detyped yet");
        }

        final ServiceTarget serviceTarget = serviceActivatorContext.getServiceTarget();

        final NewDomainControllerConnectionService domainControllerClientService = new NewDomainControllerConnectionService(this, fileRepository, 10L);

        final HostModel hostConfig = getOldHostModel();
        final RemoteDomainControllerElement remoteDomainControllerElement = hostConfig.getRemoteDomainControllerElement();
        final InetAddress hostAddress;
        try {
            hostAddress = InetAddress.getByName(remoteDomainControllerElement.getHost());
        } catch (final UnknownHostException e) {
            throw new RuntimeException("Failed to get remote domain controller address", e);
        }
        final ManagementElement managementElement = hostConfig.getManagementElement();

        serviceTarget.addService(DomainControllerConnectionService.SERVICE_NAME, domainControllerClientService)
            .addListener(new AbstractServiceListener<DomainControllerConnection>() {
                @Override
                public void serviceFailed(final ServiceController<? extends DomainControllerConnection> serviceController, final StartException reason) {
                    log.error("Failed to register with domain controller.", reason);
                }
            })
            .addInjection(domainControllerClientService.getDomainControllerAddressInjector(), hostAddress)
            .addInjection(domainControllerClientService.getDomainControllerPortInjector(), remoteDomainControllerElement.getPort())
            .addDependency(NetworkInterfaceService.JBOSS_NETWORK_INTERFACE.append(managementElement.getInterfaceName()), NetworkInterfaceBinding.class, domainControllerClientService.getLocalManagementInterfaceInjector())
            .addInjection(domainControllerClientService.getLocalManagementPortInjector(), managementElement.getPort())
            .addDependency(SERVICE_NAME_BASE.append("executor"), ScheduledExecutorService.class, domainControllerClientService.getExecutorServiceInjector())
            .addDependency(SERVICE_NAME_BASE.append("thread-factory"), ThreadFactory.class, domainControllerClientService.getThreadFactoryInjector())
        .addAliases(DomainControllerConnection.SERVICE_NAME)
        .setInitialMode(ServiceController.Mode.ACTIVE)
        .install();
    }

    private void activateManagementCommunication(final ServiceActivatorContext serviceActivatorContext) {
        final ServiceTarget serviceTarget = serviceActivatorContext.getServiceTarget();
        final ModelNode managementResource = getHostModel().get(MANAGEMENT);
        final String managementInterface = managementResource.require(INTERFACE).asString();
        final int managementPort = managementResource.require(PORT).asInt();

        // Add the executor
        final ServiceName threadFactoryServiceName = SERVICE_NAME_BASE.append("thread-factory");
            serviceTarget.addService(threadFactoryServiceName, new ThreadFactoryService())
                .install();

        final ServiceName executorServiceName = SERVICE_NAME_BASE.append("executor");

        /**
         * Replace below with fixed ScheduledThreadPoolService
         */
        final InjectedValue<ThreadFactory> threadFactoryValue = new InjectedValue<ThreadFactory>();
        serviceTarget.addService(executorServiceName, new Service<ScheduledExecutorService>() {
            private ScheduledExecutorService executorService;
            @Override
            public synchronized void start(final StartContext context) throws StartException {
                executorService = Executors.newScheduledThreadPool(20, threadFactoryValue.getValue());
            }

            @Override
            public synchronized void stop(final StopContext context) {
                executorService.shutdown();
            }

            @Override
            public synchronized ScheduledExecutorService getValue() throws IllegalStateException {
                return executorService;
            }
        }).addDependency(threadFactoryServiceName, ThreadFactory.class, threadFactoryValue)
        .install();

        //  Add the management communication service
        final ManagementCommunicationService managementCommunicationService = new ManagementCommunicationService();
        serviceTarget.addService(ManagementCommunicationService.SERVICE_NAME, managementCommunicationService)
//            .addDependency(NetworkInterfaceService.JBOSS_NETWORK_INTERFACE.append(managementElement.getInterfaceName()), NetworkInterfaceBinding.class, managementCommunicationService.getInterfaceInjector())
//            .addInjection(managementCommunicationService.getPortInjector(), managementElement.getPort())
            .addDependency(NetworkInterfaceService.JBOSS_NETWORK_INTERFACE.append(managementInterface), NetworkInterfaceBinding.class, managementCommunicationService.getInterfaceInjector())
            .addInjection(managementCommunicationService.getPortInjector(), managementPort)
            .addDependency(executorServiceName, ExecutorService.class, managementCommunicationService.getExecutorServiceInjector())
            .addDependency(threadFactoryServiceName, ThreadFactory.class, managementCommunicationService.getThreadFactoryInjector())
            .setInitialMode(ServiceController.Mode.ACTIVE)
            .install();

        //  Add the DC to host controller operation handler
        final ManagementOperationHandlerService<NewHostControllerOperationHandler> operationHandlerService
                = new ManagementOperationHandlerService<NewHostControllerOperationHandler>(new NewHostControllerOperationHandler(this));
        serviceTarget.addService(ManagementCommunicationService.SERVICE_NAME.append("host", "controller"), operationHandlerService)
            .addDependency(ManagementCommunicationService.SERVICE_NAME, ManagementCommunicationService.class, new ManagementCommunicationServiceInjector(operationHandlerService))
            .install();

        //  Add the server to host controller operation handler
        final ManagementOperationHandlerService<NewServerToHostControllerOperationHandler> serverOperationHandlerService
                = new ManagementOperationHandlerService<NewServerToHostControllerOperationHandler>(new NewServerToHostControllerOperationHandler(this));
        serviceTarget.addService(ManagementCommunicationService.SERVICE_NAME.append("server", "to", "host", "controller"), serverOperationHandlerService)
            .addDependency(ManagementCommunicationService.SERVICE_NAME, ManagementCommunicationService.class,  new ManagementCommunicationServiceInjector(serverOperationHandlerService))
            .install();
    }

    void setDomainControllerConnection(final DomainControllerConnection domainControllerConnection) {
        this.domainControllerConnection = domainControllerConnection;

        // By having a remote repo as a secondary content will be synced only if needed
        final FallbackRepository repository = new FallbackRepository(fileRepository, domainControllerConnection.getRemoteFileRepository());
        modelManager.setFileRepository(repository);
        this.remoteBackedRepository = repository;
    }

    void setManagementSocketAddress(final InetSocketAddress managementSocketAddress) {
        this.managementSocketAddress = managementSocketAddress;
    }

    public ModelManager getModelManager() {
        return modelManager;
    }

    protected DomainModel getDomainModel() {
        return modelManager.getDomainModel();
    }

    @Deprecated
    protected HostModel getOldHostModel() {
        return modelManager.getHostModel();
    }

    protected ModelNode getHostModel() {
        return super.getModel();
    }

    /**
     * Set the domain for the host controller.  If this is the first time the domain has been set on this instance it will
     * also invoke the server launch process.
     *
     * @param domain The domain configuration
     */
    public void setDomain(final DomainModel domain) {
        modelManager.setDomainModel(domain);
    }

    public NewManagedServer getServer(final String name) {
        return servers.get(name);
    }

    private void checkState(final NewManagedServer server, final ServerState expected) {
        final ServerState state = server.getState();
        if (state != expected) {
            log.warnf("Server %s is not in the expected %s state: %s" , server.getServerProcessName(), expected, state);
        }
    }

    public Map<String, NewManagedServer> getServers() {
        synchronized (servers) {
            return Collections.unmodifiableMap(servers);
        }
    }

    private void synchronizeDeployments() {
        final DomainModel domainConfig = getDomainModel();
        final Set<String> serverGroupNames = domainConfig.getServerGroupNames();
        for (final Property svr : getHostModel().get(SERVER).asPropertyList()) {
            final String serverGroupName = svr.getValue().require(GROUP).asString();
            if (serverGroupNames.remove(serverGroupName)) {
                for (final ServerGroupDeploymentElement deployment : domainConfig.getServerGroup(serverGroupName).getDeployments()) {
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
                for (final Property svr : getHostModel().get(SERVER).asPropertyList()) {
                    final ModelNode server = svr.getValue();
                    final String serverName = server.require(NAME).asString();
                    // TODO take command line input on what servers to start
                    if (server.get(START).asBoolean()) {
                        log.info("Starting server " + serverName);
                        try {
                            startServer(serverName, managementSocketAddress);
                        } catch (final IOException e) {
                            // FIXME handle failure to start server
                            log.error("Failed to start server " + serverName, e);
                        }
                    }
                    else log.info("Server " + serverName + " is configured to not be started");
                }
            } else {
                // FIXME -- this got dropped in the move to an update-based boot
                // handle it properly
//                reconnectServers();
            }
        }
    }

    public ServerStatus startServer(final String serverName) {
        try {
            final String processName = NewManagedServer.getServerProcessName(serverName);
            final NewManagedServer server = servers.get(processName);
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
        catch (final Exception e) {
            log.errorf(e, "Failed to start server %s", serverName);
        }

        ServerStatus status = determineServerStatus(serverName);
        // FIXME total hack; set up some sort of notification scheme
        for (int i = 0; i < 50; i++) {
            status = determineServerStatus(serverName);
            if (status == ServerStatus.STARTING) {
                try {
                    Thread.sleep(100);
                } catch (final InterruptedException e) {
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

    public ServerStatus stopServer(final String serverName, final long gracefulTimeout) {
        try {
            final String processName = ManagedServer.getServerProcessName(serverName);
            final NewManagedServer server = servers.get(processName);
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
        catch (final Exception e) {
            log.errorf(e, "Failed to stop server %s", serverName);
        }

        return determineServerStatus(serverName);
    }

    public ServerStatus restartServer(final String serverName, final long gracefulTimeout) {
        stopServer(serverName, gracefulTimeout);
        return startServer(serverName);
    }

    private void startServer(final String serverName, final InetSocketAddress managementSocket) throws IOException {
        final NewManagedServer server = new NewManagedServer(serverName, getDomainModel(), getOldHostModel(), null, getHostModel(), environment, processControllerClient, managementSocket);
        servers.put(server.getServerProcessName(), server);
        server.addServerProcess();
        server.startServerProcess();
    }

    private List<HostUpdateResult<?>> applyUpdatesToServers(final List<AbstractHostModelUpdate<?>> updates,
                                                              final List<HostUpdateApplierResponse> hostResults,
                                                              final boolean allowOverallRollback) {
        List<HostUpdateResult<?>> result;
        final Map<AbstractHostModelUpdate<?>, AbstractServerModelUpdate<?>> serverByHost = new HashMap<AbstractHostModelUpdate<?>, AbstractServerModelUpdate<?>>();
        final Map<AbstractServerModelUpdate<?>, HostUpdateResult<Object>> resultsByUpdate = new HashMap<AbstractServerModelUpdate<?>, HostUpdateResult<Object>>();
        for (int i = 0; i < updates.size(); i++) {
            final AbstractHostModelUpdate<?> hostUpdate = updates.get(i);
            final AbstractServerModelUpdate<?> serverUpdate = hostUpdate.getServerModelUpdate();
            if (serverUpdate != null) {
                serverByHost.put(hostUpdate, serverUpdate);
                resultsByUpdate.put(serverUpdate, new HostUpdateResult<Object>());
            }
        }
        final Map<ServerIdentity, List<AbstractServerModelUpdate<?>>> updatesByServer =
                getUpdatesByServer(updates, hostResults, serverByHost);

        // TODO Add param to configure pushing out concurrently
        for (final Map.Entry<ServerIdentity, List<AbstractServerModelUpdate<?>>> entry : updatesByServer.entrySet()) {
            final ServerIdentity server = entry.getKey();
            final List<AbstractServerModelUpdate<?>> serverUpdates = entry.getValue();
            // Push them out
            final List<UpdateResultHandlerResponse<?>> rsps = applyUpdatesToServer(server, serverUpdates,
                    allowOverallRollback);
            for (int i = 0; i < serverUpdates.size(); i++) {
                final UpdateResultHandlerResponse<?> rsp = rsps.get(i);
                final AbstractServerModelUpdate<?> serverUpdate = entry.getValue().get(i);
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
        for (final AbstractHostModelUpdate<?> hostUpdate : updates) {
            final AbstractServerModelUpdate<?> serverUpdate = serverByHost.get(hostUpdate);
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

        final Map<ServerIdentity, List<AbstractServerModelUpdate<?>>> result = new HashMap<ServerIdentity, List<AbstractServerModelUpdate<?>>>();

        for (int i = 0; i < hostResults.size(); i++) {
            final HostUpdateApplierResponse domainResult = hostResults.get(i);
            final AbstractHostModelUpdate<?> domainUpdate = hostUpdates.get(i);
            final AbstractServerModelUpdate<?> serverUpdate = serverByHost.get(domainUpdate);
            for (final ServerIdentity server : domainResult.getServers()) {
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



    private List<ModelNode> parseHostXml(){
        try {
            return configurationPersister.load();
        } catch (ConfigurationPersistenceException e) {
            throw new RuntimeException(e);
        }
    }

    private List<ModelNode> parseDomainXml(){
        try {
            //TODO store this or pass it in from somewhere
            NewConfigurationPersister persister = NewConfigurationPersisterFactory.createDomainXmlConfigurationPersister(environment.getDomainConfigurationDir());
            return persister.load();
        } catch (ConfigurationPersistenceException e) {
            throw new RuntimeException(e);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected NewOperationContext getOperationContext(final ModelNode subModel, final ModelNode operation, final OperationHandler operationHandler) {
        return new NewHostOperationContextImpl(this, getRegistry(), subModel);
    }

    final class NewHostOperationContextImpl extends NewOperationContextImpl implements NewHostOperationContext {

        public NewHostOperationContextImpl(ModelController controller, ModelNodeRegistration registry, ModelNode subModel) {
            super(controller, registry, subModel);
        }

        @Override
        public NewHostController getController() {
            return (NewHostController) super.getController();
        }

        @Override
        public ServiceTarget getServiceTarget() {
            // TODO: A tracking service listener which will somehow call complete when the operation is done
            return serviceContainer;
        }

        @Override
        public ServiceRegistry getServiceRegistry() {
            return serviceContainer;
        }
    }
}

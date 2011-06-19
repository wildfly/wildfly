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

import java.security.AccessController;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

import org.jboss.as.controller.AbstractControllerService;
import org.jboss.as.controller.BootContext;
import org.jboss.as.controller.ControlledProcessState;
import org.jboss.as.controller.NewModelController;
import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.NewProxyController;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProxyOperationAddressTranslator;
import org.jboss.as.controller.client.helpers.domain.ServerStatus;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.persistence.ConfigurationPersistenceException;
import org.jboss.as.controller.persistence.ConfigurationPersister;
import org.jboss.as.controller.registry.ModelNodeRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.remote.NewModelControllerClientOperationHandlerFactoryService;
import org.jboss.as.controller.remote.NewRemoteProxyController;
import org.jboss.as.domain.controller.FileRepository;
import org.jboss.as.domain.controller.LocalHostControllerInfo;
import org.jboss.as.domain.controller.NewDomainController;
import org.jboss.as.domain.controller.NewDomainModelUtil;
import org.jboss.as.domain.controller.NewMasterDomainControllerClient;
import org.jboss.as.domain.controller.UnregisteredHostChannelRegistry;
import org.jboss.as.domain.controller.descriptions.DomainDescriptionProviders;
import org.jboss.as.domain.controller.operations.coordination.PrepareStepHandler;
import org.jboss.as.domain.management.security.SecurityRealmService;
import org.jboss.as.host.controller.NewRemoteDomainConnectionService.RemoteFileRepository;
import org.jboss.as.host.controller.mgmt.NewMasterDomainControllerOperationHandlerService;
import org.jboss.as.host.controller.mgmt.ServerToHostOperationHandlerFactoryService;
import org.jboss.as.host.controller.operations.LocalHostControllerInfoImpl;
import org.jboss.as.host.controller.operations.NewStartServersHandler;
import org.jboss.as.network.NetworkInterfaceBinding;
import org.jboss.as.process.ProcessInfo;
import org.jboss.as.protocol.mgmt.ManagementChannel;
import org.jboss.as.remoting.RemotingServices;
import org.jboss.as.server.mgmt.HttpManagementService;
import org.jboss.as.server.services.net.NetworkInterfaceService;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
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
import org.jboss.threads.JBossThreadFactory;

/**
 * Creates the service that acts as the {@link org.jboss.as.controller.NewModelController} for a host.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class DomainModelControllerService extends AbstractControllerService implements NewDomainController, UnregisteredHostChannelRegistry {

    public static final ServiceName SERVICE_NAME = NewHostControllerBootstrap.SERVICE_NAME_BASE.append("model", "controller");

    private final HostControllerConfigurationPersister configurationPersister;
    private final HostControllerEnvironment environment;
    private final LocalHostControllerInfoImpl hostControllerInfo;
    private final FileRepository localFileRepository;
    private final RemoteFileRepository remoteFileRepository;
    private final InjectedValue<ExecutorService> injectedExecutorService = new InjectedValue<ExecutorService>();
    private final Map<String, NewProxyController> hostProxies;
    private final Map<String, NewProxyController> serverProxies;
    private final PrepareStepHandler prepareStepHandler;
    private ModelNodeRegistration modelNodeRegistration;

    private final Map<String, ManagementChannel> unregisteredHostChannels = new HashMap<String, ManagementChannel>();
    private final Map<String, ProxyCreatedCallback> proxyCreatedCallbacks = new HashMap<String, ProxyCreatedCallback>();
    private final ExecutorService proxyExecutor = Executors.newCachedThreadPool();

    private volatile NewServerInventory serverInventory;

    public static ServiceController<NewModelController> addService(final ServiceTarget serviceTarget,
                                                            final HostControllerEnvironment environment,
                                                            final ControlledProcessState processState) {
        final Map<String, NewProxyController> hostProxies = new ConcurrentHashMap<String, NewProxyController>();
        final Map<String, NewProxyController> serverProxies = new ConcurrentHashMap<String, NewProxyController>();
        final LocalHostControllerInfoImpl hostControllerInfo = new LocalHostControllerInfoImpl(processState);
        final PrepareStepHandler prepareStepHandler = new PrepareStepHandler(hostControllerInfo, hostProxies, serverProxies);
        DomainModelControllerService service = new DomainModelControllerService(environment, processState,
                hostControllerInfo, new HostControllerConfigurationPersister(environment, hostControllerInfo),
                hostProxies, serverProxies, prepareStepHandler);
        return serviceTarget.addService(SERVICE_NAME, service)
                .addDependency(NewHostControllerBootstrap.SERVICE_NAME_BASE.append("executor"), ExecutorService.class, service.injectedExecutorService)
                .setInitialMode(ServiceController.Mode.ACTIVE)
                .install();
    }

    private DomainModelControllerService(final HostControllerEnvironment environment,
                                         final ControlledProcessState processState,
                                         final LocalHostControllerInfoImpl hostControllerInfo,
                                         final HostControllerConfigurationPersister configurationPersister,
                                         final Map<String, NewProxyController> hostProxies,
                                         final Map<String, NewProxyController> serverProxies,
                                         final PrepareStepHandler prepareStepHandler) {
        super(NewOperationContext.Type.HOST, configurationPersister, processState, DomainDescriptionProviders.ROOT_PROVIDER,
                prepareStepHandler);
        this.configurationPersister = configurationPersister;
        this.environment = environment;
        this.hostControllerInfo = hostControllerInfo;
        this.localFileRepository = new LocalFileRepository(environment);
        this.remoteFileRepository = new RemoteFileRepository(localFileRepository);
        this.hostProxies = hostProxies;
        this.serverProxies = serverProxies;
        this.prepareStepHandler = prepareStepHandler;
    }

    @Override
    public LocalHostControllerInfo getLocalHostInfo() {
        return hostControllerInfo;
    }

    @Override
    public void registerRemoteHost(NewProxyController hostControllerClient) {
        if (!hostControllerInfo.isMasterDomainController()) {
            throw new UnsupportedOperationException("Registration of remote hosts is not supported on slave host controllers");
        }
        PathAddress pa = hostControllerClient.getProxyNodeAddress();
        PathElement pe = pa.getElement(0);
        Logger.getLogger("org.jboss.domain").info("Registering host " + pe.getValue());
        if (modelNodeRegistration.getProxyController(pa) != null || hostControllerInfo.getLocalHostName().equals(pe.getValue())){
            throw new IllegalArgumentException("There is already a registered host named '" + pe.getValue() + "'");
        }
        modelNodeRegistration.registerProxyController(pe, hostControllerClient);
        hostProxies.put(pe.getValue(), hostControllerClient);


    }

    @Override
    public void unregisterRemoteHost(String id) {
        Logger.getLogger("org.jboss.domain").info("Unregistering host " + id);
        hostProxies.remove(id);
        modelNodeRegistration.unregisterProxyController(PathElement.pathElement(HOST, id));
    }

    @Override
    public void registerRunningServer(NewProxyController serverControllerClient) {
        PathAddress pa = serverControllerClient.getProxyNodeAddress();
        PathElement pe = pa.getElement(1);
        if (modelNodeRegistration.getProxyController(pa) != null) {
            throw new IllegalArgumentException("There is already a registered server named '" + pe.getValue() + "'");
        }
        Logger.getLogger("org.jboss.host.controller").info("Registering server " + pe.getValue());
        ModelNodeRegistration hostRegistration = modelNodeRegistration.getSubModel(PathAddress.pathAddress(PathElement.pathElement(HOST)));
        hostRegistration.registerProxyController(pe, serverControllerClient);
        serverProxies.put(pe.getValue(), serverControllerClient);
    }

    @Override
    public void unregisterRunningServer(String serverName) {
        PathAddress pa = PathAddress.pathAddress(PathElement.pathElement(HOST, hostControllerInfo.getLocalHostName()));
        PathElement pe = PathElement.pathElement(RUNNING_SERVER, serverName);
        Logger.getLogger("org.jboss.host.controller").info("Unregistering server " + serverName);
        ModelNodeRegistration hostRegistration = modelNodeRegistration.getSubModel(pa);
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
            String msg = msgNode.isDefined() ? msgNode.toString() : "Failed to retrieve profile operations from domain controller";
            throw new RuntimeException(msg);
        }
        return rsp.require(RESULT);
    }

    @Override
    public FileRepository getFileRepository() {
        return localFileRepository;
    }

    @Override
    protected void initModel(Resource rootResource, ModelNodeRegistration rootRegistration) {
        NewDomainModelUtil.updateCoreModel(rootResource.getModel());
        NewHostModelUtil.createHostRegistry(rootRegistration, configurationPersister, environment, localFileRepository,
                hostControllerInfo, new DelegatingServerInventory(), remoteFileRepository, this, this);
        this.modelNodeRegistration = rootRegistration;
    }

    @Override
    public void start(StartContext context) throws StartException {
        prepareStepHandler.setExecutorService(injectedExecutorService.getValue());
        super.start(context);
    }

    // See superclass start. This method is invoked from a separate non-MSC thread after start. So we can do a fair
    // bit of stuff
    @Override
    protected void boot(final BootContext context) throws ConfigurationPersistenceException {
        final ServiceTarget serviceTarget = context.getServiceTarget();
        try {
            super.boot(configurationPersister.load()); // This parses the host.xml and invokes all ops

            //Install the server inventory
            NetworkInterfaceBinding nativeManagementInterfaceBinding = getNativeManagementNetworkInterfaceBinding();
            Future<NewServerInventory> inventoryFuture = NewServerInventoryService.install(serviceTarget, this, environment,
                    nativeManagementInterfaceBinding, hostControllerInfo.getNativeManagementPort());

            //Install the core remoting endpoint and listener
            RemotingServices.installRemotingEndpoint(serviceTarget);

            if (!hostControllerInfo.isMasterDomainController()) {
                serverInventory = getFuture(inventoryFuture);

                Future<NewMasterDomainControllerClient> clientFuture = NewRemoteDomainConnectionService.install(serviceTarget,
                        getValue(),
                        hostControllerInfo.getLocalHostName(),
                        hostControllerInfo.getRemoteDomainControllerHost(),
                        hostControllerInfo.getRemoteDomainControllertPort(),
                        remoteFileRepository);
                NewMasterDomainControllerClient masterDomainControllerClient = getFuture(clientFuture);
                //Registers us with the master and gets down the master copy of the domain model to our DC
                //TODO make sure that the RDCS checks env.isUseCachedDC, and if true falls through to that
                masterDomainControllerClient.register();

            } else {
                // parse the domain.xml and load the steps
                ConfigurationPersister domainPersister = configurationPersister.getDomainPersister();
                super.boot(domainPersister.load());
                RemotingServices.installChannelServices(serviceTarget, new NewMasterDomainControllerOperationHandlerService(this, this),
                        DomainModelControllerService.SERVICE_NAME, RemotingServices.DOMAIN_CHANNEL, null, null);
                serverInventory = getFuture(inventoryFuture);
            }

            // TODO look into adding some of these services in the handlers, but ON-DEMAND. Then here just add some
            // simple service that demands them

            RemotingServices.installDomainConnectorServices(serviceTarget, nativeManagementInterfaceBinding, hostControllerInfo.getNativeManagementPort());
            ServerToHostOperationHandlerFactoryService.install(serviceTarget, NewServerInventoryService.SERVICE_NAME);
            RemotingServices.installChannelOpenListenerService(serviceTarget, RemotingServices.SERVER_CHANNEL,
                    ServerToHostOperationHandlerFactoryService.SERVICE_NAME, null, null);
            RemotingServices.installChannelServices(serviceTarget, new NewModelControllerClientOperationHandlerFactoryService(),
                    DomainModelControllerService.SERVICE_NAME, RemotingServices.MANAGEMENT_CHANNEL, null, null);

            if (hostControllerInfo.getHttpManagementInterface() != null) {
                installHttpManagementServices(serviceTarget);
            }

            startServers();

            // TODO when to call configurationPersister.successful boot? Look into this for standalone as well; may be broken now
        } finally {
            finishBoot();
        }
    }

    private void installHttpManagementServices(final ServiceTarget serviceTarget) {

        String interfaceName = hostControllerInfo.getHttpManagementInterface();
        int port = hostControllerInfo.getHttpManagementPort();
        int securePort = hostControllerInfo.getHttpManagementSecurePort();
        String securityRealm = hostControllerInfo.getHttpManagementSecurityRealm();

        Logger.getLogger("org.jboss.as").infof("creating http management service using network interface (%s) port (%s) securePort (%s)", interfaceName, port, securePort);

        final ThreadFactory httpMgmtThreads = new JBossThreadFactory(new ThreadGroup("HttpManagementService-threads"),
                Boolean.FALSE, null, "%G - %t", null, null, AccessController.getContext());
        final HttpManagementService service = new HttpManagementService();
        ServiceBuilder builder = serviceTarget.addService(HttpManagementService.SERVICE_NAME, service)
                .addDependency(
                        NetworkInterfaceService.JBOSS_NETWORK_INTERFACE.append(interfaceName),
                        NetworkInterfaceBinding.class, service.getInterfaceInjector())
                .addDependency(SERVICE_NAME, NewModelController.class, service.getModelControllerInjector())
                .addInjection(service.getTempDirInjector(), environment.getDomainTempDir().getAbsolutePath())
                .addInjection(service.getPortInjector(), port)
                .addInjection(service.getSecurePortInjector(), securePort)
                .addInjection(service.getExecutorServiceInjector(), Executors.newCachedThreadPool(httpMgmtThreads));

        if (securityRealm != null) {
            builder.addDependency(SecurityRealmService.BASE_SERVICE_NAME.append(securityRealm), SecurityRealmService.class, service.getSecurityRealmInjector());
        }
        builder.setInitialMode(ServiceController.Mode.ACTIVE)
                .install();

    }

    private NetworkInterfaceBinding getNativeManagementNetworkInterfaceBinding() {
        try {
            return hostControllerInfo.getNetworkInterfaceBinding(hostControllerInfo.getNativeManagementInterface());
        } catch (RuntimeException e) {
            // TODO this is a critical failure; we need to handle it more cleanly
            throw e;
        } catch (Exception e) {
            // TODO this is a critical failure; we need to handle it more cleanly
            throw new RuntimeException(e);
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
        ModelNode op = Util.getEmptyOperation(NewStartServersHandler.OPERATION_NAME, addr);

        getValue().execute(op, null, null, null);
    }


    @Override
    public void stop(StopContext context) {
        serverInventory = null;
        super.stop(context);
    }

    @Override
    public synchronized void registerChannel(final String hostName, final ManagementChannel channel, final ProxyCreatedCallback callback) {
        PathAddress addr = PathAddress.pathAddress(PathElement.pathElement(HOST, hostName));
        if (modelNodeRegistration.getProxyController(addr) != null) {
            throw new IllegalArgumentException("There is already a registered server named '" + hostName + "'");
        }
        if (unregisteredHostChannels.containsKey(hostName)) {
            throw new IllegalArgumentException("Already have a connection for host " + hostName);
        }
        unregisteredHostChannels.put(hostName, channel);
        proxyCreatedCallbacks.put(hostName, callback);
        channel.addCloseHandler(new CloseHandler<Channel>() {
            public void handleClose(Channel closed) {
                unregisteredHostChannels.remove(hostName);
                proxyCreatedCallbacks.remove(hostName);
            }
        });
    }

    @Override
    public synchronized NewProxyController popChannelAndCreateProxy(String hostName) {
        ManagementChannel channel = unregisteredHostChannels.remove(hostName);
        if (channel == null) {
            throw new IllegalArgumentException("No channel for host " + hostName);
        }
        final PathAddress addr = PathAddress.pathAddress(PathElement.pathElement(ModelDescriptionConstants.HOST, hostName));
        NewRemoteProxyController proxy = NewRemoteProxyController.create(proxyExecutor, addr, ProxyOperationAddressTranslator.HOST, channel);
        ProxyCreatedCallback callback = proxyCreatedCallbacks.remove(hostName);
        if (callback != null) {
            callback.proxyCreated(proxy);
        }
        return proxy;
    }

    private class DelegatingServerInventory implements NewServerInventory {
        public void serverRegistered(String serverProcessName, ManagementChannel channel, ProxyCreatedCallback callback) {
            serverInventory.serverRegistered(serverProcessName, channel, callback);
        }

        public void serverStartFailed(String serverProcessName) {
            serverInventory.serverStartFailed(serverProcessName);
        }

        public void serverStopped(String serverProcessName) {
            serverInventory.serverStopped(serverProcessName);
        }

        public String getServerProcessName(String serverName) {
            return serverInventory.getServerProcessName(serverName);
        }

        public void processInventory(Map<String, ProcessInfo> processInfos) {
            serverInventory.processInventory(processInfos);
        }

        public Map<String, ProcessInfo> determineRunningProcesses() {
            return serverInventory.determineRunningProcesses();
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
    }
}

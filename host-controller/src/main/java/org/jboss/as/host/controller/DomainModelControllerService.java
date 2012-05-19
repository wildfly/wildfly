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

import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.ProxyOperationAddressTranslator;
import org.jboss.as.controller.client.OperationAttachments;
import org.jboss.as.controller.client.OperationMessageHandler;
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

import java.io.File;
import java.io.IOException;
import java.security.AccessController;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
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
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.client.helpers.domain.ServerStatus;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.persistence.ConfigurationPersistenceException;
import org.jboss.as.controller.persistence.ConfigurationPersister;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.services.path.PathManagerService;
import org.jboss.as.controller.transform.Transformers;
import org.jboss.as.domain.controller.DomainController;
import org.jboss.as.domain.controller.DomainModelUtil;
import org.jboss.as.domain.controller.LocalHostControllerInfo;
import org.jboss.as.domain.controller.SlaveRegistrationException;
import org.jboss.as.domain.controller.descriptions.DomainDescriptionProviders;
import org.jboss.as.domain.controller.operations.coordination.PrepareStepHandler;
import org.jboss.as.host.controller.RemoteDomainConnectionService.RemoteFileRepository;
import org.jboss.as.host.controller.ignored.IgnoredDomainResourceRegistry;
import org.jboss.as.host.controller.mgmt.HostControllerRegistrationHandler;
import org.jboss.as.host.controller.mgmt.MasterDomainControllerOperationHandlerService;
import org.jboss.as.host.controller.mgmt.ServerToHostOperationHandlerFactoryService;
import org.jboss.as.host.controller.mgmt.SlaveHostPinger;
import org.jboss.as.host.controller.mgmt.TransformingProxyController;
import org.jboss.as.host.controller.operations.LocalHostControllerInfoImpl;
import org.jboss.as.host.controller.operations.StartServersHandler;
import org.jboss.as.process.CommandLineConstants;
import org.jboss.as.process.ExitCodes;
import org.jboss.as.process.ProcessControllerClient;
import org.jboss.as.process.ProcessInfo;
import org.jboss.as.process.ProcessMessageHandler;
import org.jboss.as.protocol.mgmt.ManagementChannelHandler;
import org.jboss.as.remoting.management.ManagementRemotingServices;
import org.jboss.as.repository.ContentRepository;
import org.jboss.as.repository.HostFileRepository;
import org.jboss.as.repository.LocalFileRepository;
import org.jboss.as.server.BootstrapListener;
import org.jboss.as.server.RuntimeExpressionResolver;
import org.jboss.as.server.mgmt.HttpManagementService;
import org.jboss.as.server.services.security.AbstractVaultReader;
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
import org.jboss.threads.JBossThreadFactory;

/**
 * Creates the service that acts as the {@link org.jboss.as.controller.ModelController} for a Host Controller process.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class DomainModelControllerService extends AbstractControllerService implements DomainController, HostModelUtil.HostModelRegistrar {

    public static final ServiceName SERVICE_NAME = HostControllerService.HC_SERVICE_NAME.append("model", "controller");

    private static final int PINGER_POOL_SIZE;
    static {
        int poolSize = -1;
        try {
            poolSize = Integer.parseInt(SecurityActions.getSystemProperty("jboss.as.domain.ping.pool.size", "5"));
        } catch (Exception e) {
            // TODO log
        } finally {
            PINGER_POOL_SIZE = poolSize > 0 ? poolSize : 5;
        }
    }

    private volatile HostControllerConfigurationPersister hostControllerConfigurationPersister;
    private final HostControllerEnvironment environment;
    private final HostRunningModeControl runningModeControl;
    private final LocalHostControllerInfoImpl hostControllerInfo;
    private final HostFileRepository localFileRepository;
    private final RemoteFileRepository remoteFileRepository;
    private final InjectedValue<ProcessControllerConnectionService> injectedProcessControllerConnection = new InjectedValue<ProcessControllerConnectionService>();
    private final ConcurrentMap<String, ProxyController> hostProxies;
    private final ConcurrentMap<String, HostRegistration> hostRegistrationMap = new ConcurrentHashMap<String, HostRegistration>();
    private final Map<String, ProxyController> serverProxies;
    private final PrepareStepHandler prepareStepHandler;
    private final BootstrapListener bootstrapListener;
    private ManagementResourceRegistration modelNodeRegistration;
    private final AbstractVaultReader vaultReader;
    private final ContentRepository contentRepository;
    private final ExtensionRegistry extensionRegistry;
    private final ControlledProcessState processState;
    private final IgnoredDomainResourceRegistry ignoredRegistry;
    private final PathManagerService pathManager;

    private volatile ServerInventory serverInventory;

    // TODO look into using the controller executor
    private volatile ExecutorService proxyExecutor;
    private volatile ScheduledExecutorService pingScheduler;

    public static ServiceController<ModelController> addService(final ServiceTarget serviceTarget,
                                                            final HostControllerEnvironment environment,
                                                            final HostRunningModeControl runningModeControl,
                                                            final ControlledProcessState processState,
                                                            final BootstrapListener bootstrapListener,
                                                            final PathManagerService pathManager){
        final ConcurrentMap<String, ProxyController> hostProxies = new ConcurrentHashMap<String, ProxyController>();
        final Map<String, ProxyController> serverProxies = new ConcurrentHashMap<String, ProxyController>();
        final LocalHostControllerInfoImpl hostControllerInfo = new LocalHostControllerInfoImpl(processState, environment);
        final AbstractVaultReader vaultReader = service(AbstractVaultReader.class);
        ROOT_LOGGER.debugf("Using VaultReader %s", vaultReader);
        final ContentRepository contentRepository = ContentRepository.Factory.create(environment.getDomainContentDir());
        IgnoredDomainResourceRegistry ignoredRegistry = new IgnoredDomainResourceRegistry(hostControllerInfo);
        final PrepareStepHandler prepareStepHandler = new PrepareStepHandler(hostControllerInfo, contentRepository,
                hostProxies, serverProxies, ignoredRegistry);
        DomainModelControllerService service = new DomainModelControllerService(environment, runningModeControl, processState,
                hostControllerInfo, contentRepository, hostProxies, serverProxies, prepareStepHandler, vaultReader,
                ignoredRegistry, bootstrapListener, pathManager);
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
                                         final ConcurrentMap<String, ProxyController> hostProxies,
                                         final Map<String, ProxyController> serverProxies,
                                         final PrepareStepHandler prepareStepHandler,
                                         final AbstractVaultReader vaultReader,
                                         final IgnoredDomainResourceRegistry ignoredRegistry,
                                         final BootstrapListener bootstrapListener,
                                         final PathManagerService pathManager) {
        super(ProcessType.HOST_CONTROLLER, runningModeControl, null, processState,
                DomainDescriptionProviders.ROOT_PROVIDER, prepareStepHandler, new RuntimeExpressionResolver(vaultReader));
        this.environment = environment;
        this.runningModeControl = runningModeControl;
        this.processState = processState;
        this.hostControllerInfo = hostControllerInfo;
        this.localFileRepository = new LocalFileRepository(environment.getDomainBaseDir(), environment.getDomainContentDir(), environment.getDomainConfigurationDir());

        this.remoteFileRepository = new RemoteFileRepository(localFileRepository);
        this.contentRepository = contentRepository;
        this.hostProxies = hostProxies;
        this.serverProxies = serverProxies;
        this.prepareStepHandler = prepareStepHandler;
        this.vaultReader = vaultReader;
        this.ignoredRegistry = ignoredRegistry;
        this.bootstrapListener = bootstrapListener;
        this.extensionRegistry = new ExtensionRegistry(ProcessType.HOST_CONTROLLER, runningModeControl);
        this.pathManager = pathManager;
    }

    @Override
    public RunningMode getCurrentRunningMode() {
        return runningModeControl.getRunningMode();
    }

    @Override
    public LocalHostControllerInfo getLocalHostInfo() {
        return hostControllerInfo;
    }

    @Override
    public void registerRemoteHost(final String hostName, final ManagementChannelHandler handler, final Transformers transformers, Long remoteConnectionId) throws SlaveRegistrationException {
        if (!hostControllerInfo.isMasterDomainController()) {
            throw SlaveRegistrationException.forHostIsNotMaster();
        }

        if (runningModeControl.getRunningMode() == RunningMode.ADMIN_ONLY) {
            throw SlaveRegistrationException.forMasterInAdminOnlyMode(runningModeControl.getRunningMode());
        }

        final PathElement pe = PathElement.pathElement(ModelDescriptionConstants.HOST, hostName);
        final PathAddress addr = PathAddress.pathAddress(pe);
        ProxyController existingController = modelNodeRegistration.getProxyController(addr);

        if (existingController != null || hostControllerInfo.getLocalHostName().equals(pe.getValue())){
            throw SlaveRegistrationException.forHostAlreadyExists(pe.getValue());
        }

        SlaveHostPinger pinger = remoteConnectionId == null ? null : new SlaveHostPinger(hostName, handler, pingScheduler, remoteConnectionId);
        hostRegistrationMap.put(hostName, new HostRegistration(remoteConnectionId, handler, pinger));

        // Create the proxy controller
        final TransformingProxyController hostControllerClient = TransformingProxyController.Factory.create(handler, transformers, addr, ProxyOperationAddressTranslator.HOST);

        modelNodeRegistration.registerProxyController(pe, hostControllerClient);
        hostProxies.put(hostName, hostControllerClient);
        if (pinger != null) {
            pinger.schedulePing(SlaveHostPinger.STD_TIMEOUT, SlaveHostPinger.STD_INTERVAL);
        }
    }

    @Override
    public boolean isHostRegistered(String id) {
        return hostControllerInfo.getLocalHostName().equals(id) || hostRegistrationMap.containsKey(id);
    }

    @Override
    public void unregisterRemoteHost(String id, Long remoteConnectionId) {
        HostRegistration hostRegistration = hostRegistrationMap.get(id);
        if (hostRegistration != null) {
            if ((remoteConnectionId == null || remoteConnectionId.equals(hostRegistration.remoteConnectionId)) && hostRegistrationMap.remove(id, hostRegistration)) {
                if (hostRegistration.pinger != null) {
                    hostRegistration.pinger.cancel();
                }
                hostProxies.remove(id);
                modelNodeRegistration.unregisterProxyController(PathElement.pathElement(HOST, id));
                DOMAIN_LOGGER.unregisteredRemoteSlaveHost(id);
            }
        }

    }

    @Override
    public void pingRemoteHost(String id) {
        HostRegistration reg = hostRegistrationMap.get(id);
        if (reg != null && reg.pinger != null && !reg.pinger.isCancelled()) {
            reg.pinger.schedulePing(SlaveHostPinger.SHORT_TIMEOUT, 0);
        }
    }

    @Override
    public void registerRunningServer(ProxyController serverControllerClient) {
        PathAddress pa = serverControllerClient.getProxyNodeAddress();
        PathElement pe = pa.getElement(1);
        if (modelNodeRegistration.getProxyController(pa) != null) {
            throw MESSAGES.serverNameAlreadyRegistered(pe.getValue());
        }
        ROOT_LOGGER.registeringServer(pe.getValue());
        ManagementResourceRegistration hostRegistration = modelNodeRegistration.getSubModel(PathAddress.pathAddress(PathElement.pathElement(HOST, hostControllerInfo.getLocalHostName())));
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
        ThreadFactory threadFactory = new JBossThreadFactory(new ThreadGroup("proxy-threads"), Boolean.FALSE, null, "%G - %t", null, null, AccessController.getContext());
        proxyExecutor = Executors.newCachedThreadPool(threadFactory);
        ThreadFactory pingerThreadFactory = new JBossThreadFactory(new ThreadGroup("proxy-pinger-threads"), Boolean.TRUE, null, "%G - %t", null, null, AccessController.getContext());
        pingScheduler = Executors.newScheduledThreadPool(PINGER_POOL_SIZE, pingerThreadFactory);
        super.start(context);
    }

    @Override
    protected void initModel(Resource rootResource, ManagementResourceRegistration rootRegistration) {
        DomainModelUtil.updateCoreModel(rootResource, environment);
        HostModelUtil.createRootRegistry(rootRegistration, environment, ignoredRegistry, this);
        this.modelNodeRegistration = rootRegistration;
    }

    // See superclass start. This method is invoked from a separate non-MSC thread after start. So we can do a fair
    // bit of stuff
    @Override
    protected void boot(final BootContext context) throws ConfigurationPersistenceException {

        final ServiceTarget serviceTarget = context.getServiceTarget();
        boolean ok = false;
        boolean reachedServers = false;
        try {
            // Install server inventory callback
            ServerInventoryCallbackService.install(serviceTarget);

            // Parse the host.xml and invoke all the ops. The ops should rollback on any Stage.RUNTIME failure
            // We run the first op ("add-host") separately to let it set up the host ManagementResourceRegistration
            List<ModelNode> hostBootOps = hostControllerConfigurationPersister.load();
            ModelNode addHostOp = hostBootOps.remove(0);
            ok = boot(Collections.singletonList(addHostOp), true);
            ok = ok && boot(hostBootOps, true);

            final RunningMode currentRunningMode = runningModeControl.getRunningMode();

            if (ok) {

                // Now we know our management interface configuration. Install the server inventory
                Future<ServerInventory> inventoryFuture = ServerInventoryService.install(serviceTarget, this, runningModeControl, environment,
                        hostControllerInfo.getNativeManagementInterface(), hostControllerInfo.getNativeManagementPort());

                if (!hostControllerInfo.isMasterDomainController() && !environment.isUseCachedDc()) {
                    serverInventory = getFuture(inventoryFuture);

                    if (hostControllerInfo.getRemoteDomainControllerHost() != null) {
                        Future<MasterDomainControllerClient> clientFuture = RemoteDomainConnectionService.install(serviceTarget,
                                getValue(), extensionRegistry,
                                hostControllerInfo,
                                environment.getProductConfig(),
                                hostControllerInfo.getRemoteDomainControllerSecurityRealm(),
                                remoteFileRepository);
                        MasterDomainControllerClient masterDomainControllerClient = getFuture(clientFuture);
                        //Registers us with the master and gets down the master copy of the domain model to our DC
                        //TODO make sure that the RDCS checks env.isUseCachedDC, and if true falls through to that
                        // BES 2012/02/04 Comment ^^^ implies the semantic is to use isUseCachedDC as a fallback to
                        // a failure to connect as opposed to being an instruction to not connect at all. I believe
                        // the current impl is the latter. Don't change this without a discussion first, as the
                        // current semantic is a reasonable one.
                        try {
                            masterDomainControllerClient.register();
                        } catch (Exception e) {
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

                    if (environment.isUseCachedDc()) {
                        remoteFileRepository.setRemoteFileRepositoryExecutor(new RemoteDomainConnectionService.RemoteFileRepositoryExecutor() {
                            @Override
                            public File getFile(String relativePath, byte repoId, HostFileRepository localFileRepository) {
                                return localFileRepository.getFile(relativePath);
                            }
                        });
                    }

                    // parse the domain.xml and load the steps
                    // TODO look at having LocalDomainControllerAdd do this, using Stage.IMMEDIATE for the steps
                    ConfigurationPersister domainPersister = hostControllerConfigurationPersister.getDomainPersister();
                    ok = boot(domainPersister.load(), false);

                    if (!ok && runningModeControl.getRunningMode().equals(RunningMode.ADMIN_ONLY)) {
                        ROOT_LOGGER.reportAdminOnlyDomainXmlFailure();
                        ok = true;
                    }

                    if (ok) {
                        ManagementRemotingServices.installManagementChannelServices(serviceTarget, ManagementRemotingServices.MANAGEMENT_ENDPOINT,
                                new MasterDomainControllerOperationHandlerService(this, new HostControllerRegistrationHandler.OperationExecutor() {

                                    @Override
                                    public ModelNode execute(final ModelNode operation, final OperationMessageHandler handler, final ModelController.OperationTransactionControl control, final OperationAttachments attachments, final OperationStepHandler step) {
                                        return internalExecute(operation, handler, control, attachments, step);
                                    }

                                }),
                                DomainModelControllerService.SERVICE_NAME, ManagementRemotingServices.DOMAIN_CHANNEL, null, null);
                        serverInventory = getFuture(inventoryFuture);
                    }
                }
            }

            if (ok) {
                // Install the server > host operation handler
                ServerToHostOperationHandlerFactoryService.install(serviceTarget, ServerInventoryService.SERVICE_NAME, proxyExecutor, localFileRepository);

                // demand native mgmt services
                serviceTarget.addService(ServiceName.JBOSS.append("native-mgmt-startup"), Service.NULL)
                        .addDependency(ManagementRemotingServices.channelServiceName(ManagementRemotingServices.MANAGEMENT_ENDPOINT, ManagementRemotingServices.SERVER_CHANNEL))
                        .setInitialMode(ServiceController.Mode.ACTIVE)
                        .install();

                // demand http mgmt services
                serviceTarget.addService(ServiceName.JBOSS.append("http-mgmt-startup"), Service.NULL)
                        .addDependency(ServiceBuilder.DependencyType.OPTIONAL, HttpManagementService.SERVICE_NAME)
                        .setInitialMode(ServiceController.Mode.ACTIVE)
                        .install();

                reachedServers = true;
                if (currentRunningMode == RunningMode.NORMAL) {
                    startServers();
                }
            }

        } catch (Exception e) {
            ROOT_LOGGER.caughtExceptionDuringBoot(e);
            if (!reachedServers) {
                ok = false;
            }
        } finally {
            if (ok) {
                try {
                    finishBoot();
                } finally {
                    bootstrapListener.tick();
                }
            } else {
                // Die!
                ROOT_LOGGER.unsuccessfulBoot();
                System.exit(ExitCodes.HOST_CONTROLLER_ABORT_EXIT_CODE);
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
    public void stop(final StopContext context) {
        serverInventory = null;
        extensionRegistry.clear();
        super.stop(context);

        context.asynchronous();
        Thread executorShutdown = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    pingScheduler.shutdownNow();
                } finally {
                    try {
                        proxyExecutor.shutdown();
                    } finally {
                        context.complete();
                    }
                }
            }
        }, DomainModelControllerService.class.getSimpleName() + " ExecutorService Shutdown Thread");
        executorShutdown.start();
    }


    @Override
    public void stopLocalHost() {
        stopLocalHost(0);
    }

    @Override
    public void stopLocalHost(int exitCode) {
        final ProcessControllerClient client = injectedProcessControllerConnection.getValue().getClient();
        processState.setStopping();
        try {
            client.shutdown(exitCode);
        } catch (IOException e) {
            throw MESSAGES.errorClosingDownHost(e);
        }
    }

    @Override
    public void registerHostModel(String hostName, ManagementResourceRegistration root) {
        HostModelUtil.createHostRegistry(hostName, root, hostControllerConfigurationPersister, environment, runningModeControl,
                localFileRepository, hostControllerInfo, new DelegatingServerInventory(), remoteFileRepository, contentRepository,
                this, extensionRegistry,vaultReader, ignoredRegistry, processState, pathManager);
    }

    private static class HostRegistration {
        private final Long remoteConnectionId;
        private final ManagementChannelHandler channelHandler;
        private final SlaveHostPinger pinger;


        private HostRegistration(Long remoteConnectionId, ManagementChannelHandler channelHandler, SlaveHostPinger pinger) {
            this.remoteConnectionId = remoteConnectionId;
            this.channelHandler = channelHandler;
            this.pinger = pinger;
        }

        @Override
        public int hashCode() {
            return remoteConnectionId == null ? Integer.MIN_VALUE : channelHandler.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof HostRegistration && safeEquals(remoteConnectionId, ((HostRegistration) obj).remoteConnectionId);
        }

        private static boolean safeEquals(Object a, Object b) {
            return a == b || (a != null && a.equals(b));
        }
    }

    private class DelegatingServerInventory implements ServerInventory {
        public void serverCommunicationRegistered(String serverProcessName, ManagementChannelHandler channelHandler) {
            serverInventory.serverCommunicationRegistered(serverProcessName, channelHandler);
        }

        public boolean serverReconnected(String serverProcessName, ManagementChannelHandler channelHandler) {
            return serverInventory.serverReconnected(serverProcessName, channelHandler);
        }

        public void serverProcessAdded(String serverProcessName) {
            serverInventory.serverProcessAdded(serverProcessName);
        }

        public void serverStartFailed(String serverProcessName) {
            serverInventory.serverStartFailed(serverProcessName);
        }

        @Override
        public void serverStarted(String serverProcessName) {
            serverInventory.serverStarted(serverProcessName);
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

        public ServerStatus startServer(String serverName, ModelNode domainModel) {
            return serverInventory.startServer(serverName, domainModel);
        }

        @Override
        public ServerStatus startServer(String serverName, ModelNode domainModel, boolean blocking) {
            return serverInventory.startServer(serverName, domainModel, blocking);
        }

        public void reconnectServer(String serverName, ModelNode domainModel, boolean running) {
            serverInventory.reconnectServer(serverName, domainModel, running);
        }

        public ServerStatus restartServer(String serverName, int gracefulTimeout, ModelNode domainModel) {
            return serverInventory.restartServer(serverName, gracefulTimeout, domainModel);
        }

        @Override
        public ServerStatus restartServer(String serverName, int gracefulTimeout, ModelNode domainModel, boolean blocking) {
            return serverInventory.restartServer(serverName, gracefulTimeout, domainModel, blocking);
        }

        public ServerStatus stopServer(String serverName, int gracefulTimeout) {
            return serverInventory.stopServer(serverName, gracefulTimeout);
        }

        @Override
        public ServerStatus stopServer(String serverName, int gracefulTimeout, boolean blocking) {
            return serverInventory.stopServer(serverName, gracefulTimeout, blocking);
        }

        public CallbackHandler getServerCallbackHandler() {
            return serverInventory.getServerCallbackHandler();
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

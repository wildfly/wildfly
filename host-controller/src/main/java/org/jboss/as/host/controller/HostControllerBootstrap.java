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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CRITERIA;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DOMAIN_CONTROLLER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HTTP_API;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.LOCAL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NATIVE_API;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PORT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOTE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLBACK_ON_RUNTIME_FAILURE;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.client.ExecutionContextBuilder;
import org.jboss.as.controller.interfaces.ParsedInterfaceCriteria;
import org.jboss.as.controller.persistence.ExtensibleConfigurationPersister;
import org.jboss.as.domain.controller.DomainController;
import org.jboss.as.domain.controller.DomainControllerService;
import org.jboss.as.domain.controller.FileRepository;
import org.jboss.as.domain.controller.MasterDomainControllerClient;
import org.jboss.as.domain.controller.HostControllerProxy;
import org.jboss.as.host.controller.mgmt.DomainControllerOperationHandlerService;
import org.jboss.as.host.controller.mgmt.ManagementCommunicationService;
import org.jboss.as.host.controller.mgmt.ManagementCommunicationServiceInjector;
import org.jboss.as.host.controller.mgmt.ServerToHostOperationHandler;
import org.jboss.as.process.ProcessControllerClient;
import org.jboss.as.server.mgmt.HttpManagementService;
import org.jboss.as.server.services.net.NetworkInterfaceBinding;
import org.jboss.as.server.services.net.NetworkInterfaceService;
import org.jboss.as.threads.ThreadFactoryService;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.logging.Logger;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * @author Emanuel Muckenhuber
 */
public class HostControllerBootstrap {

    private static final Logger log = Logger.getLogger("org.jboss.as.host.controller");
    static final ServiceName SERVICE_NAME_BASE = ServiceName.JBOSS.append("host", "controller");
    static final int DEFAULT_POOL_SIZE = 20;
    private final ServiceContainer serviceContainer = ServiceContainer.Factory.create();
    private final HostControllerEnvironment environment;
    private final byte[] authCode;

    public HostControllerBootstrap(final HostControllerEnvironment environment, final byte[] authCode) {
        this.environment = environment;
        this.authCode = authCode;
    }

    /**
     * Start the host controller services.
     *
     * @throws Exception
     */
    public void start() throws Exception {
        final File configDir = environment.getDomainConfigurationDir();
        final ExtensibleConfigurationPersister configurationPersister = createHostConfigurationPersister(configDir);
        final HostModel hostModel = new HostModel(configurationPersister);

        // Load the host model
        final List<ModelNode> operations = configurationPersister.load();
        final AtomicInteger count = new AtomicInteger(1);
        final ResultHandler resultHandler = new ResultHandler() {
            @Override
            public void handleResultFragment(final String[] location, final ModelNode result) {
            }

            @Override
            public void handleResultComplete() {
                if (count.decrementAndGet() == 0) {
                    // some action
                }
            }

            @Override
            public void handleFailed(final ModelNode failureDescription) {
                if (count.decrementAndGet() == 0) {
                    // some action
                }
            }

            @Override
            public void handleCancellation() {
                if (count.decrementAndGet() == 0) {
                    // some action
                }
            }
        };
        for(final ModelNode operation : operations) {
            count.incrementAndGet();
            operation.get(ROLLBACK_ON_RUNTIME_FAILURE).set(false);
            hostModel.execute(ExecutionContextBuilder.Factory.create(operation).build(), resultHandler);
        }
        if (count.decrementAndGet() == 0) {
            // some action?
        }

        final ServiceTarget serviceTarget = serviceContainer;
        serviceTarget.addListener(new AbstractServiceListener<Object>() {
            @Override
            public void serviceFailed(final ServiceController<?> serviceController, final StartException reason) {
                log.errorf(reason, "Service [%s] failed.", serviceController.getName());
            }
        });
        // Install the process controller client
        final ProcessControllerConnectionService processControllerClient = new ProcessControllerConnectionService(environment, authCode);
        serviceTarget.addService(ProcessControllerConnectionService.SERVICE_NAME, processControllerClient).install();
        // Get the raw model
        final ModelNode rawModel = hostModel.getHostModel();
        // manually install the network interface services
        activateNetworkInterfaces(rawModel, serviceTarget);
        //
        final ServiceName threadFactoryServiceName = SERVICE_NAME_BASE.append("thread-factory");
        final ServiceName executorServiceName = SERVICE_NAME_BASE.append("executor");

        serviceTarget.addService(threadFactoryServiceName, new ThreadFactoryService()).install();
        final HostControllerExecutorService executorService = new HostControllerExecutorService();
        serviceTarget.addService(executorServiceName, executorService)
            .addDependency(threadFactoryServiceName, ThreadFactory.class, executorService.threadFactoryValue)
            .install();
        //
        final String mgmtNetwork = rawModel.get(MANAGEMENT, NATIVE_API, INTERFACE).asString();
        final int mgmtPort = rawModel.get(MANAGEMENT, NATIVE_API, PORT).asInt();
        final ServerInventoryService inventory = new ServerInventoryService(environment, mgmtPort);
        serviceTarget.addService(ServerInventoryService.SERVICE_NAME, inventory)
            .addDependency(ProcessControllerConnectionService.SERVICE_NAME, ProcessControllerClient.class, inventory.getClient())
            .addDependency(NetworkInterfaceService.JBOSS_NETWORK_INTERFACE.append(mgmtNetwork), NetworkInterfaceBinding.class, inventory.getInterface())
            .install();

        final String name = rawModel.get(NAME).asString();
        final FileRepository repository = new LocalFileRepository(environment);
        final HostControllerService hc = new HostControllerService(name, hostModel, repository);
        serviceTarget.addService(HostController.SERVICE_NAME, hc)
            .addDependency(ServerInventoryService.SERVICE_NAME, ServerInventory.class, hc.getServerInventory())
            .addDependency(ServerToHostOperationHandler.SERVICE_NAME) // make sure servers can register
            .setInitialMode(Mode.ACTIVE)
            .install();

        //  Add the management communication service
        final ManagementCommunicationService managementCommunicationService = new ManagementCommunicationService();
        serviceTarget.addService(ManagementCommunicationService.SERVICE_NAME, managementCommunicationService)
            .addDependency(NetworkInterfaceService.JBOSS_NETWORK_INTERFACE.append(mgmtNetwork), NetworkInterfaceBinding.class, managementCommunicationService.getInterfaceInjector())
            .addInjection(managementCommunicationService.getPortInjector(), mgmtPort)
            .addDependency(executorServiceName, ExecutorService.class, managementCommunicationService.getExecutorServiceInjector())
            .addDependency(threadFactoryServiceName, ThreadFactory.class, managementCommunicationService.getThreadFactoryInjector())
            .setInitialMode(Mode.ACTIVE)
            .install();

        // install the domain controller
        activateDomainController(environment, rawModel, serviceTarget, repository, environment.isBackupDomainFiles(), environment.isUseCachedDc());

        if (rawModel.get(MANAGEMENT).hasDefined(HTTP_API)) {
            final HttpManagementService service = new HttpManagementService();
            serviceTarget.addService(HttpManagementService.SERVICE_NAME, service)
                    .addDependency(NetworkInterfaceService.JBOSS_NETWORK_INTERFACE.append(rawModel.get(MANAGEMENT, HTTP_API).require(INTERFACE).asString()), NetworkInterfaceBinding.class, service.getInterfaceInjector())
                    .addDependency(DomainController.SERVICE_NAME, ModelController.class, service.getModelControllerInjector())
                    .addInjection(service.getPortInjector(), rawModel.get(MANAGEMENT, HTTP_API).require(PORT).asInt())
                    .addDependency(executorServiceName, ExecutorService.class, service.getExecutorServiceInjector())
                    .setInitialMode(ServiceController.Mode.ACTIVE)
                    .addListener(new ResultHandler.ServiceStartListener(resultHandler))
                    .install();

        }



        // Add the server to host operation handler
        final ServerToHostOperationHandler serverToHost = new ServerToHostOperationHandler();
        serviceTarget.addService(ServerToHostOperationHandler.SERVICE_NAME, serverToHost)
            .addDependency(ServerInventoryService.SERVICE_NAME, ManagedServerLifecycleCallback.class, serverToHost.getCallback())
            .addDependency(ManagementCommunicationService.SERVICE_NAME, ManagementCommunicationService.class, new ManagementCommunicationServiceInjector(serverToHost))
            .install();
    }

    /**
     * Install the domain controller connection.
     *
     * @param environment the host controller environment
     * @param host the host model
     * @param serviceTarget the service target
     * @param repository the local file repository
     * @param backupDomainFiles whether the remote domain controller should be backed up
     * @param useCachedDc Pass in true if this is a slave domain controller, and we want to be able
     */
    static void activateDomainController(final HostControllerEnvironment environment, final ModelNode host, final ServiceTarget serviceTarget,
            final FileRepository repository, final boolean backupDomainFiles, final boolean useCachedDc) {
        boolean slave = !host.get(DOMAIN_CONTROLLER, LOCAL).isDefined();
        if (slave) {
            installRemoteDomainControllerConnection(environment, host, serviceTarget, repository);
        }
        installLocalDomainController(environment, host, serviceTarget, slave, repository, backupDomainFiles, useCachedDc);
    }

    static void installLocalDomainController(final HostControllerEnvironment environment, final ModelNode host, final ServiceTarget serviceTarget, final boolean isSlave,
            final FileRepository repository, final boolean backupDomainFiles, final boolean useCachedDc) {
        final String hostName = host.get(NAME).asString();
        final File configDir = environment.getDomainConfigurationDir();
        final ExtensibleConfigurationPersister domainConfigurationPersister = createDomainConfigurationPersister(configDir, isSlave);
        final DomainControllerService dcService = new DomainControllerService(domainConfigurationPersister, hostName, repository, backupDomainFiles, useCachedDc);
        ServiceBuilder<DomainController> builder = serviceTarget.addService(DomainController.SERVICE_NAME, dcService);
        if (isSlave) {
            builder.addDependency(MasterDomainControllerClient.SERVICE_NAME, MasterDomainControllerClient.class, dcService.getMasterDomainControllerClientInjector());
        }
        builder.addDependency(SERVICE_NAME_BASE.append("executor"), ScheduledExecutorService.class, dcService.getScheduledExecutorServiceInjector())
            .addDependency(HostController.SERVICE_NAME, HostControllerProxy.class, dcService.getHostControllerServiceInjector())
            .install();

        //Install the domain controller operation handler
        final DomainControllerOperationHandlerService operationHandlerService = new DomainControllerOperationHandlerService();
        serviceTarget.addService(DomainControllerOperationHandlerService.SERVICE_NAME, operationHandlerService)
            .addDependency(ManagementCommunicationService.SERVICE_NAME, ManagementCommunicationService.class, operationHandlerService.getManagementCommunicationServiceValue())
            .addDependency(DomainController.SERVICE_NAME, ModelController.class, operationHandlerService.getModelControllerValue())
            .setInitialMode(ServiceController.Mode.ACTIVE)
            .install();
    }

    static void installRemoteDomainControllerConnection(final HostControllerEnvironment environment, final ModelNode host, final ServiceTarget serviceTarget, final FileRepository repository) {

        String name;
        try {
            name = host.require(NAME).asString();
        } catch (NoSuchElementException e1) {
            throw new IllegalArgumentException("A host connecting to a remote domain controller must have its name attribute set");
        }

        final ModelNode dc = host.require(DOMAIN_CONTROLLER).require(REMOTE);
        InetAddress addr;
        try {
            addr = InetAddress.getByName(dc.require(HOST).asString());
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        int port = dc.require(PORT).asInt();
        final RemoteDomainConnectionService service = new RemoteDomainConnectionService(name, addr, port, repository);
        serviceTarget.addService(MasterDomainControllerClient.SERVICE_NAME, service)
            .setInitialMode(Mode.ACTIVE)
            .install();
    }

    /**
     * Manually install the interface services.
     *
     * @param host the host model
     * @param serviceTarget the service target
     */
    static void activateNetworkInterfaces(final ModelNode host, final ServiceTarget serviceTarget) {
        for(final Property iFace : host.get(INTERFACE).asPropertyList()) {
            final String interfaceName = iFace.getName();
            final ParsedInterfaceCriteria criteria = ParsedInterfaceCriteria.parse(iFace.getValue().require(CRITERIA));
            serviceTarget.addService(NetworkInterfaceService.JBOSS_NETWORK_INTERFACE.append(interfaceName), NetworkInterfaceService.create(interfaceName, criteria))
                .setInitialMode(Mode.ON_DEMAND)
                .install();
        }
    }

    /**
     * Create the host.xml configuration persister.
     *
     * @param configDir the domain configuration directory
     * @return the configuration persister
     */
    static ExtensibleConfigurationPersister createHostConfigurationPersister(final File configDir) {
        return ConfigurationPersisterFactory.createHostXmlConfigurationPersister(configDir);
    }

    /**
     * Create the domain.xml configuration persister, in case the DC is running in process.
     *
     * @param configDir the domain configuration directory
     * @param isSlave true if we are a slave
     * @return the configuration persister
     */
    static ExtensibleConfigurationPersister createDomainConfigurationPersister(final File configDir, boolean isSlave) {
        if (isSlave) {
            return ConfigurationPersisterFactory.createDomainXmlConfigurationPersister(configDir, "cached-remote-domain.xml");
        } else {
            return ConfigurationPersisterFactory.createDomainXmlConfigurationPersister(configDir);
        }
    }

    static final class HostControllerExecutorService implements Service<Executor> {
        final InjectedValue<ThreadFactory> threadFactoryValue = new InjectedValue<ThreadFactory>();
        private ScheduledExecutorService executorService;

        @Override
        public synchronized void start(final StartContext context) throws StartException {
            executorService = Executors.newScheduledThreadPool(DEFAULT_POOL_SIZE, threadFactoryValue.getValue());
        }

        @Override
        public synchronized void stop(final StopContext context) {
            executorService.shutdown();
        }

        @Override
        public synchronized ScheduledExecutorService getValue() throws IllegalStateException {
            return executorService;
        }
    }

}

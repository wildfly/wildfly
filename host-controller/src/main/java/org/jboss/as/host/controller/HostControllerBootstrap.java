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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.LOCAL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NATIVE_API;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PORT;

import java.io.File;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.interfaces.ParsedInterfaceCriteria;
import org.jboss.as.controller.persistence.ExtensibleConfigurationPersister;
import org.jboss.as.domain.controller.DomainController;
import org.jboss.as.domain.controller.DomainControllerService;
import org.jboss.as.domain.controller.FileRepository;
import org.jboss.as.host.controller.mgmt.DomainControllerOperationHandlerService;
import org.jboss.as.host.controller.mgmt.ManagementCommunicationService;
import org.jboss.as.host.controller.mgmt.ManagementCommunicationServiceInjector;
import org.jboss.as.host.controller.mgmt.ServerToHostOperationHandler;
import org.jboss.as.process.ProcessControllerClient;
import org.jboss.as.server.services.net.NetworkInterfaceBinding;
import org.jboss.as.server.services.net.NetworkInterfaceService;
import org.jboss.as.threads.ThreadFactoryService;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.logging.Logger;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.Service;
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
            public void handleResultComplete(final ModelNode compensatingOperation) {
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
            hostModel.execute(operation, resultHandler);
        }
        if (count.decrementAndGet() == 0) {
            // some action?
        }

        final BatchBuilder batch = serviceContainer.batchBuilder();
        batch.addListener(new AbstractServiceListener<Object>() {
            public void serviceFailed(final ServiceController<?> serviceController, final StartException reason) {
                log.errorf(reason, "Service [%s] failed.", serviceController.getName());
            }
        });
        // Get the raw model
        final ModelNode rawModel = hostModel.getHostModel();
        final String mgmtNetwork = rawModel.get(MANAGEMENT, NATIVE_API, INTERFACE).asString();
        final int mgmtPort = rawModel.get(MANAGEMENT, NATIVE_API, PORT).asInt();
        // Install the process controller client
        final ProcessControllerConnectionService processControllerClient = new ProcessControllerConnectionService(environment, authCode);
        batch.addService(ProcessControllerConnectionService.SERVICE_NAME, processControllerClient).install();
        // manually install the network interface services
        activateNetworkInterfaces(rawModel, batch);
        // install the domain controller connection
        activateDomainControllerConnection(environment, rawModel, batch);
        //
        final ServerInventoryService inventory = new ServerInventoryService(environment, mgmtPort);
        batch.addService(ServerInventoryService.SERVICE_NAME, inventory)
            .addDependency(ProcessControllerConnectionService.SERVICE_NAME, ProcessControllerClient.class, inventory.getClient())
            .addDependency(NetworkInterfaceService.JBOSS_NETWORK_INTERFACE.append(mgmtNetwork), NetworkInterfaceBinding.class, inventory.getInterface())
            .addDependency(DomainControllerConnection.SERVICE_NAME, DomainControllerConnection.class, inventory.getDomainControllerConnection())
            .install();

        final String name = rawModel.get(NAME).asString();
        final FileRepository repository = new LocalFileRepository(environment);
        final HostControllerService hc = new HostControllerService(name, hostModel, repository);
        batch.addService(HostController.SERVICE_NAME, hc)
            .addDependency(DomainControllerConnection.SERVICE_NAME, DomainControllerConnection.class, hc.getConnection())
            .addDependency(ServerInventoryService.SERVICE_NAME, ServerInventory.class, hc.getServerInventory())
            .addDependency(ServerToHostOperationHandler.SERVICE_NAME) // make sure servers can register
            .setInitialMode(Mode.ACTIVE)
            .install();

        //
        final ServiceName threadFactoryServiceName = SERVICE_NAME_BASE.append("thread-factory");
        final ServiceName executorServiceName = SERVICE_NAME_BASE.append("executor");

        batch.addService(threadFactoryServiceName, new ThreadFactoryService()).install();
        final HostControllerExecutorService executorService = new HostControllerExecutorService();
        batch.addService(executorServiceName, executorService)
            .addDependency(threadFactoryServiceName, ThreadFactory.class, executorService.threadFactoryValue)
            .install();

        //  Add the management communication service
        final ManagementCommunicationService managementCommunicationService = new ManagementCommunicationService();
        batch.addService(ManagementCommunicationService.SERVICE_NAME, managementCommunicationService)
            .addDependency(NetworkInterfaceService.JBOSS_NETWORK_INTERFACE.append(mgmtNetwork), NetworkInterfaceBinding.class, managementCommunicationService.getInterfaceInjector())
            .addInjection(managementCommunicationService.getPortInjector(), mgmtPort)
            .addDependency(executorServiceName, ExecutorService.class, managementCommunicationService.getExecutorServiceInjector())
            .addDependency(threadFactoryServiceName, ThreadFactory.class, managementCommunicationService.getThreadFactoryInjector())
            .setInitialMode(Mode.ACTIVE)
            .install();

        // Add the server to host operation handler
        final ServerToHostOperationHandler serverToHost = new ServerToHostOperationHandler();
        batch.addService(ServerToHostOperationHandler.SERVICE_NAME, serverToHost)
            .addDependency(ServerInventoryService.SERVICE_NAME, ManagedServerLifecycleCallback.class, serverToHost.getCallback())
            .addDependency(ManagementCommunicationService.SERVICE_NAME, ManagementCommunicationService.class,  new ManagementCommunicationServiceInjector(serverToHost))
            .install();


        batch.install();
    }

    /**
     * Install the domain controller connection.
     *
     * @param environment the host controller environment
     * @param host the host model
     * @param serviceTarget the service target
     */
    static void activateDomainControllerConnection(final HostControllerEnvironment environment, final ModelNode host, final ServiceTarget serviceTarget) {
        if (host.get(DOMAIN_CONTROLLER, LOCAL).isDefined()) {
            installLocalDomainController(environment, host, serviceTarget);
        } else {
            installRemoteDomainControllerConnection(environment, host, serviceTarget);
        }
    }

    static void installLocalDomainController(final HostControllerEnvironment environment, final ModelNode host, final ServiceTarget serviceTarget) {
        final File configDir = environment.getDomainConfigurationDir();
        final ExtensibleConfigurationPersister domainConfigurationPersister = createDomainConfigurationPersister(configDir);
        final DomainControllerService dcService = new DomainControllerService(domainConfigurationPersister);
        serviceTarget.addService(DomainController.SERVICE_NAME, dcService).install();

        final LocalDomainConnectionService service = new LocalDomainConnectionService();
        serviceTarget.addService(DomainControllerConnection.SERVICE_NAME, service)
            .addDependency(DomainController.SERVICE_NAME, DomainController.class, service.getDomainController())
            .setInitialMode(Mode.ACTIVE)
            .install();

        //Install the domain controller operation handler
        final DomainControllerOperationHandlerService operationHandlerService = new DomainControllerOperationHandlerService();
        serviceTarget.addService(DomainControllerOperationHandlerService.SERVICE_NAME, operationHandlerService)
            .addDependency(ManagementCommunicationService.SERVICE_NAME, ManagementCommunicationService.class, operationHandlerService.getManagementCommunicationServiceValue())
            .addDependency(DomainController.SERVICE_NAME, ModelController.class, operationHandlerService.getModelControllerValue())
            .setInitialMode(ServiceController.Mode.ACTIVE)
            .install();
    }

    static void installRemoteDomainControllerConnection(final HostControllerEnvironment environment, final ModelNode host, final ServiceTarget serviceTarget) {
        // TODO
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
     * @return the configuration persister
     */
    static ExtensibleConfigurationPersister createDomainConfigurationPersister(final File configDir) {
        return ConfigurationPersisterFactory.createDomainXmlConfigurationPersister(configDir);
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

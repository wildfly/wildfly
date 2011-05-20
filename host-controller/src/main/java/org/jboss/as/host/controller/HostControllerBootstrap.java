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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT_INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NATIVE_INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PORT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLBACK_ON_RUNTIME_FAILURE;

import java.io.File;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.client.OperationBuilder;

import org.jboss.as.controller.persistence.ConfigurationFile;

import org.jboss.as.controller.persistence.ExtensibleConfigurationPersister;
import org.jboss.as.controller.registry.ModelNodeRegistration;
import org.jboss.as.domain.controller.DomainModelImpl;
import org.jboss.as.host.controller.mgmt.ManagementCommunicationService;
import org.jboss.as.host.controller.mgmt.ManagementCommunicationServiceInjector;
import org.jboss.as.host.controller.mgmt.ServerToHostOperationHandler;
import org.jboss.as.server.services.net.NetworkInterfaceBinding;
import org.jboss.as.server.services.net.NetworkInterfaceService;
import org.jboss.as.server.services.path.AbsolutePathService;
import org.jboss.as.threads.ThreadFactoryService;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.msc.service.AbstractServiceListener;
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
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class HostControllerBootstrap {

    private static final Logger log = Logger.getLogger("org.jboss.as.host.controller");
    static final ServiceName SERVICE_NAME_BASE = ServiceName.JBOSS.append("host", "controller");
    static final int DEFAULT_POOL_SIZE = 20;
    private final ServiceContainer serviceContainer = ServiceContainer.Factory.create("host-controller");
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
        final ConfigurationFile configurationFile = environment.getHostConfigurationFile();
        final ExtensibleConfigurationPersister configurationPersister = createHostConfigurationPersister(configDir, configurationFile);

        // The first step is to load the host model, this also ensures there are no parsing errors before we
        // spend time initialising enything else.
        final List<ModelNode> operations = configurationPersister.load();

        // The first default services are registered before the bootstrap operations are executed.
        final ServiceTarget serviceTarget = serviceContainer;
        serviceTarget.addListener(new AbstractServiceListener<Object>() {
            @Override
            public void serviceFailed(final ServiceController<?> serviceController, final StartException reason) {
                log.errorf(reason, "Service [%s] failed.", serviceController.getName());
            }
        });

        registerBaseServices(serviceTarget);

        // The Bootstrap domain model is initialised ready for the operations to bootstrap the remainder of the
        // host controller.
        DomainModelProxyImpl domainModelProxy = new DomainModelProxyImpl();
        final ModelNodeRegistration hostRegistry = HostModelUtil.createHostRegistry(configurationPersister, environment, domainModelProxy);
        final ModelNodeRegistration rootRegistration = HostModelUtil.createBootstrapHostRegistry(hostRegistry, domainModelProxy);
        DomainModelImpl domainModel = new DomainModelImpl(rootRegistration, serviceContainer, configurationPersister);
        domainModelProxy.setDomainModel(domainModel);

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

        for (final ModelNode operation : operations) {
            count.incrementAndGet();
            operation.get(OPERATION_HEADERS, ROLLBACK_ON_RUNTIME_FAILURE).set(false);
            domainModel.execute(OperationBuilder.Factory.create(operation).build(), resultHandler);
        }
        if (count.decrementAndGet() == 0) {
            // some action?
        }

        final String hostName = domainModel.getLocalHostName();
        final ModelNode hostModelNode = domainModel.getHostModel();

        final String mgmtNetwork = hostModelNode.get(MANAGEMENT_INTERFACE, NATIVE_INTERFACE, INTERFACE).asString();
        final int mgmtPort = hostModelNode.get(MANAGEMENT_INTERFACE, NATIVE_INTERFACE, PORT).asInt();
        final ServerInventoryService inventory = new ServerInventoryService(environment, mgmtPort);
        serviceTarget.addService(ServerInventoryService.SERVICE_NAME, inventory)
                .addDependency(ProcessControllerConnectionService.SERVICE_NAME, ProcessControllerConnectionService.class, inventory.getClient())
                .addDependency(NetworkInterfaceService.JBOSS_NETWORK_INTERFACE.append(mgmtNetwork), NetworkInterfaceBinding.class, inventory.getInterface())
                .install();

        final HostControllerService hc = new HostControllerService(hostName, hostModelNode, configurationPersister, hostRegistry);
        serviceTarget.addService(HostController.SERVICE_NAME, hc)
                .addDependency(ServerInventoryService.SERVICE_NAME, ServerInventory.class, hc.getServerInventory())
                .addDependency(ServerToHostOperationHandler.SERVICE_NAME) // make sure servers can register
                .setInitialMode(Mode.ACTIVE)
                .install();

        // Add the server to host operation handler
        final ServerToHostOperationHandler serverToHost = new ServerToHostOperationHandler();
        serviceTarget.addService(ServerToHostOperationHandler.SERVICE_NAME, serverToHost)
            .addDependency(ServerInventoryService.SERVICE_NAME, ManagedServerLifecycleCallback.class, serverToHost.getCallback())
            .addDependency(ManagementCommunicationService.SERVICE_NAME, ManagementCommunicationService.class, new ManagementCommunicationServiceInjector(serverToHost))
            .install();
    }

    /**
     * Registers the base services required before we bootstrap the host configuration.
     */
    private void registerBaseServices(final ServiceTarget serviceTarget) {
        // Install the process controller client
        final ProcessControllerConnectionService processControllerClient = new ProcessControllerConnectionService(environment, authCode);
        serviceTarget.addService(ProcessControllerConnectionService.SERVICE_NAME, processControllerClient).install();

        // Thread Factory and Executor Services
        final ServiceName threadFactoryServiceName = SERVICE_NAME_BASE.append("thread-factory");
        final ServiceName executorServiceName = SERVICE_NAME_BASE.append("executor");

        serviceTarget.addService(threadFactoryServiceName, new ThreadFactoryService()).install();
        final HostControllerExecutorService executorService = new HostControllerExecutorService();
        serviceTarget.addService(executorServiceName, executorService)
                .addDependency(threadFactoryServiceName, ThreadFactory.class, executorService.threadFactoryValue)
                .install();

        // Install required path services. (Only install those identified as required)
        AbsolutePathService.addService(HostControllerEnvironment.HOME_DIR, environment.getHomeDir().getAbsolutePath(), serviceTarget);
        AbsolutePathService.addService(HostControllerEnvironment.DOMAIN_CONFIG_DIR, environment.getDomainConfigurationDir().getAbsolutePath(), serviceTarget);
    }

    /**
     * Create the host.xml configuration persister.
     *
     * @param configDir the domain configuration directory
     * @param configurationFile the configuration file
     * @return the configuration persister
     */
    static ExtensibleConfigurationPersister createHostConfigurationPersister(final File configDir, final ConfigurationFile configurationFile) {
        return ConfigurationPersisterFactory.createHostXmlConfigurationPersister(configDir, configurationFile);
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

    /**
     * A Proxy to allow access to a DomainModel which will be created later.
     */
    static final class DomainModelProxyImpl implements DomainModelProxy {

        private DomainModelImpl domainModel;

        public void setDomainModel(final DomainModelImpl domainModel) {
            this.domainModel = domainModel;
        }

        @Override
        public DomainModelImpl getDomainModel() {
            if (domainModel == null) {
                throw new IllegalStateException("DomainModel has not been set.");
            }

            return domainModel;
        }
    }

}

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
package org.jboss.as.ejb3.subsystem;

import org.infinispan.Cache;
import org.jboss.as.clustering.GroupMembershipNotifierRegistry;
import org.jboss.as.clustering.infinispan.subsystem.EmbeddedCacheManagerService;
import org.jboss.as.clustering.registry.Registry;
import org.jboss.as.clustering.registry.RegistryService;
import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.ejb3.cache.impl.backing.clustering.GroupMembershipNotifierRegistryService;
import org.jboss.as.ejb3.deployment.DeploymentRepository;
import org.jboss.as.ejb3.remote.EJBRemoteConnectorService;
import org.jboss.as.ejb3.remote.EJBRemoteTransactionsRepository;
import org.jboss.as.ejb3.remote.EJBRemotingConnectorClientMappingsEntryProviderService;
import org.jboss.as.network.ClientMapping;
import org.jboss.as.remoting.RemotingServices;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.ServerEnvironmentService;
import org.jboss.as.txn.service.TransactionManagerService;
import org.jboss.as.txn.service.UserTransactionService;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.remoting3.Endpoint;

import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.*;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.REMOTE;

/**
 * A {@link AbstractBoottimeAddStepHandler} to handle the add operation for the EJB
 * remote service, in the EJB subsystem
 *
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class EJB3RemoteServiceAdd extends AbstractBoottimeAddStepHandler {
    static final EJB3RemoteServiceAdd INSTANCE = new EJB3RemoteServiceAdd();

    static final String DEFAULT_CLIENT_MAPPINGS_CACHE_CONTAINER_REF = "sfsb";
    static final String DEFAULT_CLIENT_MAPPINGS_CACHE_REF = "ejb-remote-connector-client-mappings";

    private EJB3RemoteServiceAdd() {
    }

    static ModelNode create(final String connectorName, final String threadPoolName, final String clientMappingCacheContainerRef,
                            final String clientMappingCacheRef) {
        // set the address for this operation
        final ModelNode address = new ModelNode();
        address.add(SUBSYSTEM, EJB3Extension.SUBSYSTEM_NAME);
        address.add(SERVICE, REMOTE);

        ModelNode operation = new ModelNode();
        operation.get(OP).set(ADD);
        operation.get(OP_ADDR).set(address);

        operation.get(CONNECTOR_REF).set(connectorName);
        operation.get(THREAD_POOL_NAME).set(threadPoolName);
        operation.get(CLIENT_MAPPINGS_CACHE_CONTAINER_REF).set(clientMappingCacheContainerRef);
        operation.get(CLIENT_MAPPINGS_CACHE_REF).set(clientMappingCacheRef);

        return operation;
    }

    // TODO why is this a boottime-only handler?
    @Override
    protected void performBoottime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {
        newControllers.addAll(installRuntimeServices(context, model, verificationHandler));
        // add ejb remote transactions repository service
        final EJBRemoteTransactionsRepository transactionsRepository = new EJBRemoteTransactionsRepository();
        final ServiceTarget serviceTarget = context.getServiceTarget();
        final ServiceController transactionRepositoryServiceController = serviceTarget.addService(EJBRemoteTransactionsRepository.SERVICE_NAME, transactionsRepository)
                .addDependency(TransactionManagerService.SERVICE_NAME, TransactionManager.class, transactionsRepository.getTransactionManagerInjector())
                .addDependency(UserTransactionService.SERVICE_NAME, UserTransaction.class, transactionsRepository.getUserTransactionInjector())
                .setInitialMode(ServiceController.Mode.ACTIVE)
                .install();
        newControllers.add(transactionRepositoryServiceController);
    }

    Collection<ServiceController<?>> installRuntimeServices(final OperationContext context, final ModelNode model, final ServiceVerificationHandler verificationHandler) {
        final String connectorName = model.require(CONNECTOR_REF).asString();
        final String threadPoolName = model.require(THREAD_POOL_NAME).asString();
        String clientMappingCacheContainerRef = DEFAULT_CLIENT_MAPPINGS_CACHE_CONTAINER_REF;
        if (model.hasDefined(CLIENT_MAPPINGS_CACHE_CONTAINER_REF)) {
            clientMappingCacheContainerRef = model.get(CLIENT_MAPPINGS_CACHE_CONTAINER_REF).asString();
        }
        String clientMappingCacheRef = DEFAULT_CLIENT_MAPPINGS_CACHE_REF;
        if (model.hasDefined(CLIENT_MAPPINGS_CACHE_REF)) {
            clientMappingCacheRef = model.get(CLIENT_MAPPINGS_CACHE_REF).asString();
        }
        final ServiceName remotingServerServiceName = RemotingServices.serverServiceName(connectorName);

        final List<ServiceController<?>> services = new ArrayList<ServiceController<?>>();
        final ServiceTarget serviceTarget = context.getServiceTarget();

        // Install the client-mapping service for the remoting connector
        final EJBRemotingConnectorClientMappingsEntryProviderService clientMappingEntryProviderService = new EJBRemotingConnectorClientMappingsEntryProviderService(remotingServerServiceName);
        final ServiceBuilder clientMappingEntryProviderServiceBuilder = serviceTarget.addService(EJBRemotingConnectorClientMappingsEntryProviderService.SERVICE_NAME, clientMappingEntryProviderService)
                .addDependency(remotingServerServiceName)
                .addDependency(ServerEnvironmentService.SERVICE_NAME, ServerEnvironment.class, clientMappingEntryProviderService.getServerEnvironmentInjector());
        if (verificationHandler != null) {
            clientMappingEntryProviderServiceBuilder.addListener(verificationHandler);
        }
        final ServiceController clientMappingEntryProviderServiceController = clientMappingEntryProviderServiceBuilder.install();
        // add it to the services to be returned
        services.add(clientMappingEntryProviderServiceController);


        // Install the clustered registry service backed by the client-mapping registry entry provider
        final RegistryService<String, List<ClientMapping>> clientMappingRegistryService = new RegistryService<String, List<ClientMapping>>(clientMappingEntryProviderService.getRegistryEntryProvider());
        // Form the client-mapping cache's ServiceName
        final ServiceName clientMappingCacheContainerServiceName = EmbeddedCacheManagerService.getServiceName(clientMappingCacheContainerRef);
        final ServiceName clientMappingCacheServiceName = clientMappingCacheContainerServiceName.append(clientMappingCacheRef);
        // install the clustered registry service as a PASSIVE service so that it will be started only if the backing cache
        // service is installed (which happens only in the presence of clustering subsystem)
        final ServiceBuilder registryServiceBuilder = clientMappingRegistryService.build(serviceTarget, EJBRemoteConnectorService.EJB_REMOTE_CONNECTOR_CLIENT_MAPPINGS_REGISTRY_SERVICE, clientMappingCacheServiceName)
                .setInitialMode(ServiceController.Mode.PASSIVE);
        if (verificationHandler != null) {
            registryServiceBuilder.addListener(verificationHandler);
        }
        final ServiceController clientMappingRegistryServiceController = registryServiceBuilder.install();
        // add it to the services to be returned
        services.add(clientMappingRegistryServiceController);


        // Install the EJB remoting connector service which will listen for client connections on the remoting channel
        // TODO: Externalize (expose via management API if needed) the version and the marshalling strategy
        final EJBRemoteConnectorService ejbRemoteConnectorService = new EJBRemoteConnectorService((byte) 0x01, new String[]{"river"}, remotingServerServiceName);
        final ServiceBuilder<EJBRemoteConnectorService> ejbRemoteConnectorServiceBuilder = serviceTarget.addService(EJBRemoteConnectorService.SERVICE_NAME, ejbRemoteConnectorService);
        // add dependency on the Remoting subsytem endpoint
        ejbRemoteConnectorServiceBuilder.addDependency(RemotingServices.SUBSYSTEM_ENDPOINT, Endpoint.class, ejbRemoteConnectorService.getEndpointInjector());
        // add dependency on the remoting server (which allows remoting connector to connect to it)
        ejbRemoteConnectorServiceBuilder.addDependency(remotingServerServiceName);
        // add rest of the dependencies
        ejbRemoteConnectorServiceBuilder.addDependency(EJB3ThreadPoolAdd.BASE_SERVICE_NAME.append(threadPoolName), ExecutorService.class, ejbRemoteConnectorService.getExecutorService())
                .addDependency(DeploymentRepository.SERVICE_NAME, DeploymentRepository.class, ejbRemoteConnectorService.getDeploymentRepositoryInjector())
                .addDependency(EJBRemoteTransactionsRepository.SERVICE_NAME, EJBRemoteTransactionsRepository.class, ejbRemoteConnectorService.getEJBRemoteTransactionsRepositoryInjector())
                .addDependency(GroupMembershipNotifierRegistryService.SERVICE_NAME, GroupMembershipNotifierRegistry.class, ejbRemoteConnectorService.getClusterRegistryInjector())
                .addDependency(ServerEnvironmentService.SERVICE_NAME, ServerEnvironment.class, ejbRemoteConnectorService.getServerEnvironmentInjector())
                        // optional dependency on the client-mapping registry service (which is backed by a cache). The dependency will be
                        // available only in the presence of clustering subsystem
                        // TODO: It looks like (optional) dependency on a PASSIVE service (like the client mapping registry service) which
                        // in turn has a (optional) dependency (which can be unavailable) is broken. i.e. the service which depends on such
                        // a PASSIVE service is marked as DOWN with PROBLEM. So for now let's not add an explicit dependency
                        // on the client-mapping registry service and let's just look it up at runtime from the VersionOneProtocolChannelReceiver
                        // .addDependency(ServiceBuilder.DependencyType.OPTIONAL, EJBRemoteConnectorService.EJB_REMOTE_CONNECTOR_CLIENT_MAPPINGS_REGISTRY_SERVICE, Registry.class, ejbRemoteConnectorService.getClientMappingsRegistryServiceInjector())
                        // optional dependency on the backing cache of the client-mapping registry (available only in the presence of clustering
                        // subsystem)
                .addDependency(ServiceBuilder.DependencyType.OPTIONAL, clientMappingCacheServiceName, Cache.class, ejbRemoteConnectorService.getClientMappingsBackingCacheInjector())
                .setInitialMode(ServiceController.Mode.ACTIVE);
        if (verificationHandler != null) {
            ejbRemoteConnectorServiceBuilder.addListener(verificationHandler);
        }
        final ServiceController ejbRemotingConnectorServiceController = ejbRemoteConnectorServiceBuilder.install();
        // add it to the services to be returned
        services.add(ejbRemotingConnectorServiceController);

        return services;
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        model.get(CONNECTOR_REF).set(operation.require(CONNECTOR_REF).asString());
        model.get(THREAD_POOL_NAME).set(operation.require(THREAD_POOL_NAME).asString());
        if (operation.hasDefined(CLIENT_MAPPINGS_CACHE_CONTAINER_REF)) {
            model.get(CLIENT_MAPPINGS_CACHE_CONTAINER_REF).set(operation.get(CLIENT_MAPPINGS_CACHE_CONTAINER_REF));
        }
        if (operation.hasDefined(CLIENT_MAPPINGS_CACHE_REF)) {
            model.get(CLIENT_MAPPINGS_CACHE_REF).set(operation.get(CLIENT_MAPPINGS_CACHE_REF));
        }
    }

}

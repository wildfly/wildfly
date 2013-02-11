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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.UserTransaction;

import org.jboss.as.clustering.registry.RegistryCollector;
import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.ejb3.EjbMessages;
import org.jboss.as.ejb3.cache.impl.backing.clustering.ClusteredBackingCacheEntryStoreSourceService;
import org.jboss.as.ejb3.deployment.DeploymentRepository;
import org.jboss.as.ejb3.remote.EJBRemoteConnectorService;
import org.jboss.as.ejb3.remote.EJBRemoteTransactionsRepository;
import org.jboss.as.ejb3.remote.EJBRemotingConnectorClientMappingsEntryProviderService;
import org.jboss.as.ejb3.remote.RemoteAsyncInvocationCancelStatusService;
import org.jboss.as.remoting.RemotingServices;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.ServerEnvironmentService;
import org.jboss.as.txn.service.TransactionManagerService;
import org.jboss.as.txn.service.TransactionSynchronizationRegistryService;
import org.jboss.as.txn.service.UserTransactionService;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.remoting3.Endpoint;
import org.jboss.remoting3.RemotingOptions;
import org.xnio.Option;
import org.xnio.OptionMap;
import org.xnio.Options;


/**
 * A {@link AbstractBoottimeAddStepHandler} to handle the add operation for the EJB
 * remote service, in the EJB subsystem
 *
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class EJB3RemoteServiceAdd extends AbstractBoottimeAddStepHandler {
    static final EJB3RemoteServiceAdd INSTANCE = new EJB3RemoteServiceAdd();

    private EJB3RemoteServiceAdd() {
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

        // Service responsible for tracking cancel() invocations on remote async method calls
        final RemoteAsyncInvocationCancelStatusService asyncInvocationCancelStatusService = new RemoteAsyncInvocationCancelStatusService();
        final ServiceController asyncCancelTrackerServiceController = serviceTarget.addService(RemoteAsyncInvocationCancelStatusService.SERVICE_NAME, asyncInvocationCancelStatusService)
                .install();
        newControllers.add(asyncCancelTrackerServiceController);

    }

    Collection<ServiceController<?>> installRuntimeServices(final OperationContext context, final ModelNode model, final ServiceVerificationHandler verificationHandler) throws OperationFailedException {
        final String connectorName = EJB3RemoteResourceDefinition.CONNECTOR_REF.resolveModelAttribute(context, model).asString();
        final String threadPoolName = EJB3RemoteResourceDefinition.THREAD_POOL_NAME.resolveModelAttribute(context, model).asString();
        final ServiceName remotingServerServiceName = RemotingServices.serverServiceName(connectorName);

        final List<ServiceController<?>> services = new ArrayList<ServiceController<?>>();
        final ServiceTarget serviceTarget = context.getServiceTarget();

        // Install the client-mapping service for the remoting connector
        final EJBRemotingConnectorClientMappingsEntryProviderService clientMappingEntryProviderService = new EJBRemotingConnectorClientMappingsEntryProviderService(remotingServerServiceName);
        final ServiceBuilder clientMappingEntryProviderServiceBuilder = serviceTarget.addService(EJBRemotingConnectorClientMappingsEntryProviderService.SERVICE_NAME, clientMappingEntryProviderService)
                .addDependency(ServerEnvironmentService.SERVICE_NAME, ServerEnvironment.class, clientMappingEntryProviderService.getServerEnvironmentInjector())
                .addDependency(remotingServerServiceName);
        if (verificationHandler != null) {
            clientMappingEntryProviderServiceBuilder.addListener(verificationHandler);
        }
        final ServiceController clientMappingEntryProviderServiceController = clientMappingEntryProviderServiceBuilder.install();
        // add it to the services to be returned
        services.add(clientMappingEntryProviderServiceController);

        final OptionMap channelCreationOptions = this.getChannelCreationOptions(context);
        // Install the EJB remoting connector service which will listen for client connections on the remoting channel
        // TODO: Externalize (expose via management API if needed) the version and the marshalling strategy
        final EJBRemoteConnectorService ejbRemoteConnectorService = new EJBRemoteConnectorService((byte) 0x01, new String[]{"river"}, remotingServerServiceName, channelCreationOptions);
        final ServiceBuilder<EJBRemoteConnectorService> ejbRemoteConnectorServiceBuilder = serviceTarget.addService(EJBRemoteConnectorService.SERVICE_NAME, ejbRemoteConnectorService);
        // add dependency on the Remoting subsystem endpoint
        ejbRemoteConnectorServiceBuilder.addDependency(RemotingServices.SUBSYSTEM_ENDPOINT, Endpoint.class, ejbRemoteConnectorService.getEndpointInjector());
        // add dependency on the remoting server (which allows remoting connector to connect to it)
        ejbRemoteConnectorServiceBuilder.addDependency(remotingServerServiceName);
        // add rest of the dependencies
        ejbRemoteConnectorServiceBuilder.addDependency(EJB3SubsystemModel.BASE_THREAD_POOL_SERVICE_NAME.append(threadPoolName), ExecutorService.class, ejbRemoteConnectorService.getExecutorService())
                .addDependency(DeploymentRepository.SERVICE_NAME, DeploymentRepository.class, ejbRemoteConnectorService.getDeploymentRepositoryInjector())
                .addDependency(EJBRemoteTransactionsRepository.SERVICE_NAME, EJBRemoteTransactionsRepository.class, ejbRemoteConnectorService.getEJBRemoteTransactionsRepositoryInjector())
                .addDependency(ClusteredBackingCacheEntryStoreSourceService.CLIENT_MAPPING_REGISTRY_COLLECTOR_SERVICE_NAME, RegistryCollector.class, ejbRemoteConnectorService.getClusterRegistryCollectorInjector())
                .addDependency(ServerEnvironmentService.SERVICE_NAME, ServerEnvironment.class, ejbRemoteConnectorService.getServerEnvironmentInjector())
                .addDependency(TransactionManagerService.SERVICE_NAME, TransactionManager.class, ejbRemoteConnectorService.getTransactionManagerInjector())
                .addDependency(TransactionSynchronizationRegistryService.SERVICE_NAME, TransactionSynchronizationRegistry.class, ejbRemoteConnectorService.getTxSyncRegistryInjector())
                .addDependency(RemoteAsyncInvocationCancelStatusService.SERVICE_NAME, RemoteAsyncInvocationCancelStatusService.class, ejbRemoteConnectorService.getAsyncInvocationCancelStatusInjector())
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
        EJB3RemoteResourceDefinition.CONNECTOR_REF.validateAndSet(operation, model);
        EJB3RemoteResourceDefinition.THREAD_POOL_NAME.validateAndSet(operation, model);
    }

    private OptionMap getChannelCreationOptions(final OperationContext context) throws OperationFailedException {
        // read the full model of the current resource
        final ModelNode fullModel = Resource.Tools.readModel(context.readResource(PathAddress.EMPTY_ADDRESS));
        final ModelNode channelCreationOptions = fullModel.get(EJB3SubsystemModel.CHANNEL_CREATION_OPTIONS);
        if (channelCreationOptions.isDefined() && channelCreationOptions.asInt() > 0) {
            final ClassLoader loader = this.getClass().getClassLoader();
            final OptionMap.Builder builder = OptionMap.builder();
            for (final Property optionProperty : channelCreationOptions.asPropertyList()) {
                final String name = optionProperty.getName();
                final ModelNode propValueModel = optionProperty.getValue();
                final String type = ChannelCreationOptionResource.CHANNEL_CREATION_OPTION_TYPE.resolveModelAttribute(context,propValueModel).asString();
                final String optionClassName = this.getClassNameForChannelOptionType(type);
                final String fullyQualifiedOptionName = optionClassName + "." + name;
                final Option option = Option.fromString(fullyQualifiedOptionName, loader);
                final String value = ChannelCreationOptionResource.CHANNEL_CREATION_OPTION_VALUE.resolveModelAttribute(context, propValueModel).asString();
                builder.set(option, option.parseValue(value, loader));
            }
            return builder.getMap();
        } else {
            return OptionMap.EMPTY;
        }
    }

    private String getClassNameForChannelOptionType(final String optionType) {
        if ("remoting".equals(optionType)) {
            return RemotingOptions.class.getName();
        }
        if ("xnio".equals(optionType)) {
            return Options.class.getName();
        }
        throw EjbMessages.MESSAGES.unknownChannelCreationOptionType(optionType);
    }
}

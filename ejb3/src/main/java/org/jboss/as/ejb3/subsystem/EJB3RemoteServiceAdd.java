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

import java.util.ServiceLoader;
import java.util.concurrent.ExecutorService;

import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.UserTransaction;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.deployment.DeploymentRepository;
import org.jboss.as.ejb3.remote.EJBRemoteConnectorService;
import org.jboss.as.ejb3.remote.EJBRemoteTransactionsRepository;
import org.jboss.as.ejb3.remote.EJBRemotingConnectorClientMappingsEntryProviderService;
import org.jboss.as.ejb3.remote.RegistryCollector;
import org.jboss.as.ejb3.remote.RegistryCollectorService;
import org.jboss.as.ejb3.remote.RegistryInstallerService;
import org.jboss.as.ejb3.remote.RemoteAsyncInvocationCancelStatusService;
import org.jboss.as.remoting.RemotingConnectorBindingInfoService;
import org.jboss.as.remoting.RemotingServices;
import org.jboss.as.txn.service.TransactionManagerService;
import org.jboss.as.txn.service.TransactionSynchronizationRegistryService;
import org.jboss.as.txn.service.TxnServices;
import org.jboss.as.txn.service.UserTransactionService;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.remoting3.Endpoint;
import org.jboss.remoting3.RemotingOptions;
import org.wildfly.clustering.ejb.BeanManagerFactoryBuilderConfiguration;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.spi.CacheGroupBuilderProvider;
import org.wildfly.clustering.spi.CacheGroupServiceNameFactory;
import org.wildfly.clustering.spi.GroupBuilderProvider;
import org.wildfly.clustering.spi.LocalCacheGroupBuilderProvider;
import org.wildfly.clustering.spi.LocalGroupBuilderProvider;
import org.xnio.Option;
import org.xnio.OptionMap;
import org.xnio.Options;

import com.arjuna.ats.jbossatx.jta.RecoveryManagerService;


/**
 * A {@link AbstractAddStepHandler} to handle the add operation for the EJB
 * remote service, in the EJB subsystem
 *
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class EJB3RemoteServiceAdd extends AbstractAddStepHandler {
    static final EJB3RemoteServiceAdd INSTANCE = new EJB3RemoteServiceAdd();

    private EJB3RemoteServiceAdd() {
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        installRuntimeServices(context, model);

        // add ejb remote transactions repository service
        final EJBRemoteTransactionsRepository transactionsRepository = new EJBRemoteTransactionsRepository();
        final ServiceTarget serviceTarget = context.getServiceTarget();
        serviceTarget.addService(EJBRemoteTransactionsRepository.SERVICE_NAME, transactionsRepository)
                .addDependency(TransactionManagerService.SERVICE_NAME, TransactionManager.class, transactionsRepository.getTransactionManagerInjector())
                .addDependency(UserTransactionService.SERVICE_NAME, UserTransaction.class, transactionsRepository.getUserTransactionInjector())
                .addDependency(TxnServices.JBOSS_TXN_ARJUNA_RECOVERY_MANAGER, RecoveryManagerService.class, transactionsRepository.getRecoveryManagerInjector())
                .setInitialMode(ServiceController.Mode.ACTIVE)
                .install();

        // Service responsible for tracking cancel() invocations on remote async method calls
        final RemoteAsyncInvocationCancelStatusService asyncInvocationCancelStatusService = new RemoteAsyncInvocationCancelStatusService();
        serviceTarget.addService(RemoteAsyncInvocationCancelStatusService.SERVICE_NAME, asyncInvocationCancelStatusService).install();
    }

    void installRuntimeServices(final OperationContext context, final ModelNode model) throws OperationFailedException {
        final String connectorName = EJB3RemoteResourceDefinition.CONNECTOR_REF.resolveModelAttribute(context, model).asString();
        final String threadPoolName = EJB3RemoteResourceDefinition.THREAD_POOL_NAME.resolveModelAttribute(context, model).asString();
        final ServiceName remotingServerInfoServiceName = RemotingConnectorBindingInfoService.serviceName(connectorName);

        final ServiceTarget target = context.getServiceTarget();

        // Install the client-mapping service for the remoting connector
        new EJBRemotingConnectorClientMappingsEntryProviderService().build(target, remotingServerInfoServiceName)
                .setInitialMode(ServiceController.Mode.ON_DEMAND)
                .install();

        new RegistryInstallerService().build(target).setInitialMode(ServiceController.Mode.ON_DEMAND).install();

        // Handle case where no infinispan subsystem exists or does not define an ejb cache-container
        Resource rootResource = context.readResourceFromRoot(PathAddress.EMPTY_ADDRESS);
        PathElement infinispanPath = PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, "infinispan");
        if (!rootResource.hasChild(infinispanPath) || !rootResource.getChild(infinispanPath).hasChild(PathElement.pathElement("cache-container", BeanManagerFactoryBuilderConfiguration.DEFAULT_CONTAINER_NAME))) {
            // Install services that would normally be installed by this container/cache
            ModuleIdentifier module = Module.forClass(this.getClass()).getIdentifier();
            for (GroupBuilderProvider provider : ServiceLoader.load(LocalGroupBuilderProvider.class, LocalGroupBuilderProvider.class.getClassLoader())) {
                for (Builder<?> builder : provider.getBuilders(BeanManagerFactoryBuilderConfiguration.DEFAULT_CONTAINER_NAME, module)) {
                    builder.build(target).install();
                }
            }
            for (CacheGroupBuilderProvider provider : ServiceLoader.load(LocalCacheGroupBuilderProvider.class, LocalCacheGroupBuilderProvider.class.getClassLoader())) {
                for (Builder<?> builder : provider.getBuilders(BeanManagerFactoryBuilderConfiguration.DEFAULT_CONTAINER_NAME, CacheGroupServiceNameFactory.DEFAULT_CACHE)) {
                    builder.build(target).install();
                }
            }
        }

        final OptionMap channelCreationOptions = this.getChannelCreationOptions(context);
        // Install the EJB remoting connector service which will listen for client connections on the remoting channel
        // TODO: Externalize (expose via management API if needed) the version and the marshalling strategy
        final EJBRemoteConnectorService ejbRemoteConnectorService = new EJBRemoteConnectorService((byte) 0x02, new String[]{"river"}, channelCreationOptions);
        target.addService(EJBRemoteConnectorService.SERVICE_NAME, ejbRemoteConnectorService)
                // add dependency on the Remoting subsystem endpoint
                .addDependency(RemotingServices.SUBSYSTEM_ENDPOINT, Endpoint.class, ejbRemoteConnectorService.getEndpointInjector())
                // add dependency on the remoting server (which allows remoting connector to connect to it)
                // add rest of the dependencies
                .addDependency(EJB3SubsystemModel.BASE_THREAD_POOL_SERVICE_NAME.append(threadPoolName), ExecutorService.class, ejbRemoteConnectorService.getExecutorService())
                .addDependency(DeploymentRepository.SERVICE_NAME, DeploymentRepository.class, ejbRemoteConnectorService.getDeploymentRepositoryInjector())
                .addDependency(EJBRemoteTransactionsRepository.SERVICE_NAME, EJBRemoteTransactionsRepository.class, ejbRemoteConnectorService.getEJBRemoteTransactionsRepositoryInjector())
                .addDependency(RegistryCollectorService.SERVICE_NAME, RegistryCollector.class, ejbRemoteConnectorService.getClusterRegistryCollectorInjector())
                .addDependency(TransactionManagerService.SERVICE_NAME, TransactionManager.class, ejbRemoteConnectorService.getTransactionManagerInjector())
                .addDependency(TransactionSynchronizationRegistryService.SERVICE_NAME, TransactionSynchronizationRegistry.class, ejbRemoteConnectorService.getTxSyncRegistryInjector())
                .addDependency(RemoteAsyncInvocationCancelStatusService.SERVICE_NAME, RemoteAsyncInvocationCancelStatusService.class, ejbRemoteConnectorService.getAsyncInvocationCancelStatusInjector())
                .addDependency(remotingServerInfoServiceName, RemotingConnectorBindingInfoService.RemotingConnectorInfo.class, ejbRemoteConnectorService.getRemotingConnectorInfoInjectedValue())
                .setInitialMode(ServiceController.Mode.ACTIVE)
                .install();
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
                final String type = RemoteConnectorChannelCreationOptionResource.CHANNEL_CREATION_OPTION_TYPE.resolveModelAttribute(context,propValueModel).asString();
                final String optionClassName = this.getClassNameForChannelOptionType(type);
                final String fullyQualifiedOptionName = optionClassName + "." + name;
                final Option option = Option.fromString(fullyQualifiedOptionName, loader);
                final String value = RemoteConnectorChannelCreationOptionResource.CHANNEL_CREATION_OPTION_VALUE.resolveModelAttribute(context, propValueModel).asString();
                builder.set(option, option.parseValue(value, loader));
            }
            return builder.getMap();
        }
        return OptionMap.EMPTY;
    }

    private String getClassNameForChannelOptionType(final String optionType) {
        if ("remoting".equals(optionType)) {
            return RemotingOptions.class.getName();
        }
        if ("xnio".equals(optionType)) {
            return Options.class.getName();
        }
        throw EjbLogger.ROOT_LOGGER.unknownChannelCreationOptionType(optionType);
    }
}

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

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ExecutorService;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.remote.AssociationService;
import org.jboss.as.ejb3.remote.EJBRemoteConnectorService;
import org.jboss.as.ejb3.remote.EJBRemotingConnectorClientMappingsEntryProviderService;
import org.jboss.as.network.ClientMapping;
import org.jboss.as.remoting.RemotingServices;
import org.jboss.as.txn.service.TxnServices;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.remoting3.Endpoint;
import org.jboss.remoting3.RemotingOptions;
import org.wildfly.clustering.ejb.ClientMappingsRegistryProvider;
import org.wildfly.clustering.service.FunctionSupplierDependency;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SupplierDependency;
import org.wildfly.clustering.spi.CacheServiceConfiguratorProvider;
import org.wildfly.clustering.spi.GroupServiceConfiguratorProvider;
import org.wildfly.clustering.spi.LocalCacheServiceConfiguratorProvider;
import org.wildfly.clustering.spi.LocalGroupServiceConfiguratorProvider;
import org.wildfly.transaction.client.provider.remoting.RemotingTransactionService;
import org.xnio.Option;
import org.xnio.OptionMap;
import org.xnio.Options;

/**
 * A {@link AbstractAddStepHandler} to handle the add operation for the EJB
 * remote service, in the EJB subsystem
 *
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class EJB3RemoteServiceAdd extends AbstractBoottimeAddStepHandler {

    private final ClientMappingsRegistryProvider provider;

    EJB3RemoteServiceAdd(AttributeDefinition... attributes) {
        super(attributes);
        Iterator<ClientMappingsRegistryProvider> providers = ServiceLoader.load(ClientMappingsRegistryProvider.class, ClientMappingsRegistryProvider.class.getClassLoader()).iterator();
        this.provider = providers.hasNext() ? providers.next() : null;
    }

    /**
     * Override populateModel() to handle case of deprecated attribute connector-ref
     * - if connector-ref is present, use it to initialise connectors
     * - if connector-ref is not present and connectors is not present, throw an exception
     */
    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {

        if (operation.hasDefined(EJB3RemoteResourceDefinition.CONNECTOR_REF.getName())) {
            ModelNode connectorRef = operation.remove(EJB3RemoteResourceDefinition.CONNECTOR_REF.getName());
            operation.get(EJB3RemoteResourceDefinition.CONNECTORS.getName()).set(new ModelNode().add(connectorRef));
        }
        super.populateModel(operation, model);
    }

    @Override
    protected void performBoottime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        installRuntimeServices(context, model);
    }

    void installRuntimeServices(final OperationContext context, final ModelNode model) throws OperationFailedException {
        final String clientMappingsClusterName = EJB3RemoteResourceDefinition.CLIENT_MAPPINGS_CLUSTER_NAME.resolveModelAttribute(context, model).asString();
        final List<ModelNode> connectorNameNodes = EJB3RemoteResourceDefinition.CONNECTORS.resolveModelAttribute(context, model).asList();
        final String threadPoolName = EJB3RemoteResourceDefinition.THREAD_POOL_NAME.resolveModelAttribute(context, model).asString();
        final boolean executeInWorker = EJB3RemoteResourceDefinition.EXECUTE_IN_WORKER.resolveModelAttribute(context, model).asBoolean();

        final ServiceTarget target = context.getServiceTarget();

        // for each connector specified, we need to set up a client-mappings cache
        for (ModelNode connectorNameNode : connectorNameNodes) {
            String connectorName = connectorNameNode.asString();

            // Install the client-mapping service for the remoting connector
            ServiceConfigurator clientMappingsEntryProviderConfigurator = new EJBRemotingConnectorClientMappingsEntryProviderService(clientMappingsClusterName, connectorName).configure(context);
            clientMappingsEntryProviderConfigurator.build(target).setInitialMode(ServiceController.Mode.ON_DEMAND).install();

            if (this.provider != null) {
                // Install the registry for the remoting connector's client mappings
                SupplierDependency<Map.Entry<String, List<ClientMapping>>> registryEntryDependency = new ServiceSupplierDependency<>(clientMappingsEntryProviderConfigurator.getServiceName());
                for (CapabilityServiceConfigurator configurator : this.provider.getServiceConfigurators(clientMappingsClusterName, connectorName, new FunctionSupplierDependency<>(registryEntryDependency, Map.Entry::getValue))) {
                    configurator.configure(context).build(target).install();
                }
            }
        }

        // Handle case where no infinispan subsystem exists or does not define an ejb cache-container
        PathElement infinispanPath = PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, "infinispan");
        Resource infinispanResource = safeGetResource(context, infinispanPath);
        if (infinispanResource == null || !infinispanResource.hasChild(PathElement.pathElement("cache-container", clientMappingsClusterName))) {
            // Install services that would normally be installed by this container/cache
            CapabilityServiceSupport support = context.getCapabilityServiceSupport();
            for (GroupServiceConfiguratorProvider provider : ServiceLoader.load(LocalGroupServiceConfiguratorProvider.class, LocalGroupServiceConfiguratorProvider.class.getClassLoader())) {
                for (CapabilityServiceConfigurator configurator : provider.getServiceConfigurators(requirement -> requirement.getServiceName(support, clientMappingsClusterName), clientMappingsClusterName)) {
                    configurator.configure(support).build(target).install();
                }
            }
            for (CacheServiceConfiguratorProvider provider : ServiceLoader.load(LocalCacheServiceConfiguratorProvider.class, LocalCacheServiceConfiguratorProvider.class.getClassLoader())) {
                for (CapabilityServiceConfigurator configurator : provider.getServiceConfigurators(requirement -> requirement.getServiceName(support, clientMappingsClusterName, null), clientMappingsClusterName, null)) {
                    configurator.configure(support).build(target).install();
                }
            }
        }

        final OptionMap channelCreationOptions = this.getChannelCreationOptions(context);
        // Install the EJB remoting connector service which will listen for client connections on the remoting channel
        // TODO: Externalize (expose via management API if needed) the version and the marshalling strategy
        final EJBRemoteConnectorService ejbRemoteConnectorService = new EJBRemoteConnectorService(channelCreationOptions,
                FilterSpecClassResolverFilter.getFilterForOperationContext(context));
        ServiceBuilder<?> builder = context.getCapabilityServiceTarget().addCapability(EJB3RemoteResourceDefinition.EJB_REMOTE_CAPABILITY)
                .setInstance(ejbRemoteConnectorService)
                .addAliases(EJBRemoteConnectorService.SERVICE_NAME)
                // add dependency on the Remoting subsystem endpoint
                .addDependency(RemotingServices.SUBSYSTEM_ENDPOINT, Endpoint.class, ejbRemoteConnectorService.getEndpointInjector())
                // add rest of the dependencies
                .addDependency(AssociationService.SERVICE_NAME, AssociationService.class, ejbRemoteConnectorService.getAssociationServiceInjector())
                .addDependency(TxnServices.JBOSS_TXN_REMOTE_TRANSACTION_SERVICE, RemotingTransactionService.class, ejbRemoteConnectorService.getRemotingTransactionServiceInjector())
                .setInitialMode(ServiceController.Mode.LAZY);
        if (!executeInWorker) {
            builder.addDependency(EJB3SubsystemModel.BASE_THREAD_POOL_SERVICE_NAME.append(threadPoolName), ExecutorService.class, ejbRemoteConnectorService.getExecutorService());
        }
        builder.install();
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

    private static Resource safeGetResource(OperationContext context, PathElement path) {
        try {
            return context.readResourceFromRoot(PathAddress.pathAddress(path), false);
        } catch (RuntimeException e) {
            // No such resource
            return null;
        }
    }
}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.subsystem;

import static org.jboss.as.ejb3.subsystem.EJB3RemoteResourceDefinition.EJB_REMOTE_CAPABILITY_NAME;

import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.CapabilityServiceBuilder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceNameFactory;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.remote.AssociationService;
import org.jboss.as.ejb3.remote.EJBRemoteConnectorService;
import org.jboss.as.ejb3.remote.EJBRemotingConnectorClientMappingsServiceConfigurator;
import org.jboss.as.network.ClientMapping;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.remoting3.Endpoint;
import org.jboss.remoting3.RemotingOptions;
import org.wildfly.clustering.ejb.remote.ClientMappingsRegistryProvider;
import org.wildfly.clustering.ejb.remote.RemoteEjbRequirement;
import org.wildfly.clustering.infinispan.service.InfinispanRequirement;
import org.wildfly.clustering.server.service.ClusteringCacheRequirement;
import org.wildfly.clustering.ejb.remote.LegacyClientMappingsRegistryProviderFactory;
import org.wildfly.clustering.service.ChildTargetService;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SimpleSupplierDependency;
import org.wildfly.clustering.service.SupplierDependency;
import org.wildfly.service.ServiceDependency;
import org.wildfly.service.ServiceInstaller;
import org.wildfly.transaction.client.provider.remoting.RemotingTransactionService;
import org.xnio.Option;
import org.xnio.OptionMap;
import org.xnio.Options;

/**
 * A {@link AbstractAddStepHandler} to handle the add operation for the Jakarta Enterprise Beans
 * remote service, in the Jakarta Enterprise Beans subsystem
 *
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @author <a href="mailto:rachmato@redhat.com">Richard Achmatowicz</a>
 */
public class EJB3RemoteServiceAdd extends AbstractBoottimeAddStepHandler {

    private static final LegacyClientMappingsRegistryProviderFactory LEGACY_PROVIDER_FACTORY = ServiceLoader.load(LegacyClientMappingsRegistryProviderFactory.class, LegacyClientMappingsRegistryProviderFactory.class.getClassLoader()).findFirst().orElse(null);

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

        final List<ModelNode> connectorNameNodes = EJB3RemoteResourceDefinition.CONNECTORS.resolveModelAttribute(context, model).asList();
        final String threadPoolName = EJB3RemoteResourceDefinition.THREAD_POOL_NAME.resolveModelAttribute(context, model).asString();
        final boolean executeInWorker = EJB3RemoteResourceDefinition.EXECUTE_IN_WORKER.resolveModelAttribute(context, model).asBoolean();

        final ServiceTarget target = context.getServiceTarget();
        final CapabilityServiceSupport support = context.getCapabilityServiceSupport();

        // final ClientMappingsRegistryProvider provider = providerFactory.createClientMappingsRegistryProvider(clientMappingsClusterName);
        final SupplierDependency<ClientMappingsRegistryProvider> provider = getClientMappingsRegistryProvider(context, model);

        // for each connector specified, we need to set up a client-mappings cache
        for (ModelNode connectorNameNode : connectorNameNodes) {
            String connectorName = connectorNameNode.asString();

            // Install the client-mappings entry provider service for the remoting connector
            ServiceConfigurator clientMappingsConfigurator = new EJBRemotingConnectorClientMappingsServiceConfigurator(connectorName).configure(context);
            clientMappingsConfigurator.build(target).setInitialMode(ServiceController.Mode.ON_DEMAND).install();

            SupplierDependency<List<ClientMapping>> clientMappings = new ServiceSupplierDependency<>(clientMappingsConfigurator.getServiceName());
            // use a ChildTargetService so that the provider may be resolved
            Consumer<ServiceTarget> installer = new Consumer<ServiceTarget>() {
                @Override
                public void accept(ServiceTarget serviceTarget) {
                    for (CapabilityServiceConfigurator configurator : provider.get().getServiceConfigurators(connectorName, clientMappings)) {
                        ServiceController<?> controller = configurator.configure(support).build(target).install();
                        ServiceName registryParentName = ServiceNameFactory.parseServiceName(ClusteringCacheRequirement.REGISTRY.getName());
                        for (ServiceName providedName : controller.provides()) {
                            if (registryParentName.isParentOf(providedName)) {
                                ServiceInstaller.builder(ServiceDependency.on(providedName)).provides(ServiceNameFactory.resolveServiceName(EJB3RemoteResourceDefinition.CLIENT_MAPPINGS_REGISTRY, connectorName)).build().install(target);
                            }
                        }
                    }
                }
            };
            ServiceName name = ServiceName.JBOSS.append("ejb", "remote", "client-mappings-registry", "installer", "connector", connectorName);
            provider.register(target.addService(name)).setInstance(new ChildTargetService(installer)).install();
        }

        final OptionMap channelCreationOptions = this.getChannelCreationOptions(context);
        // Install the Jakarta Enterprise Beans remoting connector service which will listen for client connections on the remoting channel
        // TODO: Externalize (expose via management API if needed) the version and the marshalling strategy
        final CapabilityServiceBuilder<?> builder = context.getCapabilityServiceTarget().addCapability(EJB3RemoteResourceDefinition.EJB_REMOTE_CAPABILITY);
        final Consumer<EJBRemoteConnectorService> serviceConsumer = builder.provides(EJB3RemoteResourceDefinition.EJB_REMOTE_CAPABILITY);
        final Supplier<Endpoint> endpointSupplier = builder.requiresCapability(EJB3RemoteResourceDefinition.REMOTING_ENDPOINT_CAPABILITY_NAME, Endpoint.class);
        Supplier<ExecutorService> executorServiceSupplier = null;
        if (!executeInWorker) {
            executorServiceSupplier = builder.requiresCapability(EJB3RemoteResourceDefinition.THREAD_POOL_CAPABILITY_NAME, ExecutorService.class, threadPoolName);
        }
        // add rest of the dependencies
        final Supplier<AssociationService> associationServiceSupplier = builder.requires(AssociationService.SERVICE_NAME);
        final Supplier<RemotingTransactionService> remotingTransactionServiceSupplier = builder.requiresCapability(EJB3RemoteResourceDefinition.REMOTE_TRANSACTION_SERVICE_CAPABILITY_NAME, RemotingTransactionService.class);
        builder.addAliases(EJBRemoteConnectorService.SERVICE_NAME).setInitialMode(ServiceController.Mode.LAZY);
        final EJBRemoteConnectorService ejbRemoteConnectorService = new EJBRemoteConnectorService(serviceConsumer, endpointSupplier, executorServiceSupplier, associationServiceSupplier, remotingTransactionServiceSupplier, channelCreationOptions,
                FilterSpecClassResolverFilter.getFilterForOperationContext(context));
        builder.setInstance(ejbRemoteConnectorService);
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
                final String optionClassName = getClassNameForChannelOptionType(type);
                final String fullyQualifiedOptionName = optionClassName + "." + name;
                final Option option = Option.fromString(fullyQualifiedOptionName, loader);
                final String value = RemoteConnectorChannelCreationOptionResource.CHANNEL_CREATION_OPTION_VALUE.resolveModelAttribute(context, propValueModel).asString();
                builder.set(option, option.parseValue(value, loader));
            }
            return builder.getMap();
        }
        return OptionMap.EMPTY;
    }

    private static String getClassNameForChannelOptionType(final String optionType) {
        if ("remoting".equals(optionType)) {
            return RemotingOptions.class.getName();
        }
        if ("xnio".equals(optionType)) {
            return Options.class.getName();
        }
        throw EjbLogger.ROOT_LOGGER.unknownChannelCreationOptionType(optionType);
    }

    /*
     * Return a client mappings registry provider, used to provide base clustering abstractions for the client mappings registries.
     * The preference for obtaining the provider is:
     * - use a client mappings registry provider defined in the distributable-ejb subsystem and installed as a service
     * - otherwise, use the legacy provider loaded from the classpath
     */
    private SupplierDependency<ClientMappingsRegistryProvider> getClientMappingsRegistryProvider(OperationContext context, ModelNode model) throws OperationFailedException {
        if (context.hasOptionalCapability(RemoteEjbRequirement.CLIENT_MAPPINGS_REGISTRY_PROVIDER.getName(), EJB_REMOTE_CAPABILITY_NAME, null)) {
            return new ServiceSupplierDependency<>(RemoteEjbRequirement.CLIENT_MAPPINGS_REGISTRY_PROVIDER.getServiceName(context));
        }
        String clusterName = EJB3RemoteResourceDefinition.CLIENT_MAPPINGS_CLUSTER_NAME.resolveModelAttribute(context, model).asString();
        context.requireOptionalCapability(InfinispanRequirement.CONTAINER.resolve(clusterName), EJB_REMOTE_CAPABILITY_NAME, EJB3RemoteResourceDefinition.CLIENT_MAPPINGS_CLUSTER_NAME.getName());
        EjbLogger.ROOT_LOGGER.legacyClientMappingsRegistryProviderInUse(clusterName);
        return new SimpleSupplierDependency<>(LEGACY_PROVIDER_FACTORY.createClientMappingsRegistryProvider(clusterName));
    }
}

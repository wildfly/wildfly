/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.subsystem;

import static org.jboss.as.ejb3.subsystem.EJB3RemoteResourceDefinition.CONNECTOR_CAPABILITY_NAME;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.CapabilityServiceBuilder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.RequirementServiceTarget;
import org.jboss.as.controller.ServiceNameFactory;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.remote.AssociationService;
import org.jboss.as.ejb3.remote.EJBRemoteConnectorService;
import org.jboss.as.ejb3.remote.ModuleAvailabilityRegistrarServiceInstaller;
import org.jboss.as.network.ClientMapping;
import org.jboss.as.network.ProtocolSocketBinding;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.remoting3.Endpoint;
import org.jboss.remoting3.RemotingOptions;
import org.wildfly.clustering.ejb.remote.ClientMappingsRegistryProvider;
import org.wildfly.clustering.ejb.remote.LegacyClientMappingsRegistryProviderFactory;
import org.wildfly.clustering.ejb.remote.LegacyModuleAvailabilityRegistrarProviderFactory;
import org.wildfly.clustering.ejb.remote.ModuleAvailabilityRegistrarProvider;
import org.wildfly.clustering.infinispan.service.InfinispanServiceDescriptor;
import org.wildfly.clustering.server.service.ClusteringServiceDescriptor;
import org.wildfly.common.function.Functions;
import org.wildfly.common.net.Inet;
import org.wildfly.service.descriptor.UnaryServiceDescriptor;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;
import org.wildfly.transaction.client.provider.remoting.RemotingTransactionService;
import org.xnio.Option;
import org.xnio.OptionMap;
import org.xnio.Options;

/**
 * A {@link AbstractAddStepHandler} to handle the add operation for the Jakarta Enterprise Beans remote service, in the Jakarta
 * Enterprise Beans subsystem
 *
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @author <a href="mailto:rachmato@redhat.com">Richard Achmatowicz</a>
 */
public class EJB3RemoteServiceAdd extends AbstractBoottimeAddStepHandler {

    // legacy provider factory for ClientMappingsRegistry
    private static final LegacyClientMappingsRegistryProviderFactory LEGACY_CLIENT_MAPPINGS_REGISTRY_PROVIDER_FACTORY = ServiceLoader
            .load(LegacyClientMappingsRegistryProviderFactory.class, LegacyClientMappingsRegistryProviderFactory.class.getClassLoader())
            .findFirst().orElse(null);
    // legacy provider factory for ModuleAvailabilityRegistrar
    private static final LegacyModuleAvailabilityRegistrarProviderFactory LEGACY_MODULE_AVAILABILITY_REGISTRAR_PROVIDER_FACTORY = ServiceLoader
            .load(LegacyModuleAvailabilityRegistrarProviderFactory.class, LegacyModuleAvailabilityRegistrarProviderFactory.class.getClassLoader())
            .findFirst().orElse(null);

    @SuppressWarnings("unchecked")
    private static final UnaryServiceDescriptor<List<ClientMapping>> CLIENT_MAPPINGS = UnaryServiceDescriptor
            .of("org.wildfly.ejb.remote.client-mappings", (Class<List<ClientMapping>>) (Class<?>) List.class);

    /**
     * Override populateModel() to handle case of deprecated attribute connector-ref - if connector-ref is present, use it to
     * initialise connectors - if connector-ref is not present and connectors is not present, throw an exception
     */
    @Override
    protected void populateModel(OperationContext context, ModelNode operation, Resource resource)
            throws OperationFailedException {

        if (operation.hasDefined(EJB3RemoteResourceDefinition.CONNECTOR_REF.getName())) {
            ModelNode connectorRef = operation.remove(EJB3RemoteResourceDefinition.CONNECTOR_REF.getName());
            operation.get(EJB3RemoteResourceDefinition.CONNECTORS.getName()).set(new ModelNode().add(connectorRef));
        }
        super.populateModel(context, operation, resource);
    }

    @Override
    protected void performBoottime(OperationContext context, ModelNode operation, ModelNode model)
            throws OperationFailedException {
        installRuntimeServices(context, model);
    }

    void installRuntimeServices(final OperationContext context, final ModelNode model) throws OperationFailedException {

        final List<ModelNode> connectorNameNodes = EJB3RemoteResourceDefinition.CONNECTORS.resolveModelAttribute(context, model)
                .asList();
        final String threadPoolName = EJB3RemoteResourceDefinition.THREAD_POOL_NAME.resolveModelAttribute(context, model)
                .asString();
        final boolean executeInWorker = EJB3RemoteResourceDefinition.EXECUTE_IN_WORKER.resolveModelAttribute(context, model)
                .asBoolean();

        CapabilityServiceSupport support = context.getCapabilityServiceSupport();

        // for each connector specified, we need to set up a client-mappings cache
        for (ModelNode connectorNameNode : connectorNameNodes) {
            String connectorName = connectorNameNode.asString();

            ServiceDependency<ProtocolSocketBinding> remotingConnectorInfo = ServiceDependency.on(CONNECTOR_CAPABILITY_NAME,
                    ProtocolSocketBinding.class, connectorName);
            ServiceInstaller.builder(EJB3RemoteServiceAdd::getClientMappings, remotingConnectorInfo)
                    .provides(ServiceNameFactory.resolveServiceName(CLIENT_MAPPINGS, connectorName))
                    .requires(remotingConnectorInfo).build().install(context);

            ServiceDependency<ClientMappingsRegistryProvider> cmrProvider = getClientMappingsRegistryProvider(context, model);
            ServiceInstaller installer = new ServiceInstaller() {
                @Override
                public ServiceController<?> install(RequirementServiceTarget target) {
                    for (ServiceInstaller installer : cmrProvider.get().getServiceInstallers(support, connectorName,
                            ServiceDependency.on(CLIENT_MAPPINGS, connectorName))) {
                        ServiceController<?> controller = installer.install(target);
                        // set up an alias for each provided name of the installed service
                        ServiceName registryParentName = ServiceNameFactory.parseServiceName(ClusteringServiceDescriptor.REGISTRY.getName());
                        for (ServiceName providedName : controller.provides()) {
                            if (registryParentName.isParentOf(providedName)) {
                                ServiceInstaller.builder(ServiceDependency.on(providedName))
                                        .provides(ServiceNameFactory.resolveServiceName(
                                                EJB3RemoteResourceDefinition.CLIENT_MAPPINGS_REGISTRY, connectorName))
                                        .build().install(target);
                            }
                        }
                    }
                    return null;
                }
            };
            ServiceInstaller.builder(installer, support).requires(cmrProvider).build().install(context);
        }

        // set up a ServiceProviderRegistrar to support cluster-wide mdule availability updates via a ModuleAvailabilityRegsitrar
        ServiceDependency<ModuleAvailabilityRegistrarProvider> marProvider = getModuleAvailabilityRegistrarProvider(context, model);
        ServiceInstaller installer = new ServiceInstaller() {
            @Override
            public ServiceController<?> install(RequirementServiceTarget target) {
                for (ServiceInstaller installer : marProvider.get().getServiceInstallers(support)) {
                    System.out.println("Installing service: " + installer.getClass().getName());
                    ServiceController<?> controller = installer.install(target);
                    // set up an alias for each provided name of the installed service
                    ServiceName registryParentName = ServiceNameFactory.parseServiceName(ClusteringServiceDescriptor.SERVICE_PROVIDER_REGISTRAR.getName());
                    for (ServiceName providedName : controller.provides()) {
                        if (registryParentName.isParentOf(providedName)) {
                            ServiceInstaller.builder(ServiceDependency.on(providedName))
                                    .provides(ServiceNameFactory.resolveServiceName(EJB3RemoteResourceDefinition.MODULE_AVAILABILITY_REGISTRAR_SERVICE_PROVIDER_REGISTRAR))
                                    .build().install(target);
                        }
                    }
                }
                return null;
            }
        };
        System.out.println("EJB3RemoteServiceAdd: installing ServiceProviderRegistrar");
        ServiceInstaller.builder(installer, support).requires(marProvider).build().install(context);

        // install the ModuleAvailabilityRegistrar service
        new ModuleAvailabilityRegistrarServiceInstaller().install(context);

        final OptionMap channelCreationOptions = this.getChannelCreationOptions(context);
        // Install the Jakarta Enterprise Beans remoting connector service which will listen for client connections on the
        // remoting channel
        // TODO: Externalize (expose via management API if needed) the version and the marshalling strategy
        final CapabilityServiceBuilder<?> builder = context.getCapabilityServiceTarget()
                .addCapability(EJB3RemoteResourceDefinition.EJB_REMOTE_CAPABILITY);
        final Consumer<EJBRemoteConnectorService> serviceConsumer = builder
                .provides(EJB3RemoteResourceDefinition.EJB_REMOTE_CAPABILITY);
        final Supplier<Endpoint> endpointSupplier = builder
                .requiresCapability(EJB3RemoteResourceDefinition.REMOTING_ENDPOINT_CAPABILITY_NAME, Endpoint.class);
        Supplier<Executor> executorSupplier = !executeInWorker ? builder.requires(EJB3SubsystemRootResourceDefinition.EXECUTOR_SERVICE_DESCRIPTOR, threadPoolName) : Functions.constantSupplier(null);
        // add rest of the dependencies
        final Supplier<AssociationService> associationServiceSupplier = builder.requires(AssociationService.SERVICE_NAME);
        final Supplier<RemotingTransactionService> remotingTransactionServiceSupplier = builder.requiresCapability(
                EJB3RemoteResourceDefinition.REMOTE_TRANSACTION_SERVICE_CAPABILITY_NAME, RemotingTransactionService.class);
        builder.addAliases(EJBRemoteConnectorService.SERVICE_NAME).setInitialMode(ServiceController.Mode.LAZY);
        final EJBRemoteConnectorService ejbRemoteConnectorService = new EJBRemoteConnectorService(serviceConsumer,
                endpointSupplier, executorSupplier, associationServiceSupplier, remotingTransactionServiceSupplier,
                channelCreationOptions, FilterSpecClassResolverFilter.getFilterForOperationContext(context));
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
                final String type = RemoteConnectorChannelCreationOptionResource.CHANNEL_CREATION_OPTION_TYPE
                        .resolveModelAttribute(context, propValueModel).asString();
                final String optionClassName = getClassNameForChannelOptionType(type);
                final String fullyQualifiedOptionName = optionClassName + "." + name;
                final Option option = Option.fromString(fullyQualifiedOptionName, loader);
                final String value = RemoteConnectorChannelCreationOptionResource.CHANNEL_CREATION_OPTION_VALUE
                        .resolveModelAttribute(context, propValueModel).asString();
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
     * Return a client mappings registry provider, used to provide base clustering abstractions for the client mappings
     * registries. The preference for obtaining the provider is: - use a provider defined in the distributable-ejb subsystem
     * and installed as a service - otherwise, use the legacy provider loaded from the classpath.
     */
    private static ServiceDependency<ClientMappingsRegistryProvider> getClientMappingsRegistryProvider(OperationContext context,
                                                                                                       ModelNode model) throws OperationFailedException {
        if (context.hasOptionalCapability(ClientMappingsRegistryProvider.SERVICE_DESCRIPTOR,
                EJB3RemoteResourceDefinition.EJB_REMOTE_CAPABILITY, null)) {
            return ServiceDependency.on(ClientMappingsRegistryProvider.SERVICE_DESCRIPTOR);
        }
        String clusterName = EJB3RemoteResourceDefinition.CLIENT_MAPPINGS_CLUSTER_NAME.resolveModelAttribute(context, model)
                .asString();
        context.requireOptionalCapability(
                RuntimeCapability.resolveCapabilityName(InfinispanServiceDescriptor.CACHE_CONTAINER, clusterName),
                EJB3RemoteResourceDefinition.EJB_REMOTE_CAPABILITY_NAME,
                EJB3RemoteResourceDefinition.CLIENT_MAPPINGS_CLUSTER_NAME.getName());
        EjbLogger.ROOT_LOGGER.legacyClientMappingsRegistryProviderInUse(clusterName);
        return ServiceDependency.of(LEGACY_CLIENT_MAPPINGS_REGISTRY_PROVIDER_FACTORY.createClientMappingsRegistryProvider(clusterName));
    }

    /*
     * Return a module availability registrar provider, used to provide base clustering abstractions for the module availability
     * registrier. The preference for obtaining the provider is: - use a provider defined in the distributable-ejb subsystem and
     * installed as a service - otherwise, use the legacy provider loaded from the classpath.
     */
    private static ServiceDependency<ModuleAvailabilityRegistrarProvider> getModuleAvailabilityRegistrarProvider(OperationContext context,
                                                                                                                 ModelNode model) throws OperationFailedException {
        if (context.hasOptionalCapability(ModuleAvailabilityRegistrarProvider.SERVICE_DESCRIPTOR,
                EJB3RemoteResourceDefinition.EJB_REMOTE_CAPABILITY, null)) {
            return ServiceDependency.on(ModuleAvailabilityRegistrarProvider.SERVICE_DESCRIPTOR);
        }
        String clusterName = EJB3RemoteResourceDefinition.CLIENT_MAPPINGS_CLUSTER_NAME.resolveModelAttribute(context, model)
                .asString();
        context.requireOptionalCapability(
                RuntimeCapability.resolveCapabilityName(InfinispanServiceDescriptor.CACHE_CONTAINER, clusterName),
                EJB3RemoteResourceDefinition.EJB_REMOTE_CAPABILITY_NAME,
                EJB3RemoteResourceDefinition.CLIENT_MAPPINGS_CLUSTER_NAME.getName());
        EjbLogger.ROOT_LOGGER.legacyModuleAvailabilityRegistrarProviderInUse(clusterName);
        return ServiceDependency.of(LEGACY_MODULE_AVAILABILITY_REGISTRAR_PROVIDER_FACTORY.createModuleAvailabilityRegistrarProvider(clusterName));
    }


    /**
     * This method provides client-mapping entries for all connected Jakarta Enterprise Beans clients. It returns either a set
     * of user-defined client mappings for a multi-homed host or a single default client mapping for the single-homed host.
     * Hostnames are preferred over literal IP addresses for the destination address part (due to EJBCLIENT-349).
     *
     * @return the client mappings for this host
     */
    static List<ClientMapping> getClientMappings(ProtocolSocketBinding info) {
        List<ClientMapping> clientMappings = info.getSocketBinding().getClientMappings();

        if (!clientMappings.isEmpty()) {
            return clientMappings;
        }
        // for the destination, prefer the hostname over the literal IP address
        final InetAddress destination = info.getSocketBinding().getAddress();
        final String destinationName = Inet.toURLString(destination, true);

        // for the network, send a CIDR that is compatible with the address we are sending
        final InetAddress clientNetworkAddress;
        try {
            if (destination instanceof Inet4Address) {
                clientNetworkAddress = InetAddress.getByName("0.0.0.0");
            } else {
                clientNetworkAddress = InetAddress.getByName("::");
            }
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        return List.of(new ClientMapping(clientNetworkAddress, 0, destinationName, info.getSocketBinding().getAbsolutePort()));
    }
}

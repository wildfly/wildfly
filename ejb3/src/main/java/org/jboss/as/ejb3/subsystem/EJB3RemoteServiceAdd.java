/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.subsystem;

import static org.jboss.as.ejb3.subsystem.EJB3RemoteResourceDefinition.CONNECTOR_CAPABILITY_NAME;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Supplier;

import io.undertow.server.handlers.PathHandler;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.CapabilityServiceBuilder;
import org.jboss.as.controller.CapabilityServiceTarget;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.RequirementServiceTarget;
import org.jboss.as.controller.ServiceNameFactory;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.ejb3.deployment.DeploymentRepository;
import org.jboss.as.ejb3.deployment.DeploymentRepositoryService;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.remote.AssociationService;
import org.jboss.as.ejb3.remote.DeploymentsAssociationService;
import org.jboss.as.ejb3.remote.EJBRemoteConnectorService;
import org.jboss.as.ejb3.remote.http.EJB3RemoteHTTPService;
import org.jboss.as.network.ClientMapping;
import org.jboss.as.network.ProtocolSocketBinding;
import org.jboss.as.txn.service.TxnServices;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.remoting3.Endpoint;
import org.jboss.remoting3.RemotingOptions;
import org.wildfly.clustering.ejb.remote.ClientMappingsRegistryProvider;
import org.wildfly.clustering.ejb.remote.LegacyClientMappingsRegistryProviderFactory;
import org.wildfly.clustering.infinispan.service.InfinispanServiceDescriptor;
import org.wildfly.clustering.server.registry.Registry;
import org.wildfly.clustering.server.service.ClusteringServiceDescriptor;
import org.wildfly.common.function.Functions;
import org.wildfly.common.net.Inet;
import org.wildfly.service.descriptor.UnaryServiceDescriptor;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;
import org.wildfly.transaction.client.LocalTransactionContext;
import org.wildfly.transaction.client.provider.remoting.RemotingTransactionService;
import org.xnio.Option;
import org.xnio.OptionMap;
import org.xnio.Options;

/**
 * A {@link AbstractAddStepHandler} to handle the add operation for the Jakarta Enterprise Beans remote resource, in
 * the Jakarta Enterprise Beans subsystem.
 *
 * This resource installs all services required for handling incoming remote EJB invocations.
 *
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @author <a href="mailto:rachmato@redhat.com">Richard Achmatowicz</a>
 */
public class EJB3RemoteServiceAdd extends AbstractBoottimeAddStepHandler {

    private static final String UNDERTOW_HTTP_INVOKER_CAPABILITY_NAME = "org.wildfly.undertow.http-invoker";

    private static final LegacyClientMappingsRegistryProviderFactory LEGACY_PROVIDER_FACTORY = ServiceLoader
            .load(LegacyClientMappingsRegistryProviderFactory.class,
                    LegacyClientMappingsRegistryProviderFactory.class.getClassLoader())
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

    /**
     * Installs all services required for processing incoming remote EJB invocations, both for the Remoting
     * and HTTP protocols.
     * EJB client invocations using the http protocol can be one of two types:
     * - an invocation which will use HTTP upgrade to convert the initial HTTP connection to use the Remoting protocol
     * and use the standard EJB client library to process the invocation
     * - an invocation which will use the Undertow HTTP invoker mechanism to process the invocation using the Wildfly
     * HTTP client library, a "pure" HTTP model of processing
     *
     * @param context the OperationContext
     * @param model the "remote" resource model used to configure the services
     * @throws OperationFailedException
     */
    void installRuntimeServices(final OperationContext context, final ModelNode model) throws OperationFailedException {

        final List<ModelNode> connectorNameNodes = EJB3RemoteResourceDefinition.CONNECTORS.resolveModelAttribute(context, model)
                .asList();
        final String threadPoolName = EJB3RemoteResourceDefinition.THREAD_POOL_NAME.resolveModelAttribute(context, model)
                .asString();
        final boolean executeInWorker = EJB3RemoteResourceDefinition.EXECUTE_IN_WORKER.resolveModelAttribute(context, model)
                .asBoolean();

        // use capabilities to install services
        final CapabilityServiceTarget serviceTarget = context.getCapabilityServiceTarget();

        // Install the (Delegating)AssociationService to provide a deployment-aware AssociationService
        final CapabilityServiceBuilder<?> associationServiceBuilder = serviceTarget.addService();
        final Consumer<AssociationService> associationServiceConsumer = associationServiceBuilder.provides(AssociationService.SERVICE_NAME);

        final AssociationService associationService = new AssociationService(associationServiceConsumer);
        associationServiceBuilder.setInstance(associationService);
        associationServiceBuilder.setInitialMode(ServiceController.Mode.LAZY);
        associationServiceBuilder.install();

        // Install the DeploymentsAssociationService used for processing incoming remote invocations when deployments are available
        final CapabilityServiceBuilder<?> deploymentsAssociationServiceBuilder = serviceTarget.addService();
        final Consumer<DeploymentsAssociationService> deploymentsAssociationServiceConsumer = deploymentsAssociationServiceBuilder.provides(DeploymentsAssociationService.SERVICE_NAME);
        final Supplier<AssociationService> associationServiceSupplier1 = deploymentsAssociationServiceBuilder.requires(AssociationService.SERVICE_NAME);
        final Supplier<DeploymentRepository> deploymentRepositorySupplier = deploymentsAssociationServiceBuilder.requires(DeploymentRepositoryService.SERVICE_NAME);

        // now, for each connector, configure the relevant AssociationService dependencies
        final List<Map.Entry<Supplier<ProtocolSocketBinding>, Supplier<Registry>>> protocolRegistryPairs = new ArrayList<>();
        for (ModelNode connector : connectorNameNodes) {
            String connectorName = connector.asString();

            final Supplier<ProtocolSocketBinding> protocol = deploymentsAssociationServiceBuilder.requires(context.getCapabilityServiceName(CONNECTOR_CAPABILITY_NAME, connectorName, ProtocolSocketBinding.class));
            final Supplier<Registry> registry = deploymentsAssociationServiceBuilder.requires(ServiceNameFactory.resolveServiceName(EJB3RemoteResourceDefinition.CLIENT_MAPPINGS_REGISTRY, connectorName));
            protocolRegistryPairs.add(new AbstractMap.SimpleImmutableEntry<>(protocol, registry));
        }

        final DeploymentsAssociationService deploymentsAssociationService = new DeploymentsAssociationService(deploymentsAssociationServiceConsumer,
                associationServiceSupplier1, deploymentRepositorySupplier, protocolRegistryPairs);
        deploymentsAssociationServiceBuilder.setInstance(deploymentsAssociationService);
        deploymentsAssociationServiceBuilder.setInitialMode(ServiceController.Mode.ON_DEMAND);
        deploymentsAssociationServiceBuilder.install();

        // finally, for each Remting connector specified, set up a client-mappings cache
        for (ModelNode connectorNameNode : connectorNameNodes) {
            String connectorName = connectorNameNode.asString();

            ServiceInstaller.BlockingBuilder.of(ServiceDependency.on(CONNECTOR_CAPABILITY_NAME, ProtocolSocketBinding.class, connectorName).map(EJB3RemoteServiceAdd::getClientMappings))
                    .provides(ServiceNameFactory.resolveServiceName(CLIENT_MAPPINGS, connectorName))
                    .build().install(context);

            ServiceDependency<ClientMappingsRegistryProvider> provider = getClientMappingsRegistryProvider(context, model);
            ServiceInstaller installer = new ServiceInstaller() {
                @Override
                public ServiceController<?> install(RequirementServiceTarget target) {
                    for (ServiceInstaller installer : provider.get().getServiceInstallers(connectorName, ServiceDependency.on(CLIENT_MAPPINGS, connectorName))) {
                        ServiceController<?> controller = installer.install(target);
                        ServiceName registryParentName = ServiceNameFactory.parseServiceName(ClusteringServiceDescriptor.REGISTRY.getName());
                        for (ServiceName providedName : controller.provides()) {
                            if (registryParentName.isParentOf(providedName)) {
                                ServiceInstaller.BlockingBuilder.of(ServiceDependency.on(providedName))
                                        .provides(ServiceNameFactory.resolveServiceName(EJB3RemoteResourceDefinition.CLIENT_MAPPINGS_REGISTRY, connectorName))
                                        .startWhen(StartWhen.AVAILABLE)
                                        .build().install(target);
                            }
                        }
                    }
                    return null;
                }
            };
            ServiceInstaller.Builder.of(installer, context.getCapabilityServiceSupport()).requires(provider).build().install(context);
        }

        // Install the Jakarta Enterprise Beans connector service which will listen for client connections
        // on the Remoting connector (i.e. the client is using the Remoting transport to send EJB invocations)
        final OptionMap channelCreationOptions = this.getChannelCreationOptions(context);

        // TODO: Externalize (expose via management API if needed) the version and the marshalling strategy
        final CapabilityServiceBuilder<?> remoteConnectorServiceBuilder = context.getCapabilityServiceTarget()
                .addCapability(EJB3RemoteResourceDefinition.EJB_REMOTE_CAPABILITY);
        final Consumer<EJBRemoteConnectorService> connectorServiceConsumer =
                remoteConnectorServiceBuilder.provides(EJB3RemoteResourceDefinition.EJB_REMOTE_CAPABILITY.getCapabilityServiceName());
        final Supplier<Endpoint> endpointSupplier = remoteConnectorServiceBuilder
                .requiresCapability(EJB3RemoteResourceDefinition.REMOTING_ENDPOINT_CAPABILITY_NAME, Endpoint.class);
        final Supplier<Executor> executorSupplier = !executeInWorker ? remoteConnectorServiceBuilder.requires(EJB3SubsystemRootResourceDefinition.EXECUTOR_SERVICE_DESCRIPTOR, threadPoolName) : Functions.constantSupplier(null);
        final Supplier<AssociationService> associationServiceSupplier2 = remoteConnectorServiceBuilder.requires(AssociationService.SERVICE_NAME);
        final Supplier<RemotingTransactionService> remotingTransactionServiceSupplier = remoteConnectorServiceBuilder.requiresCapability(
                EJB3RemoteResourceDefinition.REMOTE_TRANSACTION_SERVICE_CAPABILITY_NAME, RemotingTransactionService.class);

        final EJBRemoteConnectorService ejbRemoteConnectorService = new EJBRemoteConnectorService(connectorServiceConsumer, endpointSupplier,
                executorSupplier, associationServiceSupplier2, remotingTransactionServiceSupplier,
                channelCreationOptions, FilterSpecClassResolverFilter.getFilterForOperationContext(context));
        remoteConnectorServiceBuilder.setInstance(ejbRemoteConnectorService);
        remoteConnectorServiceBuilder.addAliases(EJBRemoteConnectorService.SERVICE_NAME);
        remoteConnectorServiceBuilder.setInitialMode(ServiceController.Mode.LAZY);
        remoteConnectorServiceBuilder.install();

        // Install the Jakarta Enterprise Beans connector service which will listen for client connections
        // on the HTTP connector (i.e. the client is using the HTTP transport to send EJB invocations)
        if(context.hasOptionalCapability(UNDERTOW_HTTP_INVOKER_CAPABILITY_NAME, EJB3SubsystemRootResourceDefinition.EJB_CAPABILITY.getName(), null)) {
            final ServiceBuilder<?> remoteHTTPServiceBuilder = serviceTarget.addService();
            final Consumer<EJB3RemoteHTTPService> remoteHTTPServiceConsumer = remoteHTTPServiceBuilder.provides(EJB3RemoteHTTPService.SERVICE_NAME);
            final Supplier<PathHandler> pathHandlerSupplier = remoteHTTPServiceBuilder.requires(context.getCapabilityServiceName(UNDERTOW_HTTP_INVOKER_CAPABILITY_NAME, PathHandler.class));
            final Supplier<LocalTransactionContext> localTransactionContextSupplier = remoteHTTPServiceBuilder.requires(TxnServices.JBOSS_TXN_LOCAL_TRANSACTION_CONTEXT);
            final Supplier<AssociationService> associationServiceSupplier3 = remoteHTTPServiceBuilder.requires(AssociationService.SERVICE_NAME);

            final EJB3RemoteHTTPService service = new EJB3RemoteHTTPService(remoteHTTPServiceConsumer, pathHandlerSupplier, associationServiceSupplier3,
                    localTransactionContextSupplier, FilterSpecClassResolverFilter.getFilterForOperationContext(context));
            remoteHTTPServiceBuilder.setInstance(service);
            remoteHTTPServiceBuilder.setInitialMode(ServiceController.Mode.PASSIVE);
            remoteHTTPServiceBuilder.install();
        }
    }

    /**
     * Extract any channel creation options specified in the model and convert to an OptionsMap
     * @param context
     * @return
     * @throws OperationFailedException
     */
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
     * registries. The preference for obtaining the provider is: - use a client mappings registry provider defined in the
     * distributable-ejb subsystem and installed as a service - otherwise, use the legacy provider loaded from the classpath
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
        return ServiceDependency.of(LEGACY_PROVIDER_FACTORY.createClientMappingsRegistryProvider(clusterName));
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

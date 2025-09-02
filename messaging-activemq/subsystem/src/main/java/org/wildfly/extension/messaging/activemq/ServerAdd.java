/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.messaging.activemq;

import static org.jboss.as.controller.ModuleIdentifierUtil.parseCanonicalModuleIdentifier;

import static org.wildfly.extension.messaging.activemq._private.MessagingLogger.ROOT_LOGGER;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.security.CredentialReference.handleCredentialReferenceUpdate;
import static org.jboss.as.controller.security.CredentialReference.rollbackCredentialStoreUpdate;
import static org.wildfly.extension.messaging.activemq.AddressSettingAdd.createDefaulAddressSettings;
import static org.wildfly.extension.messaging.activemq.Capabilities.ACTIVEMQ_SERVER_CAPABILITY;
import static org.wildfly.extension.messaging.activemq.Capabilities.DATA_SOURCE_CAPABILITY;
import static org.wildfly.extension.messaging.activemq.Capabilities.ELYTRON_DOMAIN_CAPABILITY;
import static org.wildfly.extension.messaging.activemq.Capabilities.ELYTRON_SSL_CONTEXT_CAPABILITY_NAME;
import static org.wildfly.extension.messaging.activemq.Capabilities.HTTP_UPGRADE_REGISTRY_CAPABILITY_NAME;
import static org.wildfly.extension.messaging.activemq.Capabilities.JMX_CAPABILITY;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.ADDRESS_SETTING;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.BINDINGS_DIRECTORY;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.HTTP_ACCEPTOR;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.INCOMING_INTERCEPTORS;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.JGROUPS_CLUSTER;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.JGROUPS_BROADCAST_GROUP;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.JGROUPS_DISCOVERY_GROUP;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.JOURNAL_DIRECTORY;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.LARGE_MESSAGES_DIRECTORY;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.MODULE;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.NAME;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.OUTGOING_INTERCEPTORS;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.PAGING_DIRECTORY;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.SECURITY_SETTING;
import static org.wildfly.extension.messaging.activemq.PathDefinition.PATHS;
import static org.wildfly.extension.messaging.activemq.PathDefinition.RELATIVE_TO;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.ADDRESS_QUEUE_SCAN_PERIOD;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.ASYNC_CONNECTION_EXECUTION_ENABLED;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.CLUSTER_PASSWORD;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.CLUSTER_USER;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.CONNECTION_TTL_OVERRIDE;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.CREATE_BINDINGS_DIR;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.CREATE_JOURNAL_DIR;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.CREDENTIAL_REFERENCE;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.DISK_SCAN_PERIOD;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.ELYTRON_DOMAIN;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.GLOBAL_MAX_DISK_USAGE;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.GLOBAL_MAX_MEMORY_SIZE;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.ID_CACHE_SIZE;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.JMX_DOMAIN;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.JMX_MANAGEMENT_ENABLED;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.JOURNAL_BINDINGS_TABLE;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.JOURNAL_BUFFER_SIZE;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.JOURNAL_BUFFER_TIMEOUT;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.JOURNAL_COMPACT_MIN_FILES;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.JOURNAL_COMPACT_PERCENTAGE;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.JOURNAL_DATASOURCE;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.JOURNAL_FILE_OPEN_TIMEOUT;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.JOURNAL_FILE_SIZE;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.JOURNAL_JDBC_LOCK_EXPIRATION;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.JOURNAL_JDBC_LOCK_RENEW_PERIOD;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.JOURNAL_JDBC_NETWORK_TIMEOUT;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.JOURNAL_LARGE_MESSAGES_TABLE;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.JOURNAL_MAX_ATTIC_FILES;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.JOURNAL_MAX_IO;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.JOURNAL_MESSAGES_TABLE;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.JOURNAL_MIN_FILES;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.JOURNAL_NODE_MANAGER_STORE_TABLE;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.JOURNAL_PAGE_STORE_TABLE;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.JOURNAL_POOL_FILES;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.JOURNAL_SYNC_NON_TRANSACTIONAL;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.JOURNAL_SYNC_TRANSACTIONAL;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.JOURNAL_TYPE;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.LOG_JOURNAL_WRITE_RATE;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.MANAGEMENT_ADDRESS;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.MANAGEMENT_NOTIFICATION_ADDRESS;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.MEMORY_MEASURE_INTERVAL;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.MEMORY_WARNING_THRESHOLD;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.MESSAGE_COUNTER_MAX_DAY_HISTORY;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.MESSAGE_COUNTER_SAMPLE_PERIOD;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.MESSAGE_EXPIRY_SCAN_PERIOD;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.MESSAGE_EXPIRY_THREAD_PRIORITY;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.NETWORK_CHECK_LIST;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.NETWORK_CHECK_NIC;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.NETWORK_CHECK_PERIOD;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.NETWORK_CHECK_PING6_COMMAND;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.NETWORK_CHECK_PING_COMMAND;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.NETWORK_CHECK_TIMEOUT;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.NETWORK_CHECK_URL_LIST;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.OVERRIDE_IN_VM_SECURITY;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.PAGE_MAX_CONCURRENT_IO;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.PERSISTENCE_ENABLED;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.PERSIST_DELIVERY_COUNT_BEFORE_DELIVERY;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.PERSIST_ID_CACHE;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.SCHEDULED_THREAD_POOL_MAX_SIZE;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.SECURITY_DOMAIN;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.SECURITY_ENABLED;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.SECURITY_INVALIDATION_INTERVAL;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.SERVER_DUMP_INTERVAL;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.STATISTICS_ENABLED;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.THREAD_POOL_MAX_SIZE;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.TRANSACTION_TIMEOUT;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.TRANSACTION_TIMEOUT_SCAN_PERIOD;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.WILD_CARD_ROUTING_ENABLED;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import javax.management.MBeanServer;
import javax.sql.DataSource;

import io.undertow.server.handlers.ChannelUpgradeHandler;
import javax.net.ssl.SSLContext;
import org.apache.activemq.artemis.api.core.BroadcastGroupConfiguration;
import org.apache.activemq.artemis.api.core.DiscoveryGroupConfiguration;
import org.apache.activemq.artemis.api.core.Interceptor;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.core.config.BridgeConfiguration;
import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.config.CoreQueueConfiguration;
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.config.storage.DatabaseStorageConfiguration;
import org.apache.activemq.artemis.core.security.Role;
import org.apache.activemq.artemis.core.server.JournalType;
import org.apache.activemq.artemis.core.settings.impl.AddressSettings;
import org.apache.activemq.artemis.utils.critical.CriticalAnalyzerPolicy;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.CapabilityServiceBuilder;
import org.jboss.as.controller.CapabilityServiceTarget;
import org.jboss.as.controller.ObjectTypeAttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.security.CredentialReference;
import org.jboss.as.controller.services.path.PathManager;
import org.jboss.as.network.OutboundSocketBinding;
import org.jboss.as.network.SocketBinding;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.modules.Module;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.common.function.ExceptionSupplier;
import org.wildfly.extension.messaging.activemq.broadcast.BroadcastCommandDispatcherFactory;
import org.wildfly.extension.messaging.activemq.ha.HAPolicyConfigurationBuilder;
import org.wildfly.extension.messaging.activemq.jms.JMSService;
import org.wildfly.extension.messaging.activemq._private.MessagingLogger;
import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.credential.source.CredentialSource;

/**
 * Add handler for a ActiveMQ server instance.
 *
 * @author Emanuel Muckenhuber
 * @author <a href="mailto:andy.taylor@jboss.com">Andy Taylor</a>
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 * @author <a href="http://jmesnil.net">Jeff Mesnil</a> (c) 2012 Red Hat Inc.
 */
class ServerAdd extends AbstractAddStepHandler {

// Artemis-specific system properties
    private static final String ARTEMIS_BROKER_CONFIG_NODEMANAGER_STORE_TABLE_NAME = "brokerconfig.storeConfiguration.nodeManagerStoreTableName";
    private static final String ARTEMIS_BROKER_CONFIG_JBDC_LOCK_RENEW_PERIOD_MILLIS = "brokerconfig.storeConfiguration.jdbcLockRenewPeriodMillis";
    private static final String ARTEMIS_BROKER_CONFIG_JBDC_LOCK_EXPIRATION_MILLIS = "brokerconfig.storeConfiguration.jdbcLockExpirationMillis";
    private static final String ARTEMIS_BROKER_CONFIG_JDBC_LOCK_ACQUISITION_TIMEOUT_MILLIS = "brokerconfig.storeConfiguration.jdbcLockAcquisitionTimeoutMillis";

    final BiConsumer<OperationContext, String> broadcastCommandDispatcherFactoryInstaller;

    ServerAdd(BiConsumer<OperationContext, String> broadcastCommandDispatcherFactoryInstaller) {
        super(ServerDefinition.ATTRIBUTES);
        this.broadcastCommandDispatcherFactoryInstaller = broadcastCommandDispatcherFactoryInstaller;
    }

    @Override
    protected Resource createResource(OperationContext context) {
        ActiveMQServerResource resource = new ActiveMQServerResource();
        context.addResource(PathAddress.EMPTY_ADDRESS, resource);
        return resource;
    }

    @Override
    protected void populateModel(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
        super.populateModel(context, operation, resource);
        handleCredentialReferenceUpdate(context, resource.getModel().get(CREDENTIAL_REFERENCE.getName()), CREDENTIAL_REFERENCE.getName());

        // add an operation to create all the messaging paths resources that have not been already been created
        // prior to adding the ActiveMQ server
        context.addStep(new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                final ModelNode model = Resource.Tools.readModel(resource);
                for (String path : PathDefinition.PATHS.keySet()) {
                    if (!model.get(ModelDescriptionConstants.PATH).hasDefined(path)) {
                        PathAddress pathAddress = PathAddress.pathAddress(PathElement.pathElement(ModelDescriptionConstants.PATH, path));
                        context.createResource(pathAddress);
                    }
                }
            }
        }, OperationContext.Stage.MODEL);
        context.addStep((operationContext, model) -> {
            // check that if journal-datasource is defined, no other attributes related to file-system journal are set.
            if (ServerDefinition.JOURNAL_DATASOURCE.resolveModelAttribute(context, model).isDefined()) {
                checkNoAttributesIsDefined(ServerDefinition.JOURNAL_DATASOURCE.getName(), operationContext.getCurrentAddress(), model,
                        ServerDefinition.JOURNAL_TYPE,
                        ServerDefinition.JOURNAL_BUFFER_TIMEOUT,
                        ServerDefinition.JOURNAL_BUFFER_SIZE,
                        ServerDefinition.JOURNAL_SYNC_TRANSACTIONAL,
                        ServerDefinition.JOURNAL_SYNC_NON_TRANSACTIONAL,
                        ServerDefinition.LOG_JOURNAL_WRITE_RATE,
                        ServerDefinition.JOURNAL_FILE_SIZE,
                        ServerDefinition.JOURNAL_MIN_FILES,
                        ServerDefinition.JOURNAL_POOL_FILES,
                        ServerDefinition.JOURNAL_FILE_OPEN_TIMEOUT,
                        ServerDefinition.JOURNAL_COMPACT_PERCENTAGE,
                        ServerDefinition.JOURNAL_COMPACT_MIN_FILES,
                        ServerDefinition.JOURNAL_MAX_IO,
                        ServerDefinition.CREATE_BINDINGS_DIR,
                        ServerDefinition.CREATE_JOURNAL_DIR);
            }
        }, OperationContext.Stage.MODEL);
    }

    /*
     * Check that none of the attrs are defined, or log a warning.
     */
    private void checkNoAttributesIsDefined(String definedAttributeName, PathAddress address, ModelNode model, AttributeDefinition... attrs) throws OperationFailedException {
        List<String> definedAttributes = new ArrayList<>();
        for (AttributeDefinition attr : attrs) {
            if (model.get(attr.getName()).isDefined()) {
                definedAttributes.add(attr.getName());
            }
        }

        if (!definedAttributes.isEmpty()) {
            MessagingLogger.ROOT_LOGGER.invalidConfiguration(address, definedAttributeName, definedAttributes);
        }
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
        // Add a RUNTIME step to actually install the ActiveMQ Service. This will execute after the runtime step
        // added by any child resources whose ADD handler executes after this one in the model stage.
        context.addStep(new InstallServerHandler(resource), OperationContext.Stage.RUNTIME);
    }

    @Override
    protected void rollbackRuntime(OperationContext context, final ModelNode operation, final Resource resource) {
        rollbackCredentialStoreUpdate(CREDENTIAL_REFERENCE, context, resource);
    }

    private class InstallServerHandler implements OperationStepHandler {

        private final Resource resource;

        private InstallServerHandler(Resource resource) {
            this.resource = resource;
        }

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            final CapabilityServiceTarget capabilityServiceTarget= context.getCapabilityServiceTarget();

            final String serverName = context.getCurrentAddressValue();

            // Transform the configuration based on the recursive model
            final ModelNode model = Resource.Tools.readModel(resource);
            final Configuration configuration = transformConfig(context, serverName, model);

            // Create path services
            String bindingsPath = PATHS.get(BINDINGS_DIRECTORY).resolveModelAttribute(context, model.get(PATH, BINDINGS_DIRECTORY)).asString();
            String bindingsRelativeToPath = RELATIVE_TO.resolveModelAttribute(context, model.get(PATH, BINDINGS_DIRECTORY)).asString();
            String journalPath = PATHS.get(JOURNAL_DIRECTORY).resolveModelAttribute(context, model.get(PATH, JOURNAL_DIRECTORY)).asString();
            String journalRelativeToPath = RELATIVE_TO.resolveModelAttribute(context, model.get(PATH, JOURNAL_DIRECTORY)).asString();
            String largeMessagePath = PATHS.get(LARGE_MESSAGES_DIRECTORY).resolveModelAttribute(context, model.get(PATH, LARGE_MESSAGES_DIRECTORY)).asString();
            String largeMessageRelativeToPath = RELATIVE_TO.resolveModelAttribute(context, model.get(PATH, LARGE_MESSAGES_DIRECTORY)).asString();
            String pagingPath = PATHS.get(PAGING_DIRECTORY).resolveModelAttribute(context, model.get(PATH, PAGING_DIRECTORY)).asString();
            String pagingRelativeToPath = RELATIVE_TO.resolveModelAttribute(context, model.get(PATH, PAGING_DIRECTORY)).asString();

            // Add the ActiveMQ Service
            ServiceName activeMQServiceName = MessagingServices.getActiveMQServiceName(serverName);
            final CapabilityServiceBuilder<?> serviceBuilder = capabilityServiceTarget.addCapability(ACTIVEMQ_SERVER_CAPABILITY);
            Supplier<PathManager> pathManager = serviceBuilder.requires(PathManager.SERVICE_DESCRIPTOR);

            Optional<Supplier<DataSource>> dataSource = Optional.empty();
            String dataSourceName = JOURNAL_DATASOURCE.resolveModelAttribute(context, model).asStringOrNull();
            if (dataSourceName != null) {
                dataSource = Optional.of(serviceBuilder.requiresCapability(DATA_SOURCE_CAPABILITY, DataSource.class, dataSourceName));
            }
            Optional<Supplier<MBeanServer>> mbeanServer = Optional.empty();
            if (context.hasOptionalCapability(JMX_CAPABILITY, ACTIVEMQ_SERVER_CAPABILITY.getDynamicName(serverName), null)) {
                ServiceName jmxCapability = context.getCapabilityServiceName(JMX_CAPABILITY, MBeanServer.class);
                mbeanServer = Optional.of(serviceBuilder.requires(jmxCapability));
            }

            // Inject a reference to the Elytron security domain if one has been defined.
            Optional<Supplier<SecurityDomain>> elytronSecurityDomain = Optional.empty();
            if (configuration.isSecurityEnabled()) {
                final String elytronSecurityDomainName = ELYTRON_DOMAIN.resolveModelAttribute(context, model).asStringOrNull();
                if (elytronSecurityDomainName != null) {
                    elytronSecurityDomain = Optional.of(serviceBuilder.requiresCapability(ELYTRON_DOMAIN_CAPABILITY, SecurityDomain.class, elytronSecurityDomainName));
                } else {
                    final String legacySecurityDomain = SECURITY_DOMAIN.resolveModelAttribute(context, model).asStringOrNull();
                    if (legacySecurityDomain == null) {
                        throw ROOT_LOGGER.securityEnabledWithoutDomain();
                    }
                    // legacy security
                    throw ROOT_LOGGER.legacySecurityUnsupported();
                }
            }

            List<Interceptor> incomingInterceptors = processInterceptors(INCOMING_INTERCEPTORS.resolveModelAttribute(context, operation));
            List<Interceptor> outgoingInterceptors = processInterceptors(OUTGOING_INTERCEPTORS.resolveModelAttribute(context, operation));

            // Process acceptors and connectors
            final Set<String> socketBindingNames = new HashSet<>();
            final Map<String, String> sslContextNames = new HashMap<>();
            TransportConfigOperationHandlers.processAcceptors(context, configuration, model, socketBindingNames, sslContextNames);

            Map<String, Supplier<SocketBinding>> socketBindings = new HashMap<>();
            for (final String socketBindingName : socketBindingNames) {
                Supplier<SocketBinding> socketBinding = serviceBuilder.requires(SocketBinding.SERVICE_DESCRIPTOR, socketBindingName);
                socketBindings.put(socketBindingName, socketBinding);
            }

            final Set<String> connectorsSocketBindings = new HashSet<>();
            configuration.setConnectorConfigurations(TransportConfigOperationHandlers.processConnectors(context, configuration.getName(), model, connectorsSocketBindings, sslContextNames));

            Map<String, Supplier<SSLContext>> sslContexts = new HashMap<>();
            for (final Map.Entry<String, String> entry : sslContextNames.entrySet()) {
                Supplier<SSLContext> sslContext = serviceBuilder.requiresCapability(ELYTRON_SSL_CONTEXT_CAPABILITY_NAME, SSLContext.class, entry.getValue());
                sslContexts.put(entry.getValue(), sslContext);
            }

            Map<String, Supplier<OutboundSocketBinding>> outboundSocketBindings = new HashMap<>();
            Map<String, Boolean> outbounds = TransportConfigOperationHandlers.listOutBoundSocketBinding(context, connectorsSocketBindings);
            for (final String connectorSocketBinding : connectorsSocketBindings) {
                // find whether the connectorSocketBinding references a SocketBinding or an OutboundSocketBinding
                if (outbounds.get(connectorSocketBinding)) {
                    Supplier<OutboundSocketBinding> outboundSocketBinding = serviceBuilder.requires(OutboundSocketBinding.SERVICE_DESCRIPTOR, connectorSocketBinding);
                    outboundSocketBindings.put(connectorSocketBinding, outboundSocketBinding);
                } else {
                    // check if the socket binding has not already been added by the acceptors
                    if (!socketBindings.containsKey(connectorSocketBinding)) {
                        Supplier<SocketBinding> socketBinding = serviceBuilder.requires(SocketBinding.SERVICE_DESCRIPTOR, connectorSocketBinding);
                        socketBindings.put(connectorSocketBinding, socketBinding);
                    }
                }
            }
            // if there is any HTTP acceptor, add a dependency on the http-upgrade-registry service to
            // make sure that ActiveMQ server will be stopped *after* the registry (and its underlying XNIO thread)
            // is stopped.
            Set<String> httpListeners = new HashSet<>();
            if (model.hasDefined(HTTP_ACCEPTOR)) {
                for (final Property property : model.get(HTTP_ACCEPTOR).asPropertyList()) {
                    String httpListener = HTTPAcceptorDefinition.HTTP_LISTENER.resolveModelAttribute(context, property.getValue()).asString();
                    httpListeners.add(httpListener);
                }
            }
            for (String httpListener : httpListeners) {
                serviceBuilder.requires(context.getCapabilityServiceName(HTTP_UPGRADE_REGISTRY_CAPABILITY_NAME, httpListener, ChannelUpgradeHandler.class));
            }

            //this requires connectors
            BroadcastGroupAdd.addBroadcastGroupConfigs(context, configuration.getBroadcastGroupConfigurations(), configuration.getConnectorConfigurations().keySet(), model);

            final List<BroadcastGroupConfiguration> broadcastGroupConfigurations = configuration.getBroadcastGroupConfigurations();
            final Map<String, DiscoveryGroupConfiguration> discoveryGroupConfigurations = configuration.getDiscoveryGroupConfigurations();

            final Map<String, String> clusterNames = new HashMap<>(); // Maps key -> cluster name
            final Map<String, Supplier<BroadcastCommandDispatcherFactory>> commandDispatcherFactories = new HashMap<>();
            final Map<String, Supplier<SocketBinding>> groupBindings = new HashMap<>();
            final Map<ServiceName, Supplier<SocketBinding>> groupBindingServices = new HashMap<>();

            if (broadcastGroupConfigurations != null) {
                for (final BroadcastGroupConfiguration config : broadcastGroupConfigurations) {
                    final String name = config.getName();
                    final String key = "broadcast" + name;
                    if (model.hasDefined(JGROUPS_BROADCAST_GROUP, name)) {
                        ModelNode broadcastGroupModel = model.get(JGROUPS_BROADCAST_GROUP, name);
                        String channelName = JGroupsBroadcastGroupDefinition.JGROUPS_CHANNEL.resolveModelAttribute(context, broadcastGroupModel).asStringOrNull();
                        ServerAdd.this.broadcastCommandDispatcherFactoryInstaller.accept(context, channelName);
                        commandDispatcherFactories.put(key, serviceBuilder.requires(MessagingServices.getBroadcastCommandDispatcherFactoryServiceName(channelName)));
                        String clusterName = JGROUPS_CLUSTER.resolveModelAttribute(context, broadcastGroupModel).asString();
                        clusterNames.put(key, clusterName);
                    } else {
                        final ServiceName groupBindingServiceName = GroupBindingService.getBroadcastBaseServiceName(activeMQServiceName).append(name);
                        if (!groupBindingServices.containsKey(groupBindingServiceName)) {
                            Supplier<SocketBinding> groupBinding = serviceBuilder.requires(groupBindingServiceName);
                            groupBindingServices.put(groupBindingServiceName, groupBinding);
                        }
                        groupBindings.put(key, groupBindingServices.get(groupBindingServiceName));
                    }
                }
            }
            if (discoveryGroupConfigurations != null) {
                for (final DiscoveryGroupConfiguration config : discoveryGroupConfigurations.values()) {
                    final String name = config.getName();
                    final String key = "discovery" + name;
                    if (model.hasDefined(JGROUPS_DISCOVERY_GROUP, name)) {
                        ModelNode discoveryGroupModel = model.get(JGROUPS_DISCOVERY_GROUP, name);
                        String channelName = JGroupsDiscoveryGroupDefinition.JGROUPS_CHANNEL.resolveModelAttribute(context, discoveryGroupModel).asStringOrNull();
                        ServerAdd.this.broadcastCommandDispatcherFactoryInstaller.accept(context, channelName);
                        commandDispatcherFactories.put(key, serviceBuilder.requires(MessagingServices.getBroadcastCommandDispatcherFactoryServiceName(channelName)));
                        String clusterName = JGROUPS_CLUSTER.resolveModelAttribute(context, discoveryGroupModel).asString();
                        clusterNames.put(key, clusterName);
                    } else {
                        final ServiceName groupBindingServiceName = GroupBindingService.getDiscoveryBaseServiceName(activeMQServiceName).append(name);
                        if (!groupBindingServices.containsKey(groupBindingServiceName)) {
                            Supplier<SocketBinding> groupBinding = serviceBuilder.requires(groupBindingServiceName);
                            groupBindingServices.put(groupBindingServiceName, groupBinding);
                        }
                        groupBindings.put(key, groupBindingServices.get(groupBindingServiceName));
                    }
                }
            }

            // Create the ActiveMQ Service
            final ActiveMQServerService serverService = new ActiveMQServerService(
                    configuration,
                    new ActiveMQServerService.PathConfig(bindingsPath, bindingsRelativeToPath, journalPath, journalRelativeToPath, largeMessagePath, largeMessageRelativeToPath, pagingPath, pagingRelativeToPath),
                    pathManager,
                    incomingInterceptors,
                    outgoingInterceptors,
                    socketBindings,
                    outboundSocketBindings,
                    groupBindings,
                    commandDispatcherFactories,
                    clusterNames,
                    elytronSecurityDomain,
                    mbeanServer,
                    dataSource,
                    sslContexts
            );

            // inject credential-references for bridges
            addBridgeCredentialStoreReference(serverService, configuration, BridgeDefinition.CREDENTIAL_REFERENCE, context, model, serviceBuilder);
            addClusterCredentialStoreReference(serverService, CREDENTIAL_REFERENCE, context, model, serviceBuilder);

            // Install the ActiveMQ Service
            ServiceController activeMQServerServiceController = serviceBuilder.setInstance(serverService)
                    .install();
            //Add the queue services for the core queues  created throught the internal broker configuration (those queues are not added as service via the QueueAdd OSH)
            if (model.hasDefined(CommonAttributes.QUEUE)) {
                ModelNode coreQueues = model.get(CommonAttributes.QUEUE);
                for (CoreQueueConfiguration queueConfiguration : configuration.getQueueConfigurations()) {
                    if (coreQueues.has(queueConfiguration.getName())) {
                        final ServiceName queueServiceName = activeMQServiceName.append(queueConfiguration.getName());
                        final ServiceBuilder sb = context.getServiceTarget().addService(queueServiceName);
                        sb.requires(ActiveMQActivationService.getServiceName(activeMQServiceName));
                        Supplier<ActiveMQBroker> serverSupplier = sb.requires(activeMQServiceName);
                        final QueueService queueService = new QueueService(serverSupplier, queueConfiguration, false, false);
                        sb.setInitialMode(Mode.PASSIVE);
                        sb.setInstance(queueService);
                        sb.install();
                    }
                }
            }
            // Provide our custom Resource impl a ref to the ActiveMQ server so it can create child runtime resources
            ((ActiveMQServerResource) resource).setActiveMQServerServiceController(activeMQServerServiceController);
            // Install the JMSService
            boolean overrideInVMSecurity = OVERRIDE_IN_VM_SECURITY.resolveModelAttribute(context, operation).asBoolean();
            JMSService.addService(capabilityServiceTarget, activeMQServiceName, overrideInVMSecurity);

            context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
        }

        private void processStorageConfiguration(OperationContext context, ModelNode model, Configuration configuration) throws OperationFailedException {
            ModelNode journalDataSource = JOURNAL_DATASOURCE.resolveModelAttribute(context, model);
            if (!journalDataSource.isDefined()) {
                return;
            }
            DatabaseStorageConfiguration storageConfiguration = new DatabaseStorageConfiguration();
            storageConfiguration.setBindingsTableName(JOURNAL_BINDINGS_TABLE.resolveModelAttribute(context, model).asString());
            storageConfiguration.setMessageTableName(JOURNAL_MESSAGES_TABLE.resolveModelAttribute(context, model).asString());
            storageConfiguration.setLargeMessageTableName(JOURNAL_LARGE_MESSAGES_TABLE.resolveModelAttribute(context, model).asString());
            storageConfiguration.setPageStoreTableName(JOURNAL_PAGE_STORE_TABLE.resolveModelAttribute(context, model).asString());
            long networkTimeout = SECONDS.toMillis(JOURNAL_JDBC_NETWORK_TIMEOUT.resolveModelAttribute(context, model).asInt());
            // ARTEMIS-1493: Artemis API is not correct. the value must be in millis but it requires an int instead of a long.
            storageConfiguration.setJdbcNetworkTimeout((int) networkTimeout);
            // WFLY-9513 - check for System properties for HA JDBC store attributes
            //
            // if the attribute is defined, we use its value
            // otherwise, we check first for a system property
            // finally we use the attribute's default value
            //
            // this behaviour applies to JOURNAL_NODE_MANAGER_STORE_TABLE, JOURNAL_JDBC_LOCK_EXPIRATION
            // and JOURNAL_JDBC_LOCK_RENEW_PERIOD attributes.
            final String nodeManagerStoreTableName;
            if (model.hasDefined(JOURNAL_NODE_MANAGER_STORE_TABLE.getName())) {
                nodeManagerStoreTableName = JOURNAL_NODE_MANAGER_STORE_TABLE.resolveModelAttribute(context, model).asString();
            } else if (org.wildfly.security.manager.WildFlySecurityManager.getSystemPropertiesPrivileged().containsKey(ARTEMIS_BROKER_CONFIG_NODEMANAGER_STORE_TABLE_NAME)) {
                nodeManagerStoreTableName = org.wildfly.security.manager.WildFlySecurityManager.getSystemPropertiesPrivileged().getProperty(ARTEMIS_BROKER_CONFIG_NODEMANAGER_STORE_TABLE_NAME);
            } else {
                nodeManagerStoreTableName = JOURNAL_NODE_MANAGER_STORE_TABLE.getDefaultValue().asString();
            }
            // the system property is removed, otherwise Artemis will use it to override the value from the configuration
            org.wildfly.security.manager.WildFlySecurityManager.getSystemPropertiesPrivileged().remove(ARTEMIS_BROKER_CONFIG_NODEMANAGER_STORE_TABLE_NAME);
            storageConfiguration.setNodeManagerStoreTableName(nodeManagerStoreTableName);
            final long lockExpirationInMillis;
            if (model.hasDefined(JOURNAL_JDBC_LOCK_EXPIRATION.getName())) {
                lockExpirationInMillis = SECONDS.toMillis(JOURNAL_JDBC_LOCK_EXPIRATION.resolveModelAttribute(context, model).asInt());
            } else if (org.wildfly.security.manager.WildFlySecurityManager.getSystemPropertiesPrivileged().containsKey(ARTEMIS_BROKER_CONFIG_JBDC_LOCK_EXPIRATION_MILLIS)) {
                lockExpirationInMillis = Long.parseLong(org.wildfly.security.manager.WildFlySecurityManager.getSystemPropertiesPrivileged().getProperty(ARTEMIS_BROKER_CONFIG_JBDC_LOCK_EXPIRATION_MILLIS));
            } else {
                lockExpirationInMillis = SECONDS.toMillis(JOURNAL_JDBC_LOCK_EXPIRATION.getDefaultValue().asInt());
            }
            // the system property is removed, otherwise Artemis will use it to override the value from the configuration
            org.wildfly.security.manager.WildFlySecurityManager.getSystemPropertiesPrivileged().remove(ARTEMIS_BROKER_CONFIG_JBDC_LOCK_EXPIRATION_MILLIS);
            storageConfiguration.setJdbcLockExpirationMillis(lockExpirationInMillis);
            final long lockRenewPeriodInMillis;
            if (model.hasDefined(JOURNAL_JDBC_LOCK_RENEW_PERIOD.getName())) {
                lockRenewPeriodInMillis = SECONDS.toMillis(JOURNAL_JDBC_LOCK_RENEW_PERIOD.resolveModelAttribute(context, model).asInt());
            } else if (org.wildfly.security.manager.WildFlySecurityManager.getSystemPropertiesPrivileged().containsKey(ARTEMIS_BROKER_CONFIG_JBDC_LOCK_RENEW_PERIOD_MILLIS)) {
                lockRenewPeriodInMillis = Long.parseLong(org.wildfly.security.manager.WildFlySecurityManager.getSystemPropertiesPrivileged().getProperty(ARTEMIS_BROKER_CONFIG_JBDC_LOCK_RENEW_PERIOD_MILLIS));
            } else {
                lockRenewPeriodInMillis = SECONDS.toMillis(JOURNAL_JDBC_LOCK_RENEW_PERIOD.getDefaultValue().asInt());
            }
            // the system property is removed, otherwise Artemis will use it to override the value from the configuration
            org.wildfly.security.manager.WildFlySecurityManager.getSystemPropertiesPrivileged().remove(ARTEMIS_BROKER_CONFIG_JBDC_LOCK_RENEW_PERIOD_MILLIS);
            storageConfiguration.setJdbcLockRenewPeriodMillis(lockRenewPeriodInMillis);
            // this property is used for testing only and has no corresponding model attribute.
            // However the default value in Artemis is not correct (should be -1, not 60s)
            final long jdbcLockAcquisitionTimeoutMillis;
            if (org.wildfly.security.manager.WildFlySecurityManager.getSystemPropertiesPrivileged().containsKey(ARTEMIS_BROKER_CONFIG_JDBC_LOCK_ACQUISITION_TIMEOUT_MILLIS)) {
                jdbcLockAcquisitionTimeoutMillis = Long.parseLong(org.wildfly.security.manager.WildFlySecurityManager.getSystemPropertiesPrivileged().getProperty(ARTEMIS_BROKER_CONFIG_JDBC_LOCK_ACQUISITION_TIMEOUT_MILLIS));
            } else {
                jdbcLockAcquisitionTimeoutMillis = -1;
            }
            // the system property is removed, otherwise Artemis will use it to override the value from the configuration
            org.wildfly.security.manager.WildFlySecurityManager.getSystemPropertiesPrivileged().remove(ARTEMIS_BROKER_CONFIG_JDBC_LOCK_ACQUISITION_TIMEOUT_MILLIS);
            storageConfiguration.setJdbcLockAcquisitionTimeoutMillis(jdbcLockAcquisitionTimeoutMillis);
            configuration.setStoreConfiguration(storageConfiguration);
        }

        /**
         * Transform the detyped operation parameters into the ActiveMQ configuration.
         *
         * @param context the operation context
         * @param serverName the name of the ActiveMQ instance
         * @param model the subsystem root resource model
         * @return the ActiveMQ configuration
         */
        private Configuration transformConfig(final OperationContext context, String serverName, final ModelNode model) throws OperationFailedException {

            Configuration configuration = new ConfigurationImpl();

            configuration.setName(serverName);
            //To avoid the automatic reloading of the logging.properties by the broker.
            configuration.setConfigurationFileRefreshPeriod(-1);
            configuration.setAddressQueueScanPeriod(ADDRESS_QUEUE_SCAN_PERIOD.resolveModelAttribute(context, model).asLong());
            configuration.setEnabledAsyncConnectionExecution(ASYNC_CONNECTION_EXECUTION_ENABLED.resolveModelAttribute(context, model).asBoolean());

            configuration.setClusterPassword(CLUSTER_PASSWORD.resolveModelAttribute(context, model).asString());
            configuration.setClusterUser(CLUSTER_USER.resolveModelAttribute(context, model).asString());
            configuration.setConnectionTTLOverride(CONNECTION_TTL_OVERRIDE.resolveModelAttribute(context, model).asInt());
            configuration.setCreateBindingsDir(CREATE_BINDINGS_DIR.resolveModelAttribute(context, model).asBoolean());
            configuration.setCreateJournalDir(CREATE_JOURNAL_DIR.resolveModelAttribute(context, model).asBoolean());
            configuration.setGlobalMaxSize(GLOBAL_MAX_MEMORY_SIZE.resolveModelAttribute(context, model).asLong());
            configuration.setMaxDiskUsage(GLOBAL_MAX_DISK_USAGE.resolveModelAttribute(context, model).asInt());
            configuration.setDiskScanPeriod(DISK_SCAN_PERIOD.resolveModelAttribute(context, model).asInt());
            configuration.setIDCacheSize(ID_CACHE_SIZE.resolveModelAttribute(context, model).asInt());
            // TODO do we want to allow the jmx configuration ?
            configuration.setJMXDomain(JMX_DOMAIN.resolveModelAttribute(context, model).asString());
            configuration.setJMXManagementEnabled(JMX_MANAGEMENT_ENABLED.resolveModelAttribute(context, model).asBoolean());
            // Journal
            final JournalType journalType = JournalType.valueOf(JOURNAL_TYPE.resolveModelAttribute(context, model).asString());
            configuration.setJournalType(journalType);

            ModelNode value = JOURNAL_BUFFER_SIZE.resolveModelAttribute(context, model);
            if (value.isDefined()) {
                configuration.setJournalBufferSize_AIO(value.asInt());
                configuration.setJournalBufferSize_NIO(value.asInt());
            }
            value = JOURNAL_BUFFER_TIMEOUT.resolveModelAttribute(context, model);
            if (value.isDefined()) {
                configuration.setJournalBufferTimeout_AIO(value.asInt());
                configuration.setJournalBufferTimeout_NIO(value.asInt());
            }
            value = JOURNAL_MAX_IO.resolveModelAttribute(context, model);
            if (value.isDefined()) {
                configuration.setJournalMaxIO_AIO(value.asInt());
                configuration.setJournalMaxIO_NIO(value.asInt());
            }
            configuration.setJournalCompactMinFiles(JOURNAL_COMPACT_MIN_FILES.resolveModelAttribute(context, model).asInt());
            configuration.setJournalCompactPercentage(JOURNAL_COMPACT_PERCENTAGE.resolveModelAttribute(context, model).asInt());
            configuration.setJournalFileSize(JOURNAL_FILE_SIZE.resolveModelAttribute(context, model).asInt());
            configuration.setJournalMinFiles(JOURNAL_MIN_FILES.resolveModelAttribute(context, model).asInt());
            configuration.setJournalPoolFiles(JOURNAL_POOL_FILES.resolveModelAttribute(context, model).asInt());
            configuration.setJournalFileOpenTimeout(JOURNAL_FILE_OPEN_TIMEOUT.resolveModelAttribute(context, model).asInt());
            configuration.setJournalSyncNonTransactional(JOURNAL_SYNC_NON_TRANSACTIONAL.resolveModelAttribute(context, model).asBoolean());
            configuration.setJournalSyncTransactional(JOURNAL_SYNC_TRANSACTIONAL.resolveModelAttribute(context, model).asBoolean());
            configuration.setJournalMaxAtticFiles(JOURNAL_MAX_ATTIC_FILES.resolveModelAttribute(context, model).asInt());
            configuration.setLogJournalWriteRate(LOG_JOURNAL_WRITE_RATE.resolveModelAttribute(context, model).asBoolean());

            configuration.setManagementAddress(SimpleString.toSimpleString(MANAGEMENT_ADDRESS.resolveModelAttribute(context, model).asString()));
            configuration.setManagementNotificationAddress(SimpleString.toSimpleString(MANAGEMENT_NOTIFICATION_ADDRESS.resolveModelAttribute(context, model).asString()));

            configuration.setMemoryMeasureInterval(MEMORY_MEASURE_INTERVAL.resolveModelAttribute(context, model).asLong());
            configuration.setMemoryWarningThreshold(MEMORY_WARNING_THRESHOLD.resolveModelAttribute(context, model).asInt());

            configuration.setMessageCounterEnabled(STATISTICS_ENABLED.resolveModelAttribute(context, model).asBoolean());
            configuration.setMessageCounterSamplePeriod(MESSAGE_COUNTER_SAMPLE_PERIOD.resolveModelAttribute(context, model).asInt());
            configuration.setMessageCounterMaxDayHistory(MESSAGE_COUNTER_MAX_DAY_HISTORY.resolveModelAttribute(context, model).asInt());
            configuration.setMessageExpiryScanPeriod(MESSAGE_EXPIRY_SCAN_PERIOD.resolveModelAttribute(context, model).asLong());
            configuration.setMessageExpiryThreadPriority(MESSAGE_EXPIRY_THREAD_PRIORITY.resolveModelAttribute(context, model).asInt());

            configuration.setPersistDeliveryCountBeforeDelivery(PERSIST_DELIVERY_COUNT_BEFORE_DELIVERY.resolveModelAttribute(context, model).asBoolean());

            configuration.setPageMaxConcurrentIO(PAGE_MAX_CONCURRENT_IO.resolveModelAttribute(context, model).asInt());

            configuration.setPersistenceEnabled(PERSISTENCE_ENABLED.resolveModelAttribute(context, model).asBoolean());
            configuration.setPersistIDCache(PERSIST_ID_CACHE.resolveModelAttribute(context, model).asBoolean());

            configuration.setScheduledThreadPoolMaxSize(SCHEDULED_THREAD_POOL_MAX_SIZE.resolveModelAttribute(context, model).asInt());
            configuration.setSecurityEnabled(SECURITY_ENABLED.resolveModelAttribute(context, model).asBoolean());
            configuration.setSecurityInvalidationInterval(SECURITY_INVALIDATION_INTERVAL.resolveModelAttribute(context, model).asLong());
            configuration.setServerDumpInterval(SERVER_DUMP_INTERVAL.resolveModelAttribute(context, model).asLong());
            configuration.setThreadPoolMaxSize(THREAD_POOL_MAX_SIZE.resolveModelAttribute(context, model).asInt());
            configuration.setTransactionTimeout(TRANSACTION_TIMEOUT.resolveModelAttribute(context, model).asLong());
            configuration.setTransactionTimeoutScanPeriod(TRANSACTION_TIMEOUT_SCAN_PERIOD.resolveModelAttribute(context, model).asLong());
            configuration.getWildcardConfiguration().setRoutingEnabled(WILD_CARD_ROUTING_ENABLED.resolveModelAttribute(context, model).asBoolean());

            if (model.hasDefined(NETWORK_CHECK_NIC.getName())
                    || model.hasDefined(NETWORK_CHECK_PERIOD.getName())
                    || model.hasDefined(NETWORK_CHECK_TIMEOUT.getName())
                    || model.hasDefined(NETWORK_CHECK_LIST.getName())
                    || model.hasDefined(NETWORK_CHECK_URL_LIST.getName())
                    || model.hasDefined(NETWORK_CHECK_PING_COMMAND.getName())
                    || model.hasDefined(NETWORK_CHECK_PING6_COMMAND.getName())) {
                configuration.setNetworCheckNIC(NETWORK_CHECK_NIC.resolveModelAttribute(context, model).asStringOrNull());
                configuration.setNetworkCheckList(NETWORK_CHECK_LIST.resolveModelAttribute(context, model).asStringOrNull());
                configuration.setNetworkCheckPeriod(NETWORK_CHECK_PERIOD.resolveModelAttribute(context, model).asLong());
                configuration.setNetworkCheckPing6Command(NETWORK_CHECK_PING6_COMMAND.resolveModelAttribute(context, model).asString());
                configuration.setNetworkCheckPingCommand(NETWORK_CHECK_PING_COMMAND.resolveModelAttribute(context, model).asString());
                configuration.setNetworkCheckTimeout(NETWORK_CHECK_TIMEOUT.resolveModelAttribute(context, model).asInt());
                configuration.setNetworkCheckURLList(NETWORK_CHECK_URL_LIST.resolveModelAttribute(context, model).asStringOrNull());
            }

            configuration.setCriticalAnalyzer(ServerDefinition.CRITICAL_ANALYZER_ENABLED.resolveModelAttribute(context, model).asBoolean());
            configuration.setCriticalAnalyzerCheckPeriod(ServerDefinition.CRITICAL_ANALYZER_CHECK_PERIOD.resolveModelAttribute(context, model).asLong());
            configuration.setCriticalAnalyzerPolicy(CriticalAnalyzerPolicy.valueOf(ServerDefinition.CRITICAL_ANALYZER_POLICY.resolveModelAttribute(context, model).asString()));
            configuration.setCriticalAnalyzerTimeout(ServerDefinition.CRITICAL_ANALYZER_TIMEOUT.resolveModelAttribute(context, model).asLong());

            processStorageConfiguration(context, model, configuration);
            HAPolicyConfigurationBuilder.getInstance().addHAPolicyConfiguration(context, configuration, model);

            processAddressSettings(context, configuration, model);
            processSecuritySettings(context, configuration, model);

            // Add in items from child resources
            ConfigurationHelper.addGroupingHandlerConfiguration(context, configuration, model);
            configuration.setDiscoveryGroupConfigurations(ConfigurationHelper.addDiscoveryGroupConfigurations(context, model));
            ConfigurationHelper.addDivertConfigurations(context, configuration, model);
            ConfigurationHelper.addQueueConfigurations(context, configuration, model);
            ConfigurationHelper.addBridgeConfigurations(context, configuration, model);
            ConfigurationHelper.addClusterConnectionConfigurations(context, configuration, model);
            ConfigurationHelper.addConnectorServiceConfigurations(context, configuration, model);

            return configuration;
        }

        /**
         * Process the address settings.
         *
         * @param configuration the ActiveMQ configuration
         * @param params the detyped operation parameters
         * @throws OperationFailedException
         */
        private void processAddressSettings(final OperationContext context, final Configuration configuration, final ModelNode params) throws OperationFailedException {
            boolean merged = false;
            if (params.hasDefined(ADDRESS_SETTING)) {
                for (final Property property : params.get(ADDRESS_SETTING).asPropertyList()) {
                    final String match = property.getName();
                    final ModelNode config = property.getValue();
                    boolean isRootAddressMatch = configuration.getWildcardConfiguration().getAnyWordsString().equals(match);
                    final AddressSettings settings = AddressSettingAdd.createSettings(context, config, isRootAddressMatch);
                    if (!merged && isRootAddressMatch) {
                        settings.merge(createDefaulAddressSettings());
                        merged = true;
                    }
                    configuration.addAddressSetting(match, settings);
                }
                if (!merged) {
                    configuration.addAddressSetting(configuration.getWildcardConfiguration().getAnyWordsString(), createDefaulAddressSettings());
                }
            }
        }

        private List<Class> unwrapClasses(List<ModelNode> classesModel) throws OperationFailedException {
            List<Class> classes = new ArrayList<>();

            for (ModelNode classModel : classesModel) {
                Class<?> clazz = unwrapClass(classModel);
                classes.add(clazz);
            }

            return classes;
        }

        private Class unwrapClass(ModelNode classModel) throws OperationFailedException {
            String className = classModel.get(NAME).asString();
            String moduleName = classModel.get(MODULE).asString();
            try {
                Module module = Module.getCallerModuleLoader().loadModule(parseCanonicalModuleIdentifier(moduleName));
                Class<?> clazz = module.getClassLoader().loadClass(className);
                return clazz;
            } catch (Exception e) {
                throw MessagingLogger.ROOT_LOGGER.unableToLoadClassFromModule(className, moduleName);
            }
        }

        private List<Interceptor> processInterceptors(ModelNode model) throws OperationFailedException {
            if (!model.isDefined()) {
                return Collections.emptyList();
            }
            List<Interceptor> interceptors = new ArrayList<>();
            List<ModelNode> interceptorModels = model.asList();
            for (Class clazz : unwrapClasses(interceptorModels)) {
                try {
                    Interceptor interceptor = Interceptor.class.cast(clazz.newInstance());
                    interceptors.add(interceptor);
                } catch (Exception e) {
                    throw new OperationFailedException(e);
                }
            }
            return interceptors;
        }

        /**
         * Process the security settings.
         *
         * @param configuration the ActiveMQ configuration
         * @param params the detyped operation parameters
         */
        private void processSecuritySettings(final OperationContext context, final Configuration configuration, final ModelNode params) throws OperationFailedException {
            if (params.get(SECURITY_SETTING).isDefined()) {
                for (final Property property : params.get(SECURITY_SETTING).asPropertyList()) {
                    final String match = property.getName();
                    final ModelNode config = property.getValue();

                    if (config.hasDefined(CommonAttributes.ROLE)) {
                        final Set<Role> roles = new HashSet<Role>();
                        for (final Property role : config.get(CommonAttributes.ROLE).asPropertyList()) {
                            roles.add(SecurityRoleDefinition.transform(context, role.getName(), role.getValue()));
                        }
                        configuration.getSecurityRoles().put(match, roles);
                    }
                }
            }
        }

        private void addBridgeCredentialStoreReference(ActiveMQServerService amqService, Configuration configuration, ObjectTypeAttributeDefinition credentialReferenceAttributeDefinition, OperationContext context, ModelNode model, ServiceBuilder<?> serviceBuilder) throws OperationFailedException {
            for (BridgeConfiguration bridgeConfiguration : configuration.getBridgeConfigurations()) {
                String name = bridgeConfiguration.getName();
                InjectedValue<ExceptionSupplier<CredentialSource, Exception>> injector = amqService.getBridgeCredentialSourceSupplierInjector(name);

                String[] modelFilter = {CommonAttributes.BRIDGE, name};

                ModelNode filteredModelNode = model;
                if (modelFilter != null && modelFilter.length > 0) {
                    for (String path : modelFilter) {
                        if (filteredModelNode.get(path).isDefined()) {
                            filteredModelNode = filteredModelNode.get(path);
                        } else {
                            break;
                        }
                    }
                }
                ModelNode value = credentialReferenceAttributeDefinition.resolveModelAttribute(context, filteredModelNode);
                if (value.isDefined()) {
                    injector.inject(CredentialReference.getCredentialSourceSupplier(context, credentialReferenceAttributeDefinition, filteredModelNode, serviceBuilder));
                }
            }
        }

        private void addClusterCredentialStoreReference(ActiveMQServerService amqService, ObjectTypeAttributeDefinition credentialReferenceAttributeDefinition, OperationContext context, ModelNode model, ServiceBuilder<?> serviceBuilder) throws OperationFailedException {
            ModelNode value = credentialReferenceAttributeDefinition.resolveModelAttribute(context, model);
            if (value.isDefined()) {
                amqService.getClusterCredentialSourceSupplierInjector()
                        .inject(CredentialReference.getCredentialSourceSupplier(context, credentialReferenceAttributeDefinition, model, serviceBuilder));
            }
        }
    }
}

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

package org.wildfly.extension.messaging.activemq;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.wildfly.extension.messaging.activemq.Capabilities.ACTIVEMQ_SERVER_CAPABILITY;
import static org.wildfly.extension.messaging.activemq.Capabilities.DATA_SOURCE_CAPABILITY;
import static org.wildfly.extension.messaging.activemq.Capabilities.ELYTRON_DOMAIN_CAPABILITY;
import static org.wildfly.extension.messaging.activemq.Capabilities.JMX_CAPABILITY;
import static org.wildfly.extension.messaging.activemq.Capabilities.PATH_MANAGER_CAPABILITY;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.ADDRESS_SETTING;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.BINDINGS_DIRECTORY;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.BROADCAST_GROUP;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.DISCOVERY_GROUP;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.HTTP_ACCEPTOR;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.INCOMING_INTERCEPTORS;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.JGROUPS_CLUSTER;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.JOURNAL_DIRECTORY;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.LARGE_MESSAGES_DIRECTORY;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.MODULE;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.NAME;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.OUTGOING_INTERCEPTORS;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.PAGING_DIRECTORY;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.SECURITY_SETTING;
import static org.wildfly.extension.messaging.activemq.PathDefinition.PATHS;
import static org.wildfly.extension.messaging.activemq.PathDefinition.RELATIVE_TO;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.ASYNC_CONNECTION_EXECUTION_ENABLED;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.CLUSTER_PASSWORD;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.CLUSTER_USER;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.CONNECTION_TTL_OVERRIDE;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.CREATE_BINDINGS_DIR;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.CREATE_JOURNAL_DIR;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.ELYTRON_DOMAIN;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.ID_CACHE_SIZE;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.JMX_DOMAIN;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.JMX_MANAGEMENT_ENABLED;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.JOURNAL_BINDINGS_TABLE;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.JOURNAL_BUFFER_SIZE;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.JOURNAL_BUFFER_TIMEOUT;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.JOURNAL_COMPACT_MIN_FILES;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.JOURNAL_COMPACT_PERCENTAGE;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.JOURNAL_DATABASE;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.JOURNAL_DATASOURCE;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.JOURNAL_FILE_SIZE;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.JOURNAL_JDBC_LOCK_EXPIRATION;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.JOURNAL_JDBC_LOCK_RENEW_PERIOD;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.JOURNAL_JDBC_NETWORK_TIMEOUT;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.JOURNAL_LARGE_MESSAGES_TABLE;
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
import static org.wildfly.extension.messaging.activemq.ha.HAPolicyConfigurationBuilder.addHAPolicyConfiguration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import javax.management.MBeanServer;
import javax.sql.DataSource;

import org.apache.activemq.artemis.api.config.ActiveMQDefaultConfiguration;
import org.apache.activemq.artemis.api.core.BroadcastGroupConfiguration;
import org.apache.activemq.artemis.api.core.DiscoveryGroupConfiguration;
import org.apache.activemq.artemis.api.core.Interceptor;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.core.config.BridgeConfiguration;
import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.config.storage.DatabaseStorageConfiguration;
import org.apache.activemq.artemis.core.security.Role;
import org.apache.activemq.artemis.core.server.JournalType;
import org.apache.activemq.artemis.core.settings.impl.AddressSettings;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
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
import org.jboss.as.security.plugins.SecurityDomainContext;
import org.jboss.as.security.service.SecurityBootstrapService;
import org.jboss.as.security.service.SecurityDomainService;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.clustering.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.spi.ClusteringDefaultRequirement;
import org.wildfly.clustering.spi.ClusteringRequirement;
import org.wildfly.common.function.ExceptionSupplier;
import org.wildfly.extension.messaging.activemq.jms.JMSService;
import org.wildfly.extension.messaging.activemq.logging.MessagingLogger;
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
    public static final ServerAdd INSTANCE = new ServerAdd();

// Artemis-specific system properties
    private static final String ARTEMIS_BROKER_CONFIG_NODEMANAGER_STORE_TABLE_NAME = "brokerconfig.storeConfiguration.nodeManagerStoreTableName";
    private static final String ARTEMIS_BROKER_CONFIG_JBDC_LOCK_RENEW_PERIOD_MILLIS = "brokerconfig.storeConfiguration.jdbcLockRenewPeriodMillis";
    private static final String ARTEMIS_BROKER_CONFIG_JBDC_LOCK_EXPIRATION_MILLIS = "brokerconfig.storeConfiguration.jdbcLockExpirationMillis";
    private static final String ARTEMIS_BROKER_CONFIG_JDBC_LOCK_ACQUISITION_TIMEOUT_MILLIS = "brokerconfig.storeConfiguration.jdbcLockAcquisitionTimeoutMillis";

    private ServerAdd() {
        super(ACTIVEMQ_SERVER_CAPABILITY, ServerDefinition.ATTRIBUTES);
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
        for(AttributeDefinition attr : attrs) {
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
        context.addStep(new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                final ServiceTarget serviceTarget = context.getServiceTarget();

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
                final ServiceBuilder serviceBuilder = serviceTarget.addService(activeMQServiceName);
                Supplier pathManager = serviceBuilder.requires(context.getCapabilityServiceName(PATH_MANAGER_CAPABILITY, PathManager.class));

                Optional<Supplier<DataSource>> dataSource = Optional.empty();
                ModelNode dataSourceModel = JOURNAL_DATASOURCE.resolveModelAttribute(context, model);
                if (dataSourceModel.isDefined()) {
                    ServiceName dataSourceCapability = context.getCapabilityServiceName(DATA_SOURCE_CAPABILITY, dataSourceModel.asString(), DataSource.class);
                    dataSource = Optional.of(serviceBuilder.requires(dataSourceCapability));
                }
                Optional<Supplier<MBeanServer>> mbeanServer = Optional.empty();
                if (context.hasOptionalCapability(JMX_CAPABILITY, ACTIVEMQ_SERVER_CAPABILITY.getDynamicName(serverName), null)) {
                    ServiceName jmxCapability = context.getCapabilityServiceName(JMX_CAPABILITY, MBeanServer.class);
                    mbeanServer = Optional.of(serviceBuilder.requires(jmxCapability));
                }

                // Inject a reference to the Elytron security domain if one has been defined.
                Optional<Supplier<SecurityDomain>> elytronSecurityDomain = Optional.empty();
                // legacy security
                Optional<Supplier<SecurityDomainContext>> securityDomainContext = Optional.empty();
                final ModelNode elytronSecurityDomainModel = ELYTRON_DOMAIN.resolveModelAttribute(context, model);
                if (elytronSecurityDomainModel.isDefined()) {
                    ServiceName elytronDomainCapability = context.getCapabilityServiceName(ELYTRON_DOMAIN_CAPABILITY, elytronSecurityDomainModel.asString(), SecurityDomain.class);
                    elytronSecurityDomain = Optional.of(serviceBuilder.requires(elytronDomainCapability));
                } else {
                    // Add legacy security
                    String domain = SECURITY_DOMAIN.resolveModelAttribute(context, model).asString();
                    securityDomainContext = Optional.of(serviceBuilder.requires(SecurityDomainService.SERVICE_NAME.append(domain)));
                    // WFLY-6652 / WFLY-10292 this dependency ensures that Artemis will be able to destroy any queues created on behalf of a
                    // pooled-connection-factory client during server stop
                    serviceBuilder.requires(SecurityBootstrapService.SERVICE_NAME);
                }

                List<Interceptor> incomingInterceptors = processInterceptors(INCOMING_INTERCEPTORS.resolveModelAttribute(context, operation));
                List<Interceptor> outgoingInterceptors = processInterceptors(OUTGOING_INTERCEPTORS.resolveModelAttribute(context, operation));

                // Process acceptors and connectors
                final Set<String> socketBindingNames = new HashSet<String>();
                TransportConfigOperationHandlers.processAcceptors(context, configuration, model, socketBindingNames);

                Map<String, Supplier<SocketBinding>> socketBindings = new HashMap<>();
                for (final String socketBindingName : socketBindingNames) {
                    Supplier<SocketBinding> socketBinding = serviceBuilder.requires(SocketBinding.JBOSS_BINDING_NAME.append(socketBindingName));
                    socketBindings.put(socketBindingName, socketBinding);
                }

                final Set<String> connectorsSocketBindings = new HashSet<String>();
                TransportConfigOperationHandlers.processConnectors(context, configuration, model, connectorsSocketBindings);

                Map<String, Supplier<OutboundSocketBinding>> outboundSocketBindings = new HashMap<>();
                Map<String, Boolean> outbounds = TransportConfigOperationHandlers.listOutBoundSocketBinding(context, connectorsSocketBindings);
                for (final String connectorSocketBinding : connectorsSocketBindings) {
                    // find whether the connectorSocketBinding references a SocketBinding or an OutboundSocketBinding
                    if (outbounds.get(connectorSocketBinding)) {
                        final ServiceName outboundSocketName = OutboundSocketBinding.OUTBOUND_SOCKET_BINDING_BASE_SERVICE_NAME.append(connectorSocketBinding);
                        Supplier<OutboundSocketBinding> outboundSocketBinding = serviceBuilder.requires(outboundSocketName);
                        outboundSocketBindings.put(connectorSocketBinding, outboundSocketBinding);
                    } else {
                        // check if the socket binding has not already been added by the acceptors
                        if (!socketBindings.containsKey(connectorSocketBinding)) {
                            Supplier<SocketBinding> socketBinding = serviceBuilder.requires(SocketBinding.JBOSS_BINDING_NAME.append(connectorSocketBinding));
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
                    serviceBuilder.requires(MessagingServices.HTTP_UPGRADE_REGISTRY.append(httpListener));
                }

                //this requires connectors
                BroadcastGroupAdd.addBroadcastGroupConfigs(context, configuration, model);

                final List<BroadcastGroupConfiguration> broadcastGroupConfigurations = configuration.getBroadcastGroupConfigurations();
                final Map<String, DiscoveryGroupConfiguration> discoveryGroupConfigurations = configuration.getDiscoveryGroupConfigurations();

                final Map<String, String> clusterNames = new HashMap<>();
                final Map<String, Supplier<CommandDispatcherFactory>> commandDispatcherFactories = new HashMap<>();
                final Map<ServiceName, Supplier<CommandDispatcherFactory>> commandDispatcherFactoryServices = new HashMap<>();
                final Map<String, Supplier<SocketBinding>> groupBindings = new HashMap<>();
                final Map<ServiceName, Supplier<SocketBinding>> groupBindingServices = new HashMap<>();

                if(broadcastGroupConfigurations != null) {
                    for(final BroadcastGroupConfiguration config : broadcastGroupConfigurations) {
                        final String name = config.getName();
                        final String key = "broadcast" + name;
                        ModelNode broadcastGroupModel = model.get(BROADCAST_GROUP, name);

                        if (broadcastGroupModel.hasDefined(JGROUPS_CLUSTER.getName())) {
                            ModelNode channel = BroadcastGroupDefinition.JGROUPS_CHANNEL.resolveModelAttribute(context, broadcastGroupModel);
                            ServiceName commandDispatcherFactoryServiceName = channel.isDefined() ? ClusteringRequirement.COMMAND_DISPATCHER_FACTORY.getServiceName(context, channel.asString()) : ClusteringDefaultRequirement.COMMAND_DISPATCHER_FACTORY.getServiceName(context);
                            String clusterName = JGROUPS_CLUSTER.resolveModelAttribute(context, broadcastGroupModel).asString();
                            if (!commandDispatcherFactoryServices.containsKey(commandDispatcherFactoryServiceName)) {
                                Supplier<CommandDispatcherFactory> commandDispatcherFactory = serviceBuilder.requires(commandDispatcherFactoryServiceName);
                                commandDispatcherFactoryServices.put(commandDispatcherFactoryServiceName, commandDispatcherFactory);
                            }
                            commandDispatcherFactories.put(key, commandDispatcherFactoryServices.get(commandDispatcherFactoryServiceName));
                            clusterNames.put(key, clusterName);
                        } else {
                            final ServiceName groupBindingServiceName = GroupBindingService.getBroadcastBaseServiceName(activeMQServiceName).append(name);
                            if (!groupBindingServices.containsKey(groupBindingServiceName)) {
                                Supplier<SocketBinding> groupBinding = serviceBuilder.requires(groupBindingServiceName);
                                groupBindingServices.put(groupBindingServiceName, groupBinding) ;
                            }
                            groupBindings.put(key, groupBindingServices.get(groupBindingServiceName));
                        }
                    }
                }
                if(discoveryGroupConfigurations != null) {
                    for(final DiscoveryGroupConfiguration config : discoveryGroupConfigurations.values()) {
                        final String name = config.getName();
                        final String key = "discovery" + name;
                        ModelNode discoveryGroupModel = model.get(DISCOVERY_GROUP, name);
                        if (discoveryGroupModel.hasDefined(JGROUPS_CLUSTER.getName())) {
                            ModelNode channel = DiscoveryGroupDefinition.JGROUPS_CHANNEL.resolveModelAttribute(context, discoveryGroupModel);
                            ServiceName commandDispatcherFactoryServiceName = channel.isDefined() ? ClusteringRequirement.COMMAND_DISPATCHER_FACTORY.getServiceName(context, channel.asString()) : ClusteringDefaultRequirement.COMMAND_DISPATCHER_FACTORY.getServiceName(context);
                            String clusterName = JGROUPS_CLUSTER.resolveModelAttribute(context, discoveryGroupModel).asString();
                            if (!commandDispatcherFactoryServices.containsKey(commandDispatcherFactoryServiceName)) {
                                Supplier<CommandDispatcherFactory> commandDispatcherFactory = serviceBuilder.requires(commandDispatcherFactoryServiceName);
                                commandDispatcherFactoryServices.put(commandDispatcherFactoryServiceName, commandDispatcherFactory);
                            }
                            commandDispatcherFactories.put(key, commandDispatcherFactoryServices.get(commandDispatcherFactoryServiceName));
                            clusterNames.put(key, clusterName);
                        } else {
                            final ServiceName groupBindingServiceName = GroupBindingService.getDiscoveryBaseServiceName(activeMQServiceName).append(name);
                            if (!groupBindingServices.containsKey(groupBindingServiceName)) {
                                Supplier<SocketBinding> groupBinding = serviceBuilder.requires(groupBindingServiceName);
                                groupBindingServices.put(groupBindingServiceName, groupBinding) ;
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
                        securityDomainContext,
                        mbeanServer,
                        dataSource
                );

                // inject credential-references for bridges
                addBridgeCredentialStoreReference(serverService, configuration, BridgeDefinition.CREDENTIAL_REFERENCE, context, model, serviceBuilder);
                addClusterCredentialStoreReference(serverService, ServerDefinition.CREDENTIAL_REFERENCE, context, model, serviceBuilder);

                // Install the ActiveMQ Service
                ServiceController activeMQServerServiceController = serviceBuilder.setInstance(serverService)
                        .install();
                // Provide our custom Resource impl a ref to the ActiveMQ server so it can create child runtime resources
                ((ActiveMQServerResource)resource).setActiveMQServerServiceController(activeMQServerServiceController);

                // Install the JMSService
                boolean overrideInVMSecurity = OVERRIDE_IN_VM_SECURITY.resolveModelAttribute(context, operation).asBoolean();
                JMSService.addService(serviceTarget, activeMQServiceName, overrideInVMSecurity);

                context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
            }
        }, OperationContext.Stage.RUNTIME);
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

        configuration.setEnabledAsyncConnectionExecution(ASYNC_CONNECTION_EXECUTION_ENABLED.resolveModelAttribute(context, model).asBoolean());

        configuration.setClusterPassword(CLUSTER_PASSWORD.resolveModelAttribute(context, model).asString());
        configuration.setClusterUser(CLUSTER_USER.resolveModelAttribute(context, model).asString());
        configuration.setConnectionTTLOverride(CONNECTION_TTL_OVERRIDE.resolveModelAttribute(context, model).asInt());
        configuration.setCreateBindingsDir(CREATE_BINDINGS_DIR.resolveModelAttribute(context, model).asBoolean());
        configuration.setCreateJournalDir(CREATE_JOURNAL_DIR.resolveModelAttribute(context, model).asBoolean());

        configuration.setIDCacheSize(ID_CACHE_SIZE.resolveModelAttribute(context, model).asInt());
        // TODO do we want to allow the jmx configuration ?
        configuration.setJMXDomain(JMX_DOMAIN.resolveModelAttribute(context, model).asString());
        configuration.setJMXManagementEnabled(JMX_MANAGEMENT_ENABLED.resolveModelAttribute(context, model).asBoolean());
        // Journal
        final JournalType journalType = JournalType.valueOf(JOURNAL_TYPE.resolveModelAttribute(context, model).asString());
        configuration.setJournalType(journalType);

        // AIO Journal
        configuration.setJournalBufferSize_AIO(JOURNAL_BUFFER_SIZE.resolveModelAttribute(context, model).asInt(ActiveMQDefaultConfiguration.getDefaultJournalBufferSizeAio()));
        configuration.setJournalBufferTimeout_AIO(JOURNAL_BUFFER_TIMEOUT.resolveModelAttribute(context, model).asInt(ActiveMQDefaultConfiguration.getDefaultJournalBufferTimeoutAio()));
        configuration.setJournalMaxIO_AIO(JOURNAL_MAX_IO.resolveModelAttribute(context, model).asInt(ActiveMQDefaultConfiguration.getDefaultJournalMaxIoAio()));
        // NIO Journal
        configuration.setJournalBufferSize_NIO(JOURNAL_BUFFER_SIZE.resolveModelAttribute(context, model).asInt(ActiveMQDefaultConfiguration.getDefaultJournalBufferSizeNio()));
        configuration.setJournalBufferTimeout_NIO(JOURNAL_BUFFER_TIMEOUT.resolveModelAttribute(context, model).asInt(ActiveMQDefaultConfiguration.getDefaultJournalBufferTimeoutNio()));
        configuration.setJournalMaxIO_NIO(JOURNAL_MAX_IO.resolveModelAttribute(context, model).asInt(ActiveMQDefaultConfiguration.getDefaultJournalMaxIoNio()));
        //
        configuration.setJournalCompactMinFiles(JOURNAL_COMPACT_MIN_FILES.resolveModelAttribute(context, model).asInt());
        configuration.setJournalCompactPercentage(JOURNAL_COMPACT_PERCENTAGE.resolveModelAttribute(context, model).asInt());
        configuration.setJournalFileSize(JOURNAL_FILE_SIZE.resolveModelAttribute(context, model).asInt());
        configuration.setJournalMinFiles(JOURNAL_MIN_FILES.resolveModelAttribute(context, model).asInt());
        configuration.setJournalPoolFiles(JOURNAL_POOL_FILES.resolveModelAttribute(context, model).asInt());
        configuration.setJournalSyncNonTransactional(JOURNAL_SYNC_NON_TRANSACTIONAL.resolveModelAttribute(context, model).asBoolean());
        configuration.setJournalSyncTransactional(JOURNAL_SYNC_TRANSACTIONAL.resolveModelAttribute(context, model).asBoolean());
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
        configuration.setWildcardRoutingEnabled(WILD_CARD_ROUTING_ENABLED.resolveModelAttribute(context, model).asBoolean());

        processStorageConfiguration(context, model, configuration);
        addHAPolicyConfiguration(context, configuration, model);

        processAddressSettings(context, configuration, model);
        processSecuritySettings(context, configuration, model);

        // Add in items from child resources
        GroupingHandlerAdd.addGroupingHandlerConfig(context,configuration, model);
        DiscoveryGroupAdd.addDiscoveryGroupConfigs(context, configuration, model);
        DivertAdd.addDivertConfigs(context, configuration, model);
        QueueAdd.addQueueConfigs(context, configuration, model);
        BridgeAdd.addBridgeConfigs(context, configuration, model);
        ClusterConnectionAdd.addClusterConnectionConfigs(context, configuration, model);
        ConnectorServiceDefinition.addConnectorServiceConfigs(context, configuration, model);

        return configuration;
    }

    private static void processStorageConfiguration(OperationContext context, ModelNode model, Configuration configuration) throws OperationFailedException {
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
        storageConfiguration.setJdbcNetworkTimeout((int)networkTimeout);
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
        } else if ( org.wildfly.security.manager.WildFlySecurityManager.getSystemPropertiesPrivileged().containsKey(ARTEMIS_BROKER_CONFIG_NODEMANAGER_STORE_TABLE_NAME)) {
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
        } else if ( org.wildfly.security.manager.WildFlySecurityManager.getSystemPropertiesPrivileged().containsKey(ARTEMIS_BROKER_CONFIG_JBDC_LOCK_EXPIRATION_MILLIS)) {
            lockExpirationInMillis = Long.parseLong( org.wildfly.security.manager.WildFlySecurityManager.getSystemPropertiesPrivileged().getProperty(ARTEMIS_BROKER_CONFIG_JBDC_LOCK_EXPIRATION_MILLIS));
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
            lockRenewPeriodInMillis = Long.parseLong( org.wildfly.security.manager.WildFlySecurityManager.getSystemPropertiesPrivileged().getProperty(ARTEMIS_BROKER_CONFIG_JBDC_LOCK_RENEW_PERIOD_MILLIS));
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
        ModelNode databaseNode = JOURNAL_DATABASE.resolveModelAttribute(context, model);
        final String database = databaseNode.isDefined() ? databaseNode.asString() : null;
        try {
            storageConfiguration.setSqlProvider(new PropertySQLProviderFactory(database));
        } catch (IOException e) {
            throw new OperationFailedException(e);
        }
        configuration.setStoreConfiguration(storageConfiguration);
    }

    /**
     * Process the address settings.
     *
     * @param configuration the ActiveMQ configuration
     * @param params        the detyped operation parameters
     */
    /**
     * Process the address settings.
     *
     * @param configuration the ActiveMQ configuration
     * @param params        the detyped operation parameters
     * @throws org.jboss.as.controller.OperationFailedException
     */
    static void processAddressSettings(final OperationContext context, final Configuration configuration, final ModelNode params) throws OperationFailedException {
        if (params.hasDefined(ADDRESS_SETTING)) {
            for (final Property property : params.get(ADDRESS_SETTING).asPropertyList()) {
                final String match = property.getName();
                final ModelNode config = property.getValue();
                final AddressSettings settings = AddressSettingAdd.createSettings(context, config);
                configuration.getAddressesSettings().put(match, settings);
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

    private static Class unwrapClass(ModelNode classModel) throws OperationFailedException {
        String className = classModel.get(NAME).asString();
        String moduleName = classModel.get(MODULE).asString();
        try {
            ModuleIdentifier moduleID = ModuleIdentifier.fromString(moduleName);
            Module module = Module.getCallerModuleLoader().loadModule(moduleID);
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
     * @param params        the detyped operation parameters
     */
    static void processSecuritySettings(final OperationContext context, final Configuration configuration, final ModelNode params) throws OperationFailedException {
        if (params.get(SECURITY_SETTING).isDefined()) {
            for (final Property property : params.get(SECURITY_SETTING).asPropertyList()) {
                final String match = property.getName();
                final ModelNode config = property.getValue();

                if(config.hasDefined(CommonAttributes.ROLE)) {
                    final Set<Role> roles = new HashSet<Role>();
                    for (final Property role : config.get(CommonAttributes.ROLE).asPropertyList()) {
                        roles.add(SecurityRoleDefinition.transform(context, role.getName(), role.getValue()));
                    }
                    configuration.getSecurityRoles().put(match, roles);
                }
            }
        }
    }

    private static void addBridgeCredentialStoreReference(ActiveMQServerService amqService, Configuration configuration, ObjectTypeAttributeDefinition credentialReferenceAttributeDefinition, OperationContext context, ModelNode model, ServiceBuilder<?> serviceBuilder) throws OperationFailedException {
        for (BridgeConfiguration bridgeConfiguration: configuration.getBridgeConfigurations()) {
            String name = bridgeConfiguration.getName();
            InjectedValue<ExceptionSupplier<CredentialSource, Exception>> injector = amqService.getBridgeCredentialSourceSupplierInjector(name);

            String[] modelFilter = { CommonAttributes.BRIDGE, name };

            ModelNode filteredModelNode = model;
            if (modelFilter != null && modelFilter.length > 0) {
                for (String path : modelFilter) {
                    if (filteredModelNode.get(path).isDefined())
                        filteredModelNode = filteredModelNode.get(path);
                    else
                        break;
                }
            }
            ModelNode value = credentialReferenceAttributeDefinition.resolveModelAttribute(context, filteredModelNode);
            if (value.isDefined()) {
                injector.inject(CredentialReference.getCredentialSourceSupplier(context, credentialReferenceAttributeDefinition, filteredModelNode, serviceBuilder));
            }
        }
    }

    private static void addClusterCredentialStoreReference(ActiveMQServerService amqService, ObjectTypeAttributeDefinition credentialReferenceAttributeDefinition, OperationContext context, ModelNode model, ServiceBuilder<?> serviceBuilder) throws OperationFailedException {
        ModelNode value = credentialReferenceAttributeDefinition.resolveModelAttribute(context, model);
        if (value.isDefined()) {
            amqService.getClusterCredentialSourceSupplierInjector()
                    .inject(CredentialReference.getCredentialSourceSupplier(context, credentialReferenceAttributeDefinition, model, serviceBuilder));
        }
    }
}

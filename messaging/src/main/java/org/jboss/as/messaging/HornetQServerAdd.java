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

package org.jboss.as.messaging;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.MBeanServer;

import org.hornetq.api.core.DiscoveryGroupConfiguration;
import org.hornetq.api.core.SimpleString;
import org.hornetq.core.config.BroadcastGroupConfiguration;
import org.hornetq.core.config.Configuration;
import org.hornetq.core.config.impl.ConfigurationImpl;
import org.hornetq.core.security.Role;
import org.hornetq.core.server.HornetQServer;
import org.hornetq.core.server.JournalType;
import org.hornetq.core.settings.impl.AddressSettings;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.messaging.jms.JMSService;
import org.jboss.as.network.OutboundSocketBinding;
import org.jboss.as.network.SocketBinding;
import org.jboss.as.server.services.path.RelativePathService;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceBuilder.DependencyType;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceListener;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RELATIVE_TO;
import static org.jboss.as.messaging.CommonAttributes.ADDRESS_SETTING;
import static org.jboss.as.messaging.CommonAttributes.ALLOW_FAILBACK;
import static org.jboss.as.messaging.CommonAttributes.ASYNC_CONNECTION_EXECUTION_ENABLED;
import static org.jboss.as.messaging.CommonAttributes.BACKUP;
import static org.jboss.as.messaging.CommonAttributes.BINDINGS_DIRECTORY;
import static org.jboss.as.messaging.CommonAttributes.CLUSTERED;
import static org.jboss.as.messaging.CommonAttributes.CLUSTER_PASSWORD;
import static org.jboss.as.messaging.CommonAttributes.CLUSTER_USER;
import static org.jboss.as.messaging.CommonAttributes.CONNECTION_FACTORY;
import static org.jboss.as.messaging.CommonAttributes.CONNECTION_TTL_OVERRIDE;
import static org.jboss.as.messaging.CommonAttributes.CREATE_BINDINGS_DIR;
import static org.jboss.as.messaging.CommonAttributes.CREATE_JOURNAL_DIR;
import static org.jboss.as.messaging.CommonAttributes.FAILBACK_DELAY;
import static org.jboss.as.messaging.CommonAttributes.FAILOVER_ON_SHUTDOWN;
import static org.jboss.as.messaging.CommonAttributes.ID_CACHE_SIZE;
import static org.jboss.as.messaging.CommonAttributes.JMS_QUEUE;
import static org.jboss.as.messaging.CommonAttributes.JMS_TOPIC;
import static org.jboss.as.messaging.CommonAttributes.JMX_DOMAIN;
import static org.jboss.as.messaging.CommonAttributes.JMX_MANAGEMENT_ENABLED;
import static org.jboss.as.messaging.CommonAttributes.JOURNAL_BUFFER_SIZE;
import static org.jboss.as.messaging.CommonAttributes.JOURNAL_BUFFER_TIMEOUT;
import static org.jboss.as.messaging.CommonAttributes.JOURNAL_COMPACT_MIN_FILES;
import static org.jboss.as.messaging.CommonAttributes.JOURNAL_COMPACT_PERCENTAGE;
import static org.jboss.as.messaging.CommonAttributes.JOURNAL_DIRECTORY;
import static org.jboss.as.messaging.CommonAttributes.JOURNAL_FILE_SIZE;
import static org.jboss.as.messaging.CommonAttributes.JOURNAL_MAX_IO;
import static org.jboss.as.messaging.CommonAttributes.JOURNAL_MIN_FILES;
import static org.jboss.as.messaging.CommonAttributes.JOURNAL_SYNC_NON_TRANSACTIONAL;
import static org.jboss.as.messaging.CommonAttributes.JOURNAL_SYNC_TRANSACTIONAL;
import static org.jboss.as.messaging.CommonAttributes.JOURNAL_TYPE;
import static org.jboss.as.messaging.CommonAttributes.LARGE_MESSAGES_DIRECTORY;
import static org.jboss.as.messaging.CommonAttributes.LIVE_CONNECTOR_REF;
import static org.jboss.as.messaging.CommonAttributes.LOG_JOURNAL_WRITE_RATE;
import static org.jboss.as.messaging.CommonAttributes.MANAGEMENT_ADDRESS;
import static org.jboss.as.messaging.CommonAttributes.MANAGEMENT_NOTIFICATION_ADDRESS;
import static org.jboss.as.messaging.CommonAttributes.MEMORY_MEASURE_INTERVAL;
import static org.jboss.as.messaging.CommonAttributes.MEMORY_WARNING_THRESHOLD;
import static org.jboss.as.messaging.CommonAttributes.MESSAGE_COUNTER_ENABLED;
import static org.jboss.as.messaging.CommonAttributes.MESSAGE_COUNTER_MAX_DAY_HISTORY;
import static org.jboss.as.messaging.CommonAttributes.MESSAGE_COUNTER_SAMPLE_PERIOD;
import static org.jboss.as.messaging.CommonAttributes.MESSAGE_EXPIRY_SCAN_PERIOD;
import static org.jboss.as.messaging.CommonAttributes.MESSAGE_EXPIRY_THREAD_PRIORITY;
import static org.jboss.as.messaging.CommonAttributes.PAGING_DIRECTORY;
import static org.jboss.as.messaging.CommonAttributes.PERF_BLAST_PAGES;
import static org.jboss.as.messaging.CommonAttributes.PERSISTENCE_ENABLED;
import static org.jboss.as.messaging.CommonAttributes.PERSIST_DELIVERY_COUNT_BEFORE_DELIVERY;
import static org.jboss.as.messaging.CommonAttributes.PERSIST_ID_CACHE;
import static org.jboss.as.messaging.CommonAttributes.POOLED_CONNECTION_FACTORY;
import static org.jboss.as.messaging.CommonAttributes.QUEUE;
import static org.jboss.as.messaging.CommonAttributes.RUN_SYNC_SPEED_TEST;
import static org.jboss.as.messaging.CommonAttributes.SCHEDULED_THREAD_POOL_MAX_SIZE;
import static org.jboss.as.messaging.CommonAttributes.SECURITY_DOMAIN;
import static org.jboss.as.messaging.CommonAttributes.SECURITY_ENABLED;
import static org.jboss.as.messaging.CommonAttributes.SECURITY_INVALIDATION_INTERVAL;
import static org.jboss.as.messaging.CommonAttributes.SECURITY_SETTING;
import static org.jboss.as.messaging.CommonAttributes.SERVER_DUMP_INTERVAL;
import static org.jboss.as.messaging.CommonAttributes.SHARED_STORE;
import static org.jboss.as.messaging.CommonAttributes.THREAD_POOL_MAX_SIZE;
import static org.jboss.as.messaging.CommonAttributes.TRANSACTION_TIMEOUT;
import static org.jboss.as.messaging.CommonAttributes.TRANSACTION_TIMEOUT_SCAN_PERIOD;
import static org.jboss.as.messaging.CommonAttributes.WILD_CARD_ROUTING_ENABLED;

import org.jboss.as.security.plugins.SecurityDomainContext;
import org.jboss.as.security.service.SecurityDomainService;

/**
 * Add handler for a HornetQ server instance.
 *
 * @author Emanuel Muckenhuber
 * @author <a href="mailto:andy.taylor@jboss.com">Andy Taylor</a>
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
class HornetQServerAdd implements OperationStepHandler {

    private static final String DEFAULT_PATH = "messaging";
    private static final String DEFAULT_RELATIVE_TO = "jboss.server.data.dir";
    static final String PATH_BASE = "paths";

    static final String DEFAULT_BINDINGS_DIR = "bindings";
    static final String DEFAULT_JOURNAL_DIR = "journal";
    static final String DEFAULT_LARGE_MESSSAGE_DIR = "largemessages";
    static final String DEFAULT_PAGING_DIR = "paging";

    public static final HornetQServerAdd INSTANCE = new HornetQServerAdd();

    private HornetQServerAdd() {
    }

    /** {@inheritDoc */
    public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {

        // We use a custom Resource impl so we can expose runtime HQ components (e.g. AddressControl) as child resources
        final HornetQServerResource resource = new HornetQServerResource();
        context.addResource(PathAddress.EMPTY_ADDRESS, resource);
        final ModelNode model = resource.getModel();

        for (final AttributeDefinition attributeDefinition : CommonAttributes.SIMPLE_ROOT_RESOURCE_ATTRIBUTES) {
            attributeDefinition.validateAndSet(operation, model);
        }

        model.get(QUEUE).setEmptyObject();
        model.get(CONNECTION_FACTORY).setEmptyObject();
        model.get(JMS_QUEUE).setEmptyObject();
        model.get(JMS_TOPIC).setEmptyObject();
        model.get(POOLED_CONNECTION_FACTORY).setEmptyObject();

        if (context.isNormalServer()) {
            context.addStep(new OperationStepHandler() {
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                    final List<ServiceController<?>> controllers = new ArrayList<ServiceController<?>>();
                    final ServiceVerificationHandler verificationHandler = new ServiceVerificationHandler();
                    performRuntime(context, resource, verificationHandler, controllers);

                    context.addStep(verificationHandler, OperationContext.Stage.VERIFY);

                    if (context.completeStep() == OperationContext.ResultAction.ROLLBACK) {
                        for(ServiceController<?> controller : controllers) {
                            context.removeService(controller.getName());
                        }
                    }
                }
            }, OperationContext.Stage.RUNTIME);
        }
        context.completeStep();
    }

    private void performRuntime(final OperationContext context, final HornetQServerResource resource,
                                  final ServiceVerificationHandler verificationHandler,
                                  final List<ServiceController<?>> newControllers) throws OperationFailedException {
        // Add a RUNTIME step to actually install the HQ Service. This will execute after the runtime step
        // added by any child resources whose ADD handler executes after this one in the model stage.
        context.addStep(new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                final ServiceTarget serviceTarget = context.getServiceTarget();

                final String serverName = PathAddress.pathAddress(operation.require(OP_ADDR)).getLastElement().getValue();

                // Transform the configuration based on the recursive model
                final ModelNode model = Resource.Tools.readModel(resource);
                final Configuration configuration = transformConfig(context, serverName, model);

                // Create path services

                final ServiceName bindingsPath = createDirectoryService(DEFAULT_BINDINGS_DIR, model.get(PATH, BINDINGS_DIRECTORY), serviceTarget, operation, newControllers, verificationHandler);
                final ServiceName journalPath = createDirectoryService(DEFAULT_JOURNAL_DIR, model.get(PATH, JOURNAL_DIRECTORY), serviceTarget, operation, newControllers, verificationHandler);
                final ServiceName largeMessagePath = createDirectoryService(DEFAULT_LARGE_MESSSAGE_DIR, model.get(PATH, LARGE_MESSAGES_DIRECTORY), serviceTarget, operation, newControllers, verificationHandler);
                final ServiceName pagingPath = createDirectoryService(DEFAULT_PAGING_DIR, model.get(PATH, PAGING_DIRECTORY), serviceTarget, operation, newControllers, verificationHandler);

                // Create the HornetQ Service
                final HornetQService hqService = new HornetQService();
                hqService.setConfiguration(configuration);

                // Add the HornetQ Service
                ServiceName hqServiceName = MessagingServices.getHornetQServiceName(serverName);
                final ServiceBuilder<HornetQServer> serviceBuilder = serviceTarget.addService(hqServiceName, hqService)
                        .addDependency(DependencyType.OPTIONAL, ServiceName.JBOSS.append("mbean", "server"), MBeanServer.class, hqService.getMBeanServer());

                serviceBuilder.addDependency(bindingsPath, String.class, hqService.getPathInjector(DEFAULT_BINDINGS_DIR));
                serviceBuilder.addDependency(journalPath, String.class, hqService.getPathInjector(DEFAULT_JOURNAL_DIR));
                serviceBuilder.addDependency(largeMessagePath, String.class, hqService.getPathInjector(DEFAULT_LARGE_MESSSAGE_DIR));
                serviceBuilder.addDependency(pagingPath, String.class, hqService.getPathInjector(DEFAULT_PAGING_DIR));

                // Add security
                String domain = SECURITY_DOMAIN.resolveModelAttribute(context, model).asString();
                serviceBuilder.addDependency(DependencyType.REQUIRED,
                        SecurityDomainService.SERVICE_NAME.append(domain),
                        SecurityDomainContext.class,
                        hqService.getSecurityDomainContextInjector());

                // Process acceptors and connectors
                final Set<String> socketBindings = new HashSet<String>();
                TransportConfigOperationHandlers.processAcceptors(configuration, model, socketBindings);

                for (final String socketBinding : socketBindings) {
                    final ServiceName socketName = SocketBinding.JBOSS_BINDING_NAME.append(socketBinding);
                    serviceBuilder.addDependency(socketName, SocketBinding.class, hqService.getSocketBindingInjector(socketBinding));
                }

                final Set<String> outboundSocketBindings = new HashSet<String>();
                TransportConfigOperationHandlers.processConnectors(configuration, model, outboundSocketBindings);
                for (final String outboundSocketBinding : outboundSocketBindings) {
                    final ServiceName outboundSocketName = OutboundSocketBinding.OUTBOUND_SOCKET_BINDING_BASE_SERVICE_NAME.append(outboundSocketBinding);
                    // Optional dependency so it won't fail if the user used a ref to socket-binding instead of
                    // outgoing-socket-binding
                    serviceBuilder.addDependency(DependencyType.OPTIONAL, outboundSocketName, OutboundSocketBinding.class,
                            hqService.getOutboundSocketBindingInjector(outboundSocketBinding));
                    if (!socketBindings.contains(outboundSocketBinding)) {
                        // Add a dependency on the regular socket binding as well so users don't have to use
                        // outgoing-socket-binding to configure a ref to the local server socket
                        final ServiceName socketName = SocketBinding.JBOSS_BINDING_NAME.append(outboundSocketBinding);
                        serviceBuilder.addDependency(DependencyType.OPTIONAL, socketName, SocketBinding.class,
                                hqService.getSocketBindingInjector(outboundSocketBinding));
                    }
                }

                final List<BroadcastGroupConfiguration> broadcastGroupConfigurations = configuration.getBroadcastGroupConfigurations();
                final Map<String, DiscoveryGroupConfiguration> discoveryGroupConfigurations = configuration.getDiscoveryGroupConfigurations();

                if(broadcastGroupConfigurations != null) {
                    for(final BroadcastGroupConfiguration config : broadcastGroupConfigurations) {
                        final String name = config.getName();
                        final ServiceName groupBinding = GroupBindingService.getBroadcastBaseServiceName(hqServiceName).append(name);
                        serviceBuilder.addDependency(groupBinding, SocketBinding.class, hqService.getGroupBindingInjector("broadcast" + name));
                    }
                }
                if(discoveryGroupConfigurations != null) {
                    for(final DiscoveryGroupConfiguration config : discoveryGroupConfigurations.values()) {
                        final String name = config.getName();
                        final ServiceName groupBinding = GroupBindingService.getDiscoveryBaseServiceName(hqServiceName).append(name);
                        serviceBuilder.addDependency(groupBinding, SocketBinding.class, hqService.getGroupBindingInjector("discovery" + name));
                    }
                }

                serviceBuilder.addListener(verificationHandler);

                // Install the HornetQ Service
                ServiceController<HornetQServer> hqServerServiceController = serviceBuilder.install();
                // Provide our custom Resource impl a ref to the HornetQServer so it can create child runtime resources
                resource.setHornetQServerServiceController(hqServerServiceController);

                newControllers.add(hqServerServiceController);
                newControllers.add(JMSService.addService(serviceTarget, hqServiceName, verificationHandler));

                context.completeStep();
            }
        }, OperationContext.Stage.RUNTIME);
    }

    /**
     * Transform the detyped operation parameters into the hornetQ configuration.
     *
     * @param context the operation context
     * @param serverName the name of the HornetQServer instance
     * @param model the subsystem root resource model
     * @return the hornetQ configuration
     */
    private Configuration transformConfig(final OperationContext context, String serverName, final ModelNode model) throws OperationFailedException {

        Configuration configuration = new ConfigurationImpl();

        configuration.setName(serverName);

        // --
        configuration.setAllowAutoFailBack(ALLOW_FAILBACK.resolveModelAttribute(context, model).asBoolean());
        configuration.setEnabledAsyncConnectionExecution(ASYNC_CONNECTION_EXECUTION_ENABLED.resolveModelAttribute(context, model).asBoolean());

        configuration.setBackup(BACKUP.resolveModelAttribute(context, model).asBoolean());
        if(model.hasDefined(LIVE_CONNECTOR_REF.getName())) {
            configuration.setLiveConnectorName(LIVE_CONNECTOR_REF.resolveModelAttribute(context, model).asString());
        }
        configuration.setClustered(CLUSTERED.resolveModelAttribute(context, model).asBoolean());
        configuration.setClusterPassword(CLUSTER_PASSWORD.resolveModelAttribute(context, model).asString());
        configuration.setClusterUser(CLUSTER_USER.resolveModelAttribute(context, model).asString());
        configuration.setConnectionTTLOverride(CONNECTION_TTL_OVERRIDE.resolveModelAttribute(context, model).asInt());
        configuration.setCreateBindingsDir(CREATE_BINDINGS_DIR.resolveModelAttribute(context, model).asBoolean());
        configuration.setCreateJournalDir(CREATE_JOURNAL_DIR.resolveModelAttribute(context, model).asBoolean());
        configuration.setFailbackDelay(FAILBACK_DELAY.resolveModelAttribute(context, model).asLong());
        configuration.setFailoverOnServerShutdown(FAILOVER_ON_SHUTDOWN.resolveModelAttribute(context, model).asBoolean());

        configuration.setIDCacheSize(ID_CACHE_SIZE.resolveModelAttribute(context, model).asInt());
        // TODO do we want to allow the jmx configuration ?
        configuration.setJMXDomain(JMX_DOMAIN.resolveModelAttribute(context, model).asString());
        configuration.setJMXManagementEnabled(JMX_MANAGEMENT_ENABLED.resolveModelAttribute(context, model).asBoolean());
        // Journal
        final JournalType journalType = JournalType.valueOf(JOURNAL_TYPE.resolveModelAttribute(context, model).asString());
        configuration.setJournalType(journalType);

        // AIO Journal
        configuration.setJournalBufferSize_AIO(JOURNAL_BUFFER_SIZE.resolveModelAttribute(context, model).asInt(ConfigurationImpl.DEFAULT_JOURNAL_BUFFER_SIZE_AIO));
        configuration.setJournalBufferTimeout_AIO(JOURNAL_BUFFER_TIMEOUT.resolveModelAttribute(context, model).asInt(ConfigurationImpl.DEFAULT_JOURNAL_BUFFER_TIMEOUT_AIO));
        configuration.setJournalMaxIO_AIO(JOURNAL_MAX_IO.resolveModelAttribute(context, model).asInt(ConfigurationImpl.DEFAULT_JOURNAL_MAX_IO_AIO));
        // NIO Journal
        configuration.setJournalBufferSize_NIO(JOURNAL_BUFFER_SIZE.resolveModelAttribute(context, model).asInt(ConfigurationImpl.DEFAULT_JOURNAL_BUFFER_SIZE_NIO));
        configuration.setJournalBufferTimeout_NIO(JOURNAL_BUFFER_TIMEOUT.resolveModelAttribute(context, model).asInt(ConfigurationImpl.DEFAULT_JOURNAL_BUFFER_TIMEOUT_NIO));
        configuration.setJournalMaxIO_NIO(JOURNAL_MAX_IO.resolveModelAttribute(context, model).asInt(ConfigurationImpl.DEFAULT_JOURNAL_MAX_IO_NIO));
        //
        configuration.setJournalCompactMinFiles(JOURNAL_COMPACT_MIN_FILES.resolveModelAttribute(context, model).asInt());
        configuration.setJournalCompactPercentage(JOURNAL_COMPACT_PERCENTAGE.resolveModelAttribute(context, model).asInt());
        configuration.setJournalFileSize(JOURNAL_FILE_SIZE.resolveModelAttribute(context, model).asInt());
        configuration.setJournalMinFiles(JOURNAL_MIN_FILES.resolveModelAttribute(context, model).asInt());
        configuration.setJournalSyncNonTransactional(JOURNAL_SYNC_NON_TRANSACTIONAL.resolveModelAttribute(context, model).asBoolean());
        configuration.setJournalSyncTransactional(JOURNAL_SYNC_TRANSACTIONAL.resolveModelAttribute(context, model).asBoolean());
        configuration.setLogJournalWriteRate(LOG_JOURNAL_WRITE_RATE.resolveModelAttribute(context, model).asBoolean());

        configuration.setManagementAddress(SimpleString.toSimpleString(MANAGEMENT_ADDRESS.resolveModelAttribute(context, model).asString()));
        configuration.setManagementNotificationAddress(SimpleString.toSimpleString(MANAGEMENT_NOTIFICATION_ADDRESS.resolveModelAttribute(context, model).asString()));

        configuration.setMemoryMeasureInterval(MEMORY_MEASURE_INTERVAL.resolveModelAttribute(context, model).asLong());
        configuration.setMemoryWarningThreshold(MEMORY_WARNING_THRESHOLD.resolveModelAttribute(context, model).asInt());

        configuration.setMessageCounterEnabled(MESSAGE_COUNTER_ENABLED.resolveModelAttribute(context, model).asBoolean());
        configuration.setMessageCounterSamplePeriod(MESSAGE_COUNTER_SAMPLE_PERIOD.resolveModelAttribute(context, model).asInt());
        configuration.setMessageCounterMaxDayHistory(MESSAGE_COUNTER_MAX_DAY_HISTORY.resolveModelAttribute(context, model).asInt());
        configuration.setMessageExpiryScanPeriod(MESSAGE_EXPIRY_SCAN_PERIOD.resolveModelAttribute(context, model).asLong());
        configuration.setMessageExpiryThreadPriority(MESSAGE_EXPIRY_THREAD_PRIORITY.resolveModelAttribute(context, model).asInt());

        configuration.setJournalPerfBlastPages(PERF_BLAST_PAGES.resolveModelAttribute(context, model).asInt());
        configuration.setPersistDeliveryCountBeforeDelivery(PERSIST_DELIVERY_COUNT_BEFORE_DELIVERY.resolveModelAttribute(context, model).asBoolean());
        configuration.setPersistenceEnabled(PERSISTENCE_ENABLED.resolveModelAttribute(context, model).asBoolean());
        configuration.setPersistIDCache(PERSIST_ID_CACHE.resolveModelAttribute(context, model).asBoolean());

        configuration.setRunSyncSpeedTest(RUN_SYNC_SPEED_TEST.resolveModelAttribute(context, model).asBoolean());

        configuration.setScheduledThreadPoolMaxSize(SCHEDULED_THREAD_POOL_MAX_SIZE.resolveModelAttribute(context, model).asInt());
        configuration.setSecurityEnabled(SECURITY_ENABLED.resolveModelAttribute(context, model).asBoolean());
        configuration.setSecurityInvalidationInterval(SECURITY_INVALIDATION_INTERVAL.resolveModelAttribute(context, model).asLong());
        configuration.setServerDumpInterval(SERVER_DUMP_INTERVAL.resolveModelAttribute(context, model).asLong());
        configuration.setSharedStore(SHARED_STORE.resolveModelAttribute(context, model).asBoolean());
        configuration.setThreadPoolMaxSize(THREAD_POOL_MAX_SIZE.resolveModelAttribute(context, model).asInt());
        configuration.setTransactionTimeout(TRANSACTION_TIMEOUT.resolveModelAttribute(context, model).asLong());
        configuration.setTransactionTimeoutScanPeriod(TRANSACTION_TIMEOUT_SCAN_PERIOD.resolveModelAttribute(context, model).asLong());
        configuration.setWildcardRoutingEnabled(WILD_CARD_ROUTING_ENABLED.resolveModelAttribute(context, model).asBoolean());
        // --
        processAddressSettings(context, configuration, model);
        processSecuritySettings(context, configuration, model);

        // Add in items from child resources
        GroupingHandlerAdd.addGroupingHandlerConfig(context,configuration, model);
        BroadcastGroupAdd.addBroadcastGroupConfigs(context, configuration, model);
        DiscoveryGroupAdd.addDiscoveryGroupConfigs(context, configuration, model);
        DivertAdd.addDivertConfigs(context, configuration, model);
        QueueAdd.addQueueConfigs(context, configuration, model);
        BridgeAdd.addBridgeConfigs(context, configuration, model);
        ClusterConnectionAdd.addClusterConnectionConfigs(context, configuration, model);
        ConnectorServiceAdd.addConnectorServiceConfigs(context, configuration, model);

        return configuration;
    }

    /**
     * Process the address settings.
     *
     * @param configuration the hornetQ configuration
     * @param params        the detyped operation parameters
     */
    /**
     * Process the address settings.
     *
     * @param configuration the hornetQ configuration
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

    /**
     * Process the security settings.
     *
     * @param configuration the hornetQ configuration
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
                        roles.add(SecurityRoleAdd.transform(context, role.getName(), role.getValue()));
                    }
                    configuration.getSecurityRoles().put(match, roles);
                }
            }
        }
    }

    /**
     * Create a path service for a given target.
     *
     *
     * @param name          the path service name
     * @param path          the detyped path element
     * @param serviceTarget the service target
     * @param operation
     * @return the created service name
     */
   static ServiceName createDirectoryService(final String name, final ModelNode path, final ServiceTarget serviceTarget, ModelNode operation,
                                               final List<ServiceController<?>> newControllers, final ServiceListener listener) {
         final ServiceName hqServiceName = MessagingServices.getHornetQServiceName(PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR)));
         final ServiceName serviceName = hqServiceName.append(name);
         final String relativeTo = path.hasDefined(RELATIVE_TO) ? path.get(RELATIVE_TO).asString() : DEFAULT_RELATIVE_TO;
         final String pathName = path.hasDefined(PATH) ? path.get(PATH).asString() : DEFAULT_PATH + name;
         RelativePathService.addService(serviceName, pathName, true, relativeTo, serviceTarget, newControllers, listener);
         return serviceName;
     }

}

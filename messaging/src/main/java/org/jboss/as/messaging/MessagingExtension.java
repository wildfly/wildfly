/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.jboss.as.messaging.AddressSettingDefinition.ADDRESS_FULL_MESSAGE_POLICY;
import static org.jboss.as.messaging.AddressSettingDefinition.LAST_VALUE_QUEUE;
import static org.jboss.as.messaging.AddressSettingDefinition.MAX_DELIVERY_ATTEMPTS;
import static org.jboss.as.messaging.AddressSettingDefinition.MAX_SIZE_BYTES;
import static org.jboss.as.messaging.AddressSettingDefinition.MESSAGE_COUNTER_HISTORY_DAY_LIMIT;
import static org.jboss.as.messaging.AddressSettingDefinition.PAGE_MAX_CACHE_SIZE;
import static org.jboss.as.messaging.AddressSettingDefinition.PAGE_SIZE_BYTES;
import static org.jboss.as.messaging.AddressSettingDefinition.REDELIVERY_DELAY;
import static org.jboss.as.messaging.AddressSettingDefinition.REDISTRIBUTION_DELAY;
import static org.jboss.as.messaging.AddressSettingDefinition.SEND_TO_DLA_ON_NO_ROUTE;
import static org.jboss.as.messaging.BridgeDefinition.QUEUE_NAME;
import static org.jboss.as.messaging.BridgeDefinition.USE_DUPLICATE_DETECTION;
import static org.jboss.as.messaging.BroadcastGroupDefinition.BROADCAST_PERIOD;
import static org.jboss.as.messaging.ClusterConnectionDefinition.ALLOW_DIRECT_CONNECTIONS_ONLY;
import static org.jboss.as.messaging.ClusterConnectionDefinition.FORWARD_WHEN_NO_CONSUMERS;
import static org.jboss.as.messaging.ClusterConnectionDefinition.MAX_HOPS;
import static org.jboss.as.messaging.CommonAttributes.ALLOW_FAILBACK;
import static org.jboss.as.messaging.CommonAttributes.ASYNC_CONNECTION_EXECUTION_ENABLED;
import static org.jboss.as.messaging.CommonAttributes.BACKUP;
import static org.jboss.as.messaging.CommonAttributes.BACKUP_GROUP_NAME;
import static org.jboss.as.messaging.CommonAttributes.BRIDGE_CONFIRMATION_WINDOW_SIZE;
import static org.jboss.as.messaging.CommonAttributes.BROADCAST_GROUP;
import static org.jboss.as.messaging.CommonAttributes.CALL_FAILOVER_TIMEOUT;
import static org.jboss.as.messaging.CommonAttributes.CALL_TIMEOUT;
import static org.jboss.as.messaging.CommonAttributes.CHECK_FOR_LIVE_SERVER;
import static org.jboss.as.messaging.CommonAttributes.CHECK_PERIOD;
import static org.jboss.as.messaging.CommonAttributes.CLUSTERED;
import static org.jboss.as.messaging.CommonAttributes.CLUSTER_CONNECTION;
import static org.jboss.as.messaging.CommonAttributes.CONNECTION_FACTORY;
import static org.jboss.as.messaging.CommonAttributes.CONNECTION_TTL;
import static org.jboss.as.messaging.CommonAttributes.CONNECTION_TTL_OVERRIDE;
import static org.jboss.as.messaging.CommonAttributes.CORE_ADDRESS;
import static org.jboss.as.messaging.CommonAttributes.CREATE_BINDINGS_DIR;
import static org.jboss.as.messaging.CommonAttributes.CREATE_JOURNAL_DIR;
import static org.jboss.as.messaging.CommonAttributes.DEAD_LETTER_ADDRESS;
import static org.jboss.as.messaging.CommonAttributes.DISCOVERY_GROUP;
import static org.jboss.as.messaging.CommonAttributes.DURABLE;
import static org.jboss.as.messaging.CommonAttributes.EXPIRY_ADDRESS;
import static org.jboss.as.messaging.CommonAttributes.FAILBACK_DELAY;
import static org.jboss.as.messaging.CommonAttributes.FAILOVER_ON_SHUTDOWN;
import static org.jboss.as.messaging.CommonAttributes.FILTER;
import static org.jboss.as.messaging.CommonAttributes.HA;
import static org.jboss.as.messaging.CommonAttributes.HORNETQ_SERVER;
import static org.jboss.as.messaging.CommonAttributes.ID_CACHE_SIZE;
import static org.jboss.as.messaging.CommonAttributes.JGROUPS_CHANNEL;
import static org.jboss.as.messaging.CommonAttributes.JGROUPS_STACK;
import static org.jboss.as.messaging.CommonAttributes.JMX_DOMAIN;
import static org.jboss.as.messaging.CommonAttributes.JMX_MANAGEMENT_ENABLED;
import static org.jboss.as.messaging.CommonAttributes.JOURNAL_BUFFER_SIZE;
import static org.jboss.as.messaging.CommonAttributes.JOURNAL_BUFFER_TIMEOUT;
import static org.jboss.as.messaging.CommonAttributes.JOURNAL_COMPACT_MIN_FILES;
import static org.jboss.as.messaging.CommonAttributes.JOURNAL_COMPACT_PERCENTAGE;
import static org.jboss.as.messaging.CommonAttributes.JOURNAL_FILE_SIZE;
import static org.jboss.as.messaging.CommonAttributes.JOURNAL_MAX_IO;
import static org.jboss.as.messaging.CommonAttributes.JOURNAL_MIN_FILES;
import static org.jboss.as.messaging.CommonAttributes.JOURNAL_SYNC_NON_TRANSACTIONAL;
import static org.jboss.as.messaging.CommonAttributes.JOURNAL_SYNC_TRANSACTIONAL;
import static org.jboss.as.messaging.CommonAttributes.JOURNAL_TYPE;
import static org.jboss.as.messaging.CommonAttributes.LOG_JOURNAL_WRITE_RATE;
import static org.jboss.as.messaging.CommonAttributes.MANAGEMENT_ADDRESS;
import static org.jboss.as.messaging.CommonAttributes.MANAGEMENT_NOTIFICATION_ADDRESS;
import static org.jboss.as.messaging.CommonAttributes.MAX_RETRY_INTERVAL;
import static org.jboss.as.messaging.CommonAttributes.MESSAGE_COUNTER_ENABLED;
import static org.jboss.as.messaging.CommonAttributes.MESSAGE_COUNTER_MAX_DAY_HISTORY;
import static org.jboss.as.messaging.CommonAttributes.MESSAGE_COUNTER_SAMPLE_PERIOD;
import static org.jboss.as.messaging.CommonAttributes.MESSAGE_EXPIRY_SCAN_PERIOD;
import static org.jboss.as.messaging.CommonAttributes.MESSAGE_EXPIRY_THREAD_PRIORITY;
import static org.jboss.as.messaging.CommonAttributes.MIN_LARGE_MESSAGE_SIZE;
import static org.jboss.as.messaging.CommonAttributes.PAGE_MAX_CONCURRENT_IO;
import static org.jboss.as.messaging.CommonAttributes.PARAM;
import static org.jboss.as.messaging.CommonAttributes.PERF_BLAST_PAGES;
import static org.jboss.as.messaging.CommonAttributes.PERSISTENCE_ENABLED;
import static org.jboss.as.messaging.CommonAttributes.PERSIST_DELIVERY_COUNT_BEFORE_DELIVERY;
import static org.jboss.as.messaging.CommonAttributes.PERSIST_ID_CACHE;
import static org.jboss.as.messaging.CommonAttributes.POOLED_CONNECTION_FACTORY;
import static org.jboss.as.messaging.CommonAttributes.REMOTING_INTERCEPTORS;
import static org.jboss.as.messaging.CommonAttributes.REPLICATION_CLUSTERNAME;
import static org.jboss.as.messaging.CommonAttributes.RETRY_INTERVAL;
import static org.jboss.as.messaging.CommonAttributes.RETRY_INTERVAL_MULTIPLIER;
import static org.jboss.as.messaging.CommonAttributes.RUNTIME_QUEUE;
import static org.jboss.as.messaging.CommonAttributes.RUN_SYNC_SPEED_TEST;
import static org.jboss.as.messaging.CommonAttributes.SECURITY_ENABLED;
import static org.jboss.as.messaging.CommonAttributes.SECURITY_INVALIDATION_INTERVAL;
import static org.jboss.as.messaging.CommonAttributes.SELECTOR;
import static org.jboss.as.messaging.CommonAttributes.SERVER_DUMP_INTERVAL;
import static org.jboss.as.messaging.CommonAttributes.SHARED_STORE;
import static org.jboss.as.messaging.CommonAttributes.TRANSACTION_TIMEOUT;
import static org.jboss.as.messaging.CommonAttributes.TRANSACTION_TIMEOUT_SCAN_PERIOD;
import static org.jboss.as.messaging.CommonAttributes.TRANSFORMER_CLASS_NAME;
import static org.jboss.as.messaging.CommonAttributes.WILD_CARD_ROUTING_ENABLED;
import static org.jboss.as.messaging.DiscoveryGroupDefinition.INITIAL_WAIT_TIMEOUT;
import static org.jboss.as.messaging.DiscoveryGroupDefinition.REFRESH_TIMEOUT;
import static org.jboss.as.messaging.DivertDefinition.EXCLUSIVE;
import static org.jboss.as.messaging.DivertDefinition.FORWARDING_ADDRESS;
import static org.jboss.as.messaging.DivertDefinition.ROUTING_NAME;
import static org.jboss.as.messaging.GroupingHandlerDefinition.GROUPING_HANDLER_ADDRESS;
import static org.jboss.as.messaging.GroupingHandlerDefinition.TYPE;
import static org.jboss.as.messaging.Namespace.MESSAGING_1_0;
import static org.jboss.as.messaging.Namespace.MESSAGING_1_1;
import static org.jboss.as.messaging.Namespace.MESSAGING_1_2;
import static org.jboss.as.messaging.Namespace.MESSAGING_1_3;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Common.AUTO_GROUP;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Common.BLOCK_ON_ACKNOWLEDGE;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Common.BLOCK_ON_DURABLE_SEND;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Common.BLOCK_ON_NON_DURABLE_SEND;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Common.CACHE_LARGE_MESSAGE_CLIENT;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Common.CLIENT_FAILURE_CHECK_PERIOD;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Common.COMPRESS_LARGE_MESSAGES;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Common.CONFIRMATION_WINDOW_SIZE;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Common.CONNECTION_LOAD_BALANCING_CLASS_NAME;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Common.CONSUMER_MAX_RATE;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Common.CONSUMER_WINDOW_SIZE;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Common.FAILOVER_ON_INITIAL_CONNECTION;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Common.GROUP_ID;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Common.PRE_ACKNOWLEDGE;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Common.PRODUCER_MAX_RATE;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Common.PRODUCER_WINDOW_SIZE;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Common.RECONNECT_ATTEMPTS;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Common.TRANSACTION_BATCH_SIZE;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Common.USE_GLOBAL_POOLS;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Pooled.INITIAL_MESSAGE_PACKET_SIZE;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Pooled.JNDI_PARAMS;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Pooled.SETUP_ATTEMPTS;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Pooled.SETUP_INTERVAL;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Pooled.TRANSACTION;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Pooled.USE_AUTO_RECOVERY;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Pooled.USE_JNDI;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Pooled.USE_LOCAL_TX;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Regular.FACTORY_TYPE;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.services.path.ResolvePathHandler;
import org.jboss.as.controller.transform.AbstractSubsystemTransformer;
import org.jboss.as.controller.transform.RejectExpressionValuesTransformer;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.TransformersSubRegistration;
import org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Common;
import org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Pooled;
import org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Regular;
import org.jboss.as.messaging.jms.ConnectionFactoryDefinition;
import org.jboss.as.messaging.jms.JMSQueueDefinition;
import org.jboss.as.messaging.jms.JMSTopicDefinition;
import org.jboss.as.messaging.jms.PooledConnectionFactoryDefinition;
import org.jboss.as.messaging.jms.bridge.JMSBridgeDefinition;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 * Domain extension that integrates HornetQ.
 *
 * <dl>
 *   <dt>AS 7.2.0</dt>
 *   <dd>
 *     <ul>
 *       <li>XML namespace: urn:jboss:domain:messaging:1.3
 *       <li>Management model: 1.2.0
 *     </ul>
 *   </dd>
 *   <dt>AS 7.1.2, 7.1.3<dt>
 *   <dd>
 *     <ul>
 *       <li>XML namespace: urn:jboss:domain:messaging:1.2
 *       <li>Management model: 1.1.0
 *     </ul>
 *   </dd>
 * </dl>
 *
 * @author Emanuel Muckenhuber
 * @author <a href="mailto:andy.taylor@jboss.com">Andy Taylor</a>
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class MessagingExtension implements Extension {

    public static final String SUBSYSTEM_NAME = "messaging";

    static final PathElement SUBSYSTEM_PATH  = PathElement.pathElement(SUBSYSTEM, SUBSYSTEM_NAME);

    static final String RESOURCE_NAME = MessagingExtension.class.getPackage().getName() + ".LocalDescriptions";

    private static final int MANAGEMENT_API_MAJOR_VERSION = 1;
    private static final int MANAGEMENT_API_MINOR_VERSION = 2;
    private static final int MANAGEMENT_API_MICRO_VERSION = 0;

    public static final ModelVersion VERSION_1_2_0 = ModelVersion.create(1, 2, 0);
    public static final ModelVersion VERSION_1_1_0 = ModelVersion.create(1, 1, 0);

    public static ResourceDescriptionResolver getResourceDescriptionResolver(final String... keyPrefix) {
        return getResourceDescriptionResolver(true, keyPrefix);
    }

    public static ResourceDescriptionResolver getResourceDescriptionResolver(final boolean useUnprefixedChildTypes, final String... keyPrefix) {
        StringBuilder prefix = new StringBuilder();
        for (String kp : keyPrefix) {
            if (prefix.length() > 0){
                prefix.append('.');
            }
            prefix.append(kp);
        }
        return new StandardResourceDescriptionResolver(prefix.toString(), RESOURCE_NAME, MessagingExtension.class.getClassLoader(), true, useUnprefixedChildTypes);
    }

    public void initialize(ExtensionContext context) {
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME,
                MANAGEMENT_API_MAJOR_VERSION,
                MANAGEMENT_API_MINOR_VERSION,
                MANAGEMENT_API_MICRO_VERSION);
        subsystem.registerXMLElementWriter(MessagingXMLWriter.INSTANCE);

        boolean registerRuntimeOnly = context.isRuntimeOnlyRegistrationValid();

        // Root resource
        final ManagementResourceRegistration rootRegistration = subsystem.registerSubsystemModel(MessagingSubsystemRootResourceDefinition.INSTANCE);
        rootRegistration.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE);

        // HQ servers
        final ManagementResourceRegistration serverRegistration = rootRegistration.registerSubModel(new HornetQServerResourceDefinition(registerRuntimeOnly));

        // Runtime addresses
        if (registerRuntimeOnly) {
            final ManagementResourceRegistration coreAddress = serverRegistration.registerSubModel(new CoreAddressDefinition());
            coreAddress.setRuntimeOnly(true);
        }

        // Address settings
        serverRegistration.registerSubModel(new AddressSettingDefinition(registerRuntimeOnly));

        // Broadcast groups
        serverRegistration.registerSubModel(new BroadcastGroupDefinition(registerRuntimeOnly));
        // getConnectorPairs, -- no, this is just the same as attribute connector-refs

        // Discovery groups
        serverRegistration.registerSubModel(new DiscoveryGroupDefinition(registerRuntimeOnly));

        // Diverts
        serverRegistration.registerSubModel(new DivertDefinition(registerRuntimeOnly));

        // Core queues
        serverRegistration.registerSubModel(QueueDefinition.newQueueDefinition(registerRuntimeOnly));
        // getExpiryAddress, setExpiryAddress, getDeadLetterAddress, setDeadLetterAddress  -- no -- just toggle the 'queue-address', make this a mutable attr of address-setting

        // Runtime core queues
        serverRegistration.registerSubModel(QueueDefinition.newRuntimeQueueDefinition(registerRuntimeOnly));

        // Acceptors
        serverRegistration.registerSubModel(GenericTransportDefinition.createAcceptorDefinition(registerRuntimeOnly));
        serverRegistration.registerSubModel(RemoteTransportDefinition.createAcceptorDefinition(registerRuntimeOnly));
        serverRegistration.registerSubModel(InVMTransportDefinition.createAcceptorDefinition(registerRuntimeOnly));

        // Connectors
        serverRegistration.registerSubModel(GenericTransportDefinition.createConnectorDefinition(registerRuntimeOnly));
        serverRegistration.registerSubModel(RemoteTransportDefinition.createConnectorDefinition(registerRuntimeOnly));
        serverRegistration.registerSubModel(InVMTransportDefinition.createConnectorDefinition(registerRuntimeOnly));

        // Bridges
        serverRegistration.registerSubModel(new BridgeDefinition(registerRuntimeOnly));

        // Cluster connections
        serverRegistration.registerSubModel(new ClusterConnectionDefinition(registerRuntimeOnly));

        // Grouping Handler
        serverRegistration.registerSubModel(new GroupingHandlerDefinition(registerRuntimeOnly));

        // Connector services
        serverRegistration.registerSubModel(new ConnectorServiceDefinition(registerRuntimeOnly));

        // Messaging paths
        //todo, shouldn't we leverage Path service from AS? see: package org.jboss.as.controller.services.path
        for (final String path : MessagingPathHandlers.PATHS.keySet()) {
            ManagementResourceRegistration bindings = serverRegistration.registerSubModel(PathElement.pathElement(PATH, path),
                    new MessagingSubsystemProviders.PathProvider(path));
            MessagingPathHandlers.register(bindings, path);
            // Create the path resolver operation
            if (context.getProcessType().isServer()) {
                final ResolvePathHandler resolvePathHandler = ResolvePathHandler.Builder.of(context.getPathManager())
                        .setPathAttribute(MessagingPathHandlers.PATHS.get(path))
                        .setRelativeToAttribute(MessagingPathHandlers.RELATIVE_TO)
                        .build();
                bindings.registerOperationHandler(resolvePathHandler.getOperationDefinition(), resolvePathHandler);
            }
        }

        // Connection factories
        serverRegistration.registerSubModel(new ConnectionFactoryDefinition(registerRuntimeOnly));
        // getJNDIBindings (no -- same as "entries")

        // Resource Adapter Pooled connection factories
        serverRegistration.registerSubModel(new PooledConnectionFactoryDefinition(registerRuntimeOnly));
        // TODO how do ConnectionFactoryControl things relate?

        // JMS Queues
        serverRegistration.registerSubModel(new JMSQueueDefinition(registerRuntimeOnly));
        // setExpiryAddress, setDeadLetterAddress  -- no -- just toggle the 'queue-address', make this a mutable attr of address-setting
        // getJNDIBindings (no -- same as "entries")

        // JMS Topics
        serverRegistration.registerSubModel(new JMSTopicDefinition(registerRuntimeOnly));
        // getJNDIBindings (no -- same as "entries")

        serverRegistration.registerSubModel(new SecuritySettingDefinition(registerRuntimeOnly));

        if (registerRuntimeOnly) {

            ResourceDefinition deploymentsDef = new SimpleResourceDefinition(SUBSYSTEM_PATH, getResourceDescriptionResolver("deployed"));
            final ManagementResourceRegistration deploymentsRegistration = subsystem.registerDeploymentModel(deploymentsDef);
            final ManagementResourceRegistration serverModel = deploymentsRegistration.registerSubModel(new HornetQServerResourceDefinition(true));

            serverModel.registerSubModel(JMSQueueDefinition.newDeployedJMSQueueDefinition());
            serverModel.registerSubModel(JMSTopicDefinition.newDeployedJMSTopicDefinition());
        }

        // JMS Bridges
        rootRegistration.registerSubModel(new JMSBridgeDefinition());

        registerTransformers_1_1_0(subsystem);
    }

    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, MESSAGING_1_0.getUriString(), MessagingSubsystemParser.getInstance());
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, MESSAGING_1_1.getUriString(), MessagingSubsystemParser.getInstance());
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, MESSAGING_1_2.getUriString(), Messaging12SubsystemParser.getInstance());
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, MESSAGING_1_3.getUriString(), Messaging13SubsystemParser.getInstance());
    }

    private static void registerTransformers_1_1_0(final SubsystemRegistration subsystem) {

        final AttributeDefinition[] transformerdPooledCFAttributes = { Pooled.INITIAL_CONNECT_ATTEMPTS,
                Pooled.INITIAL_MESSAGE_PACKET_SIZE,
                COMPRESS_LARGE_MESSAGES,
                USE_AUTO_RECOVERY,
                CALL_FAILOVER_TIMEOUT };

        final TransformersSubRegistration transformers = subsystem.registerModelTransformers(VERSION_1_1_0, new AbstractSubsystemTransformer(SUBSYSTEM_NAME) {

            @Override
            public ModelNode transformModel(final TransformationContext context, final ModelNode model) {
                ModelNode oldModel = model.clone();
                if (oldModel.hasDefined(HORNETQ_SERVER)) {
                    for (Property server : oldModel.get(HORNETQ_SERVER).asPropertyList()) {
                        if (!oldModel.get(HORNETQ_SERVER, server.getName()).hasDefined(CLUSTERED.getName())) {
                            oldModel.get(HORNETQ_SERVER, server.getName()).get(CLUSTERED.getName()).set(false);
                        }
                        oldModel.get(HORNETQ_SERVER, server.getName()).remove(CHECK_FOR_LIVE_SERVER.getName());
                        oldModel.get(HORNETQ_SERVER, server.getName()).remove(BACKUP_GROUP_NAME.getName());
                        oldModel.get(HORNETQ_SERVER, server.getName()).remove(REPLICATION_CLUSTERNAME.getName());
                        if (server.getValue().hasDefined(CLUSTER_CONNECTION)) {
                            for (Property clusterConnection : server.getValue().get(CLUSTER_CONNECTION).asPropertyList()) {
                                oldModel.get(HORNETQ_SERVER, server.getName(), CLUSTER_CONNECTION, clusterConnection.getName()).remove(CALL_FAILOVER_TIMEOUT.getName());
                            }
                        }
                        if (server.getValue().hasDefined(BROADCAST_GROUP)) {
                            for (Property broadcastGroup : server.getValue().get(BROADCAST_GROUP).asPropertyList()) {
                                oldModel.get(HORNETQ_SERVER, server.getName(), BROADCAST_GROUP, broadcastGroup.getName()).remove(JGROUPS_STACK.getName());
                                oldModel.get(HORNETQ_SERVER, server.getName(), BROADCAST_GROUP, broadcastGroup.getName()).remove(JGROUPS_CHANNEL.getName());
                            }
                        }
                        if (server.getValue().hasDefined(DISCOVERY_GROUP)) {
                            for (Property discoveryGroup : server.getValue().get(DISCOVERY_GROUP).asPropertyList()) {
                                oldModel.get(HORNETQ_SERVER, server.getName(), DISCOVERY_GROUP, discoveryGroup.getName()).remove(JGROUPS_STACK.getName());
                                oldModel.get(HORNETQ_SERVER, server.getName(), DISCOVERY_GROUP, discoveryGroup.getName()).remove(JGROUPS_CHANNEL.getName());
                            }
                        }
                        if (server.getValue().hasDefined(POOLED_CONNECTION_FACTORY)) {
                            for (Property pooledConnectionFactory : server.getValue().get(POOLED_CONNECTION_FACTORY).asPropertyList()) {
                                for (AttributeDefinition attribute : transformerdPooledCFAttributes) {
                                    oldModel.get(HORNETQ_SERVER, server.getName(), POOLED_CONNECTION_FACTORY, pooledConnectionFactory.getName()).remove(attribute.getName());
                                }
                            }
                        }
                        if (server.getValue().hasDefined(CONNECTION_FACTORY)) {
                            for (Property connectionFactory : server.getValue().get(CONNECTION_FACTORY).asPropertyList()) {
                                oldModel.get(HORNETQ_SERVER, server.getName(), CONNECTION_FACTORY, connectionFactory.getName()).remove(CALL_FAILOVER_TIMEOUT.getName());
                                if (!connectionFactory.getValue().hasDefined(HA.getName())) {
                                    oldModel.get(HORNETQ_SERVER, server.getName(), CONNECTION_FACTORY, connectionFactory.getName()).get(HA.getName()).set(HA.getDefaultValue());
                                }
                                if (connectionFactory.getValue().hasDefined(FACTORY_TYPE.getName()) && (connectionFactory.getValue().get(FACTORY_TYPE.getName()).equals(FACTORY_TYPE.getDefaultValue()))) {
                                    oldModel.get(HORNETQ_SERVER, server.getName(), CONNECTION_FACTORY, connectionFactory.getName()).get(FACTORY_TYPE.getName()).set(new ModelNode());
                                }
                            }
                        }
                        //TODO - a nicer way to automagically remove these runtime resources?
                        if (server.getValue().hasDefined(CORE_ADDRESS)) {
                            oldModel.get(HORNETQ_SERVER, server.getName()).remove(CORE_ADDRESS);
                        }
                        if (server.getValue().hasDefined(RUNTIME_QUEUE)) {
                            oldModel.get(HORNETQ_SERVER, server.getName()).remove(RUNTIME_QUEUE);
                        }
                    }
                }
                return oldModel;
            }
        });

        RejectExpressionValuesTransformer rejectServerExpressionTransformer = new RejectExpressionValuesTransformer(
                ASYNC_CONNECTION_EXECUTION_ENABLED, PERSISTENCE_ENABLED, SECURITY_ENABLED, SECURITY_INVALIDATION_INTERVAL,
                WILD_CARD_ROUTING_ENABLED, MANAGEMENT_ADDRESS, MANAGEMENT_NOTIFICATION_ADDRESS, JMX_MANAGEMENT_ENABLED, JMX_DOMAIN,
                MESSAGE_COUNTER_ENABLED, MESSAGE_COUNTER_SAMPLE_PERIOD, MESSAGE_COUNTER_MAX_DAY_HISTORY,
                CONNECTION_TTL_OVERRIDE, TRANSACTION_TIMEOUT, TRANSACTION_TIMEOUT_SCAN_PERIOD,
                MESSAGE_EXPIRY_SCAN_PERIOD, MESSAGE_EXPIRY_THREAD_PRIORITY, ID_CACHE_SIZE, PERSIST_ID_CACHE,
                REMOTING_INTERCEPTORS, BACKUP, ALLOW_FAILBACK, FAILBACK_DELAY, FAILOVER_ON_SHUTDOWN,
                SHARED_STORE, PERSIST_DELIVERY_COUNT_BEFORE_DELIVERY, PAGE_MAX_CONCURRENT_IO,
                CREATE_BINDINGS_DIR, CREATE_JOURNAL_DIR, JOURNAL_TYPE, JOURNAL_BUFFER_TIMEOUT, JOURNAL_BUFFER_SIZE,
                JOURNAL_SYNC_TRANSACTIONAL, JOURNAL_SYNC_NON_TRANSACTIONAL, LOG_JOURNAL_WRITE_RATE,
                JOURNAL_FILE_SIZE, JOURNAL_MIN_FILES, JOURNAL_COMPACT_PERCENTAGE, JOURNAL_COMPACT_MIN_FILES, JOURNAL_MAX_IO,
                PERF_BLAST_PAGES, RUN_SYNC_SPEED_TEST, SERVER_DUMP_INTERVAL);
        TransformersSubRegistration server = transformers.registerSubResource(PathElement.pathElement(HORNETQ_SERVER));
        server.registerOperationTransformer(ADD, new OperationTransformers.MultipleOperationalTransformer(
                new OperationTransformers.InsertDefaultValuesOperationTransformer(ID_CACHE_SIZE, CLUSTERED),
                new OperationTransformers.RemoveAttributesOperationTransformer(CHECK_FOR_LIVE_SERVER, BACKUP_GROUP_NAME, REPLICATION_CLUSTERNAME),
                rejectServerExpressionTransformer));
        server.registerOperationTransformer(WRITE_ATTRIBUTE_OPERATION, rejectServerExpressionTransformer.getWriteAttributeTransformer());

        rejectExpressions(server, AddressSettingDefinition.PATH, DEAD_LETTER_ADDRESS, EXPIRY_ADDRESS, REDELIVERY_DELAY, MAX_DELIVERY_ATTEMPTS, MAX_SIZE_BYTES,
                PAGE_SIZE_BYTES, PAGE_MAX_CACHE_SIZE, ADDRESS_FULL_MESSAGE_POLICY, MESSAGE_COUNTER_HISTORY_DAY_LIMIT,
                LAST_VALUE_QUEUE, REDISTRIBUTION_DELAY, SEND_TO_DLA_ON_NO_ROUTE);

        rejectExpressions(server, BroadcastGroupDefinition.PATH, BROADCAST_PERIOD);

        rejectExpressions(server, DiscoveryGroupDefinition.PATH, INITIAL_WAIT_TIMEOUT, REFRESH_TIMEOUT);

        rejectExpressions(server, DivertDefinition.PATH, ROUTING_NAME, DivertDefinition.ADDRESS, FORWARDING_ADDRESS, FILTER, TRANSFORMER_CLASS_NAME, EXCLUSIVE);

        rejectExpressions(server, BridgeDefinition.PATH, QUEUE_NAME, USE_DUPLICATE_DETECTION, BridgeDefinition.RECONNECT_ATTEMPTS, BridgeDefinition.FORWARDING_ADDRESS,
                FILTER, TRANSFORMER_CLASS_NAME, HA, MIN_LARGE_MESSAGE_SIZE, CHECK_PERIOD, CONNECTION_TTL,
                RETRY_INTERVAL, RETRY_INTERVAL_MULTIPLIER, MAX_RETRY_INTERVAL, BRIDGE_CONFIRMATION_WINDOW_SIZE);

        rejectExpressions(server, QueueDefinition.PATH, QueueDefinition.ADDRESS, FILTER, DURABLE);

        final String[] transports = { CommonAttributes.ACCEPTOR, CommonAttributes.REMOTE_ACCEPTOR, CommonAttributes.IN_VM_ACCEPTOR,
                CommonAttributes.CONNECTOR, CommonAttributes.REMOTE_CONNECTOR, CommonAttributes.IN_VM_CONNECTOR };
        for (String path : transports) {
            TransformersSubRegistration transport = rejectExpressions(server, PathElement.pathElement(path), CommonAttributes.FACTORY_CLASS);
            rejectExpressions(transport, PathElement.pathElement(PARAM), VALUE);
        }

        for (final String path : MessagingPathHandlers.PATHS.keySet()) {
            rejectExpressions(server, PathElement.pathElement(PATH, path), PATH);
        }

        RejectExpressionValuesTransformer rejectClusterConnectionExpressions = new RejectExpressionValuesTransformer(ClusterConnectionDefinition.ADDRESS,
                ALLOW_DIRECT_CONNECTIONS_ONLY, ClusterConnectionDefinition.CHECK_PERIOD, ClusterConnectionDefinition.CONNECTION_TTL, FORWARD_WHEN_NO_CONSUMERS, MAX_HOPS,
                ClusterConnectionDefinition.MAX_RETRY_INTERVAL, ClusterConnectionDefinition.RETRY_INTERVAL, ClusterConnectionDefinition.RETRY_INTERVAL_MULTIPLIER,
                ClusterConnectionDefinition.USE_DUPLICATE_DETECTION, CALL_TIMEOUT, CALL_FAILOVER_TIMEOUT);
        TransformersSubRegistration clusterConnection = server.registerSubResource(ClusterConnectionDefinition.PATH, rejectClusterConnectionExpressions, rejectClusterConnectionExpressions);
        clusterConnection.registerOperationTransformer(ADD, new OperationTransformers.MultipleOperationalTransformer(rejectClusterConnectionExpressions,
                new OperationTransformers.RemoveAttributesOperationTransformer(CALL_FAILOVER_TIMEOUT)));
        clusterConnection.registerOperationTransformer(WRITE_ATTRIBUTE_OPERATION, new OperationTransformers.MultipleOperationalTransformer(rejectClusterConnectionExpressions,
                new OperationTransformers.FailUnignoredAttributesOperationTransformer(CALL_FAILOVER_TIMEOUT)));

        rejectExpressions(server, ConnectorServiceDefinition.PATH, CommonAttributes.FACTORY_CLASS);

        RejectExpressionValuesTransformer rejectConnectionFactoryExpressions = new RejectExpressionValuesTransformer(Regular.FACTORY_TYPE,
                HA, MIN_LARGE_MESSAGE_SIZE, CALL_TIMEOUT,
                AUTO_GROUP, BLOCK_ON_ACKNOWLEDGE, BLOCK_ON_DURABLE_SEND, BLOCK_ON_NON_DURABLE_SEND, CACHE_LARGE_MESSAGE_CLIENT, CLIENT_FAILURE_CHECK_PERIOD,
                COMPRESS_LARGE_MESSAGES, CONFIRMATION_WINDOW_SIZE, CONNECTION_LOAD_BALANCING_CLASS_NAME, Common.CONNECTION_TTL, CONSUMER_MAX_RATE,
                CONSUMER_WINDOW_SIZE, FAILOVER_ON_INITIAL_CONNECTION, GROUP_ID, Common.MAX_RETRY_INTERVAL, Common.MIN_LARGE_MESSAGE_SIZE, PRE_ACKNOWLEDGE,
                PRODUCER_MAX_RATE, PRODUCER_WINDOW_SIZE, Common.RECONNECT_ATTEMPTS, Common.RETRY_INTERVAL, Common.RETRY_INTERVAL_MULTIPLIER, TRANSACTION_BATCH_SIZE,
                USE_GLOBAL_POOLS);
        TransformersSubRegistration connectionFactory = server.registerSubResource(ConnectionFactoryDefinition.PATH, rejectConnectionFactoryExpressions, rejectConnectionFactoryExpressions);
        connectionFactory.registerOperationTransformer(ADD, new OperationTransformers.MultipleOperationalTransformer(rejectConnectionFactoryExpressions,
                new OperationTransformers.RemoveAttributesOperationTransformer(CALL_FAILOVER_TIMEOUT)));
        connectionFactory.registerOperationTransformer(WRITE_ATTRIBUTE_OPERATION, new OperationTransformers.MultipleOperationalTransformer(rejectConnectionFactoryExpressions,
                new OperationTransformers.FailUnignoredAttributesOperationTransformer(CALL_FAILOVER_TIMEOUT)));

        RejectExpressionValuesTransformer rejectPooledConnectionFactoryExpressions = new RejectExpressionValuesTransformer(MIN_LARGE_MESSAGE_SIZE, CALL_TIMEOUT,
                AUTO_GROUP, BLOCK_ON_ACKNOWLEDGE, BLOCK_ON_DURABLE_SEND, BLOCK_ON_NON_DURABLE_SEND, CACHE_LARGE_MESSAGE_CLIENT, CLIENT_FAILURE_CHECK_PERIOD,
                COMPRESS_LARGE_MESSAGES, CONFIRMATION_WINDOW_SIZE, CONNECTION_LOAD_BALANCING_CLASS_NAME, Common.CONNECTION_TTL, CONSUMER_MAX_RATE,
                CONSUMER_WINDOW_SIZE, FAILOVER_ON_INITIAL_CONNECTION, GROUP_ID, Common.MAX_RETRY_INTERVAL, Common.MIN_LARGE_MESSAGE_SIZE, PRE_ACKNOWLEDGE,
                PRODUCER_MAX_RATE, PRODUCER_WINDOW_SIZE, Common.RETRY_INTERVAL, Common.RETRY_INTERVAL_MULTIPLIER, TRANSACTION_BATCH_SIZE,
                USE_GLOBAL_POOLS, // end of common attributes
                Pooled.INITIAL_CONNECT_ATTEMPTS, INITIAL_MESSAGE_PACKET_SIZE, JNDI_PARAMS, Pooled.RECONNECT_ATTEMPTS, SETUP_ATTEMPTS, SETUP_INTERVAL,
                TRANSACTION, USE_AUTO_RECOVERY, USE_JNDI, USE_LOCAL_TX);
        TransformersSubRegistration pooledConnectionFactory = server.registerSubResource(PooledConnectionFactoryDefinition.PATH, rejectPooledConnectionFactoryExpressions, rejectPooledConnectionFactoryExpressions);
        pooledConnectionFactory.registerOperationTransformer(ADD, new OperationTransformers.MultipleOperationalTransformer(
                rejectConnectionFactoryExpressions,
                new OperationTransformers.RemoveAttributesOperationTransformer(transformerdPooledCFAttributes),
                new OperationTransformers.InsertDefaultValuesOperationTransformer(Pooled.RECONNECT_ATTEMPTS)));
        pooledConnectionFactory.registerOperationTransformer(WRITE_ATTRIBUTE_OPERATION, new OperationTransformers.MultipleOperationalTransformer(
                rejectPooledConnectionFactoryExpressions,
                new OperationTransformers.FailUnignoredAttributesOperationTransformer(transformerdPooledCFAttributes)));

        rejectExpressions(server, GroupingHandlerDefinition.PATH, TYPE, GROUPING_HANDLER_ADDRESS, GroupingHandlerDefinition.TIMEOUT);

        rejectExpressions(server, JMSQueueDefinition.PATH, SELECTOR, DURABLE);
    }

    private static TransformersSubRegistration rejectExpressions(TransformersSubRegistration parent, PathElement path, AttributeDefinition... attributes) {
        final String[] names = new String[attributes.length];
        for (int i = 0; i < attributes.length; i++) {
            AttributeDefinition def = attributes[i];
            names[i] = def.getName();
        }
        return rejectExpressions(parent, path, names);
    }

    private static TransformersSubRegistration rejectExpressions(TransformersSubRegistration parent, PathElement path, String... attributes) {
        RejectExpressionValuesTransformer rejectExpressions = new RejectExpressionValuesTransformer(attributes);
        TransformersSubRegistration resource = parent.registerSubResource(path, rejectExpressions, rejectExpressions);
        resource.registerOperationTransformer(ADD, rejectExpressions);
        resource.registerOperationTransformer(WRITE_ATTRIBUTE_OPERATION, rejectExpressions.getWriteAttributeTransformer());
        return resource;
    }

}

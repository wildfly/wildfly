/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.messaging.activemq.jms;

import static org.wildfly.extension.messaging.activemq.Capabilities.ELYTRON_SSL_CONTEXT_CAPABILITY;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.CALL_FAILOVER_TIMEOUT;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.CALL_TIMEOUT;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.CLIENT_ID;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.HA;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.JGROUPS_CLUSTER;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.JGROUPS_DISCOVERY_GROUP;
import static org.wildfly.extension.messaging.activemq.jms.ConnectionFactoryAttributes.Common.DESERIALIZATION_ALLOWLIST;
import static org.wildfly.extension.messaging.activemq.jms.ConnectionFactoryAttributes.Common.DESERIALIZATION_BLACKLIST;
import static org.wildfly.extension.messaging.activemq.jms.ConnectionFactoryAttributes.Common.DESERIALIZATION_BLOCKLIST;
import static org.wildfly.extension.messaging.activemq.jms.ConnectionFactoryAttributes.Common.DESERIALIZATION_WHITELIST;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import org.apache.activemq.artemis.api.core.DiscoveryGroupConfiguration;
import org.apache.activemq.artemis.api.core.TransportConfiguration;

import org.apache.activemq.artemis.api.jms.JMSFactoryType;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.network.OutboundSocketBinding;
import org.jboss.as.network.SocketBinding;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.wildfly.extension.messaging.activemq.BinderServiceUtil;
import org.wildfly.extension.messaging.activemq.CommonAttributes;
import org.wildfly.extension.messaging.activemq.DiscoveryGroupDefinition;
import org.wildfly.extension.messaging.activemq.GroupBindingService;
import org.wildfly.extension.messaging.activemq.JGroupsDiscoveryGroupDefinition;
import org.wildfly.extension.messaging.activemq.MessagingServices;
import org.wildfly.extension.messaging.activemq.TransportConfigOperationHandlers;
import org.wildfly.extension.messaging.activemq.broadcast.BroadcastCommandDispatcherFactory;
import org.wildfly.extension.messaging.activemq.jms.ConnectionFactoryAttributes.Common;
import org.wildfly.extension.messaging.activemq._private.MessagingLogger;

import static org.wildfly.extension.messaging.activemq.jms.ConnectionFactoryAttributes.External.ENABLE_AMQ1_PREFIX;

import javax.net.ssl.SSLContext;
import org.jboss.as.controller.AttributeDefinition;

/**
 * Update adding a connection factory to the subsystem. The
 * runtime action will create the {@link ExternalConnectionFactoryService}.
 *
 * @author Emmanuel Hugonnet (c) 2018 Red Hat, inc.
 */
public class ExternalConnectionFactoryAdd extends AbstractAddStepHandler {

    public static final ExternalConnectionFactoryAdd INSTANCE = new ExternalConnectionFactoryAdd();

    private ExternalConnectionFactoryAdd() {
        super(ExternalConnectionFactoryDefinition.ATTRIBUTES);
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        final String name = context.getCurrentAddressValue();
        final ServiceName serviceName = ExternalConnectionFactoryDefinition.CAPABILITY.getCapabilityServiceName(context.getCurrentAddress());
        boolean ha = HA.resolveModelAttribute(context, model).asBoolean();
        boolean enable1Prefixes = ENABLE_AMQ1_PREFIX.resolveModelAttribute(context, model).asBoolean();
        final ModelNode discoveryGroupName = Common.DISCOVERY_GROUP.resolveModelAttribute(context, model);
        final ExternalConnectionFactoryConfiguration config = createConfiguration(context, name, model);
        JMSFactoryType jmsFactoryType = ConnectionFactoryType.valueOf(ConnectionFactoryAttributes.Regular.FACTORY_TYPE.resolveModelAttribute(context, model).asString()).getType();
        List<String> connectorNames = Common.CONNECTORS.unwrap(context, model);
        ServiceBuilder<?> builder = context.getServiceTarget()
                .addService(serviceName)
                .addAliases(JMSServices.getConnectionFactoryBaseServiceName(MessagingServices.getActiveMQServiceName()).append(name));
        ExternalConnectionFactoryService service;
        if (discoveryGroupName.isDefined()) {
            // mapping between the {discovery}-groups and the cluster names they use
            Map<String, String> clusterNames = new HashMap<>();
            Map<String, Supplier<SocketBinding>> groupBindings = new HashMap<>();
            // mapping between the {discovery}-groups and the command dispatcher factory they use
            Map<String, Supplier<BroadcastCommandDispatcherFactory>> commandDispatcherFactories = new HashMap<>();
            final String dgname = discoveryGroupName.asString();
            final String key = "discovery" + dgname;
            ModelNode discoveryGroupModel;
            try {
                discoveryGroupModel = context.readResourceFromRoot(context.getCurrentAddress().getParent().append(JGROUPS_DISCOVERY_GROUP, dgname)).getModel();
            } catch (Resource.NoSuchResourceException ex) {
                discoveryGroupModel = new ModelNode();
            }
            if (discoveryGroupModel.hasDefined(JGROUPS_CLUSTER.getName())) {
                ModelNode channel = JGroupsDiscoveryGroupDefinition.JGROUPS_CHANNEL.resolveModelAttribute(context, discoveryGroupModel);
                ServiceName commandDispatcherFactoryServiceName = MessagingServices.getBroadcastCommandDispatcherFactoryServiceName(channel.asStringOrNull());
                Supplier<BroadcastCommandDispatcherFactory> commandDispatcherFactorySupplier = builder.requires(commandDispatcherFactoryServiceName);
                commandDispatcherFactories.put(key, commandDispatcherFactorySupplier);
                String clusterName = JGROUPS_CLUSTER.resolveModelAttribute(context, discoveryGroupModel).asString();
                clusterNames.put(key, clusterName);
            } else {
                final ServiceName groupBinding = GroupBindingService.getDiscoveryBaseServiceName(MessagingServices.getActiveMQServiceName()).append(dgname);
                Supplier<SocketBinding> groupBindingSupplier = builder.requires(groupBinding);
                groupBindings.put(key, groupBindingSupplier);
            }
            service = new ExternalConnectionFactoryService(getDiscoveryGroup(context, dgname), commandDispatcherFactories, groupBindings, clusterNames, jmsFactoryType, ha, enable1Prefixes, config);
        } else {
            Map<String, Supplier<SocketBinding>> socketBindings = new HashMap<>();
            Map<String, Supplier<OutboundSocketBinding>> outboundSocketBindings = new HashMap<>();
            Set<String> connectorsSocketBindings = new HashSet<>();
            final Set<String> sslContextNames = new HashSet<>();
            TransportConfiguration[] transportConfigurations = TransportConfigOperationHandlers.processConnectors(context, connectorNames, connectorsSocketBindings, sslContextNames);
            Map<String, Boolean> outbounds = TransportConfigOperationHandlers.listOutBoundSocketBinding(context, connectorsSocketBindings);
            for (final String connectorSocketBinding : connectorsSocketBindings) {
                // find whether the connectorSocketBinding references a SocketBinding or an OutboundSocketBinding
                if (outbounds.get(connectorSocketBinding)) {
                    final ServiceName outboundSocketName = context.getCapabilityServiceName(OutboundSocketBinding.SERVICE_DESCRIPTOR, connectorSocketBinding);
                    Supplier<OutboundSocketBinding> outboundSupplier = builder.requires(outboundSocketName);
                    outboundSocketBindings.put(connectorSocketBinding, outboundSupplier);
                } else {
                    final ServiceName socketName = context.getCapabilityServiceName(SocketBinding.SERVICE_DESCRIPTOR, connectorSocketBinding);
                    Supplier<SocketBinding> socketBindingsSupplier = builder.requires(socketName);
                    socketBindings.put(connectorSocketBinding, socketBindingsSupplier);
                }
            }
            Map<String, Supplier<SSLContext>> sslContexts = new HashMap<>();
            for (final String entry : sslContextNames) {
                Supplier<SSLContext> sslContext = builder.requires(ELYTRON_SSL_CONTEXT_CAPABILITY.getCapabilityServiceName(entry));
                sslContexts.put(entry, sslContext);
            }
            service = new ExternalConnectionFactoryService(transportConfigurations, socketBindings, outboundSocketBindings, sslContexts, jmsFactoryType, ha, enable1Prefixes, config);
        }
        builder.setInstance(service);
        builder.install();
        for (String entry : Common.ENTRIES.unwrap(context, model)) {
            MessagingLogger.ROOT_LOGGER.debugf("Referencing %s with JNDI name %s", serviceName, entry);
            BinderServiceUtil.installBinderService(context.getServiceTarget(), entry, service, serviceName);
        }
    }

    static ExternalConnectionFactoryConfiguration createConfiguration(final OperationContext context, final String name, final ModelNode model) throws OperationFailedException {

        final List<String> entries = Common.ENTRIES.unwrap(context, model);

        final ExternalConnectionFactoryConfiguration config = new ExternalConnectionFactoryConfiguration()
                .setName(name)
                .setHA(false)
                .setBindings(entries.toArray(String[]::new));

        config.setHA(HA.resolveModelAttribute(context, model).asBoolean());
        config.setAutoGroup(Common.AUTO_GROUP.resolveModelAttribute(context, model).asBoolean());
        config.setBlockOnAcknowledge(Common.BLOCK_ON_ACKNOWLEDGE.resolveModelAttribute(context, model).asBoolean());
        config.setBlockOnDurableSend(Common.BLOCK_ON_DURABLE_SEND.resolveModelAttribute(context, model).asBoolean());
        config.setBlockOnNonDurableSend(Common.BLOCK_ON_NON_DURABLE_SEND.resolveModelAttribute(context, model).asBoolean());
        config.setCacheLargeMessagesClient(Common.CACHE_LARGE_MESSAGE_CLIENT.resolveModelAttribute(context, model).asBoolean());
        config.setCallTimeout(CALL_TIMEOUT.resolveModelAttribute(context, model).asLong());
        config.setClientFailureCheckPeriod(Common.CLIENT_FAILURE_CHECK_PERIOD.resolveModelAttribute(context, model).asInt());
        config.setCallFailoverTimeout(CALL_FAILOVER_TIMEOUT.resolveModelAttribute(context, model).asLong());
        final ModelNode clientId = CLIENT_ID.resolveModelAttribute(context, model);
        if (clientId.isDefined()) {
            config.setClientID(clientId.asString());
        }
        config.setCompressLargeMessages(Common.COMPRESS_LARGE_MESSAGES.resolveModelAttribute(context, model).asBoolean());
        config.setConfirmationWindowSize(Common.CONFIRMATION_WINDOW_SIZE.resolveModelAttribute(context, model).asInt());
        config.setConnectionTTL(Common.CONNECTION_TTL.resolveModelAttribute(context, model).asLong());
        List<String> connectorNames = Common.CONNECTORS.unwrap(context, model);
        config.setConnectorNames(connectorNames);
        config.setConsumerMaxRate(Common.CONSUMER_MAX_RATE.resolveModelAttribute(context, model).asInt());
        config.setConsumerWindowSize(Common.CONSUMER_WINDOW_SIZE.resolveModelAttribute(context, model).asInt());
        final ModelNode discoveryGroupName = Common.DISCOVERY_GROUP.resolveModelAttribute(context, model);
        if (discoveryGroupName.isDefined()) {
            config.setDiscoveryGroupName(discoveryGroupName.asString());
        }

        config.setDupsOKBatchSize(Common.DUPS_OK_BATCH_SIZE.resolveModelAttribute(context, model).asInt());
        config.setFailoverOnInitialConnection(Common.FAILOVER_ON_INITIAL_CONNECTION.resolveModelAttribute(context, model).asBoolean());

        final ModelNode groupId = Common.GROUP_ID.resolveModelAttribute(context, model);
        if (groupId.isDefined()) {
            config.setGroupID(groupId.asString());
        }

        final ModelNode lbcn = Common.CONNECTION_LOAD_BALANCING_CLASS_NAME.resolveModelAttribute(context, model);
        if (lbcn.isDefined()) {
            config.setLoadBalancingPolicyClassName(lbcn.asString());
        }
        config.setMaxRetryInterval(Common.MAX_RETRY_INTERVAL.resolveModelAttribute(context, model).asLong());
        config.setMinLargeMessageSize(Common.MIN_LARGE_MESSAGE_SIZE.resolveModelAttribute(context, model).asInt());
        config.setPreAcknowledge(Common.PRE_ACKNOWLEDGE.resolveModelAttribute(context, model).asBoolean());
        config.setProducerMaxRate(Common.PRODUCER_MAX_RATE.resolveModelAttribute(context, model).asInt());
        config.setProducerWindowSize(Common.PRODUCER_WINDOW_SIZE.resolveModelAttribute(context, model).asInt());
        config.setReconnectAttempts(Common.RECONNECT_ATTEMPTS.resolveModelAttribute(context, model).asInt());
        config.setRetryInterval(Common.RETRY_INTERVAL.resolveModelAttribute(context, model).asLong());
        config.setRetryIntervalMultiplier(Common.RETRY_INTERVAL_MULTIPLIER.resolveModelAttribute(context, model).asDouble());
        config.setScheduledThreadPoolMaxSize(Common.SCHEDULED_THREAD_POOL_MAX_SIZE.resolveModelAttribute(context, model).asInt());
        config.setThreadPoolMaxSize(Common.THREAD_POOL_MAX_SIZE.resolveModelAttribute(context, model).asInt());
        config.setTransactionBatchSize(Common.TRANSACTION_BATCH_SIZE.resolveModelAttribute(context, model).asInt());
        config.setUseGlobalPools(Common.USE_GLOBAL_POOLS.resolveModelAttribute(context, model).asBoolean());
        config.setLoadBalancingPolicyClassName(Common.CONNECTION_LOAD_BALANCING_CLASS_NAME.resolveModelAttribute(context, model).asString());
        final ModelNode clientProtocolManagerFactory = Common.PROTOCOL_MANAGER_FACTORY.resolveModelAttribute(context, model);
        if (clientProtocolManagerFactory.isDefined()) {
            config.setProtocolManagerFactoryStr(clientProtocolManagerFactory.asString());
        }
        if (model.hasDefined(DESERIALIZATION_BLOCKLIST.getName())) {
            List<String> deserializationBlockList = DESERIALIZATION_BLOCKLIST.unwrap(context, model);
            if (!deserializationBlockList.isEmpty()) {
                config.setDeserializationBlackList(String.join(",", deserializationBlockList));
            }
        }
        if (model.hasDefined(DESERIALIZATION_ALLOWLIST.getName())) {
            List<String> deserializationAllowList = DESERIALIZATION_ALLOWLIST.unwrap(context, model);
            if (!deserializationAllowList.isEmpty()) {
                config.setDeserializationWhiteList(String.join(",", deserializationAllowList));
            }
        }
        JMSFactoryType jmsFactoryType = ConnectionFactoryType.valueOf(ConnectionFactoryAttributes.Regular.FACTORY_TYPE.resolveModelAttribute(context, model).asString()).getType();
        config.setFactoryType(jmsFactoryType);

        config.setInitialMessagePacketSize(Common.INITIAL_MESSAGE_PACKET_SIZE.resolveModelAttribute(context, model).asInt());
        config.setEnableSharedClientID(true);
        config.setEnable1xPrefixes(true);
        config.setUseTopologyForLoadBalancing(Common.USE_TOPOLOGY.resolveModelAttribute(context, model).asBoolean());
        return config;
    }

    static DiscoveryGroupConfiguration getDiscoveryGroup(final OperationContext context, final String name) throws OperationFailedException {
        Resource discoveryGroup;
        try {
            discoveryGroup = context.readResourceFromRoot(context.getCurrentAddress().getParent().append(PathElement.pathElement(CommonAttributes.JGROUPS_DISCOVERY_GROUP, name)), true);
        } catch (Resource.NoSuchResourceException ex) {
            discoveryGroup = context.readResourceFromRoot(context.getCurrentAddress().getParent().append(PathElement.pathElement(CommonAttributes.SOCKET_DISCOVERY_GROUP, name)), true);
        }
        if (discoveryGroup != null) {
            final long refreshTimeout = DiscoveryGroupDefinition.REFRESH_TIMEOUT.resolveModelAttribute(context, discoveryGroup.getModel()).asLong();
            final long initialWaitTimeout = DiscoveryGroupDefinition.INITIAL_WAIT_TIMEOUT.resolveModelAttribute(context, discoveryGroup.getModel()).asLong();

            return new DiscoveryGroupConfiguration()
                    .setName(name)
                    .setRefreshTimeout(refreshTimeout)
                    .setDiscoveryInitialWaitTimeout(initialWaitTimeout);
        }
        return null;
    }

    @Override
    protected void populateModel(final ModelNode operation, final ModelNode model) throws OperationFailedException {
        for (AttributeDefinition attr : attributes) {
            if (DESERIALIZATION_BLACKLIST.equals(attr)) {
                if (operation.hasDefined(DESERIALIZATION_BLACKLIST.getName())) {
                    DESERIALIZATION_BLOCKLIST.validateAndSet(operation, model);
                }
            } else if (DESERIALIZATION_WHITELIST.equals(attr)) {
                if (operation.hasDefined(DESERIALIZATION_WHITELIST.getName())) {
                    DESERIALIZATION_ALLOWLIST.validateAndSet(operation, model);
                }
            } else {
                attr.validateAndSet(operation, model);
            }
        }
    }
}

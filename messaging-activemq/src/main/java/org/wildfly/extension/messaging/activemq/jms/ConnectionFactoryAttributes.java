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
package org.wildfly.extension.messaging.activemq.jms;

import static org.jboss.as.controller.SimpleAttributeDefinitionBuilder.create;
import static org.jboss.as.controller.client.helpers.MeasurementUnit.BYTES;
import static org.jboss.as.controller.client.helpers.MeasurementUnit.MILLISECONDS;
import static org.jboss.as.controller.client.helpers.MeasurementUnit.PER_SECOND;
import static org.jboss.dmr.ModelType.BOOLEAN;
import static org.jboss.dmr.ModelType.DOUBLE;
import static org.jboss.dmr.ModelType.INT;
import static org.jboss.dmr.ModelType.LONG;
import static org.jboss.dmr.ModelType.STRING;
import static org.wildfly.extension.messaging.activemq.AbstractTransportDefinition.CONNECTOR_CAPABILITY_NAME;
import static org.wildfly.extension.messaging.activemq.MessagingExtension.MESSAGING_SECURITY_SENSITIVE_TARGET;
import static org.wildfly.extension.messaging.activemq.jms.ConnectionFactoryAttribute.ConfigType.INBOUND;
import static org.wildfly.extension.messaging.activemq.jms.ConnectionFactoryAttribute.ConfigType.OUTBOUND;
import static org.wildfly.extension.messaging.activemq.jms.ConnectionFactoryAttribute.create;
import static org.wildfly.extension.messaging.activemq.jms.ConnectionFactoryDefinition.CAPABILITY_NAME;

import java.util.Arrays;

import org.apache.activemq.artemis.api.config.ActiveMQDefaultConfiguration;
import org.apache.activemq.artemis.api.core.client.ActiveMQClient;
import org.apache.activemq.artemis.api.core.client.loadbalance.RoundRobinConnectionLoadBalancingPolicy;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.AttributeMarshaller;
import org.jboss.as.controller.AttributeParser;
import org.jboss.as.controller.ObjectTypeAttributeDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ParameterCorrector;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.validation.StringAllowedValuesValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.security.CredentialReference;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.extension.messaging.activemq.AbstractTransportDefinition;
import org.wildfly.extension.messaging.activemq.CommonAttributes;
import org.wildfly.extension.messaging.activemq.InfiniteOrPositiveValidators;
import org.wildfly.extension.messaging.activemq.logging.MessagingLogger;

public interface ConnectionFactoryAttributes {

    interface Common {
    /**
     * @see ActiveMQClient.DEFAULT_AUTO_GROUP
     */
        AttributeDefinition AUTO_GROUP = SimpleAttributeDefinitionBuilder.create("auto-group", BOOLEAN)
                .setDefaultValue(ModelNode.FALSE)
                .setRequired(false)
                .setAllowExpression(true)
                .setRestartAllServices()
                .build();

    /**
     * @see ActiveMQClient.DEFAULT_BLOCK_ON_ACKNOWLEDGE
     */
        AttributeDefinition BLOCK_ON_ACKNOWLEDGE = SimpleAttributeDefinitionBuilder.create("block-on-acknowledge", BOOLEAN)
                .setDefaultValue(ModelNode.FALSE)
                .setRequired(false)
                .setAllowExpression(true)
                .setRestartAllServices()
                .build();

    /**
     * @see ActiveMQClient.DEFAULT_BLOCK_ON_DURABLE_SEND
     */
        AttributeDefinition BLOCK_ON_DURABLE_SEND = SimpleAttributeDefinitionBuilder.create("block-on-durable-send", BOOLEAN)
                .setDefaultValue(ModelNode.TRUE)
                .setRequired(false)
                .setAllowExpression(true)
                .setRestartAllServices()
                .build();

    /**
     * @see ActiveMQClient.DEFAULT_BLOCK_ON_NON_DURABLE_SEND
     */
        AttributeDefinition BLOCK_ON_NON_DURABLE_SEND = SimpleAttributeDefinitionBuilder.create("block-on-non-durable-send", BOOLEAN)
                .setDefaultValue(ModelNode.FALSE)
                .setRequired(false)
                .setAllowExpression(true)
                .setRestartAllServices()
                .build();

    /**
     * @see ActiveMQClient.DEFAULT_CACHE_LARGE_MESSAGE_CLIENT
     */
        AttributeDefinition CACHE_LARGE_MESSAGE_CLIENT = SimpleAttributeDefinitionBuilder.create("cache-large-message-client", BOOLEAN)
                .setDefaultValue(ModelNode.FALSE)
                .setRequired(false)
                .setAllowExpression(true)
                .setRestartAllServices()
                .build();

    /**
     * @see ActiveMQClient.DEFAULT_CLIENT_FAILURE_CHECK_PERIOD
     */
        AttributeDefinition CLIENT_FAILURE_CHECK_PERIOD =SimpleAttributeDefinitionBuilder.create("client-failure-check-period", LONG)
                .setDefaultValue(new ModelNode(30000L))
                .setMeasurementUnit(MILLISECONDS)
                .setRequired(false)
                .setAllowExpression(true)
                .setValidator(InfiniteOrPositiveValidators.LONG_INSTANCE)
                .setRestartAllServices()
                .build();

    /**
     * @see ActiveMQClient.DEFAULT_COMPRESS_LARGE_MESSAGES
     */
        AttributeDefinition COMPRESS_LARGE_MESSAGES = SimpleAttributeDefinitionBuilder.create("compress-large-messages", BOOLEAN)
                .setDefaultValue(ModelNode.FALSE)
                .setRequired(false)
                .setAllowExpression(true)
                .setRestartAllServices()
                .build();

    /**
     * @see ActiveMQClient.DEFAULT_CONFIRMATION_WINDOW_SIZE
     */
        AttributeDefinition CONFIRMATION_WINDOW_SIZE = SimpleAttributeDefinitionBuilder.create("confirmation-window-size", INT)
                .setDefaultValue(new ModelNode(-1))
                .setMeasurementUnit(BYTES)
                .setRequired(false)
                .setAllowExpression(true)
                .setCorrector(InfiniteOrPositiveValidators.NEGATIVE_VALUE_CORRECTOR)
                .setValidator(InfiniteOrPositiveValidators.INT_INSTANCE)
                .setRestartAllServices()
                .build();

    /**
     * @see ActiveMQClient.DEFAULT_CONNECTION_LOAD_BALANCING_POLICY_CLASS_NAME
     */
        AttributeDefinition CONNECTION_LOAD_BALANCING_CLASS_NAME = SimpleAttributeDefinitionBuilder.create("connection-load-balancing-policy-class-name", STRING)
                .setDefaultValue(new ModelNode(RoundRobinConnectionLoadBalancingPolicy.class.getCanonicalName()))
                .setRequired(false)
                .setAllowExpression(false)
                .setRestartAllServices()
                .build();

    /**
     * @see ActiveMQClient.DEFAULT_CONNECTION_TTL
     */
        AttributeDefinition CONNECTION_TTL = new SimpleAttributeDefinitionBuilder("connection-ttl", LONG)
                .setDefaultValue(new ModelNode(60000L))
                .setRequired(false)
                .setAllowExpression(true)
                .setValidator(InfiniteOrPositiveValidators.LONG_INSTANCE)
                .setMeasurementUnit(MILLISECONDS)
                .setRestartAllServices()
                .build();

        StringListAttributeDefinition CONNECTORS = new StringListAttributeDefinition.Builder(CommonAttributes.CONNECTORS)
                .setAlternatives(CommonAttributes.DISCOVERY_GROUP)
                .setRequired(true)
                .setAttributeParser(AttributeParser.STRING_LIST)
                .setAttributeMarshaller(AttributeMarshaller.STRING_LIST)
                .setCapabilityReference(new AbstractTransportDefinition.TransportCapabilityReferenceRecorder(CAPABILITY_NAME, CONNECTOR_CAPABILITY_NAME, false))
                .setRestartAllServices()
                .build();

    /**
     * @see ActiveMQClient.DEFAULT_CONSUMER_MAX_RATE
     */
        AttributeDefinition CONSUMER_MAX_RATE = SimpleAttributeDefinitionBuilder.create("consumer-max-rate", INT)
                .setDefaultValue(new ModelNode(-1))
                .setMeasurementUnit(PER_SECOND)
                .setRequired(false)
                .setAllowExpression(true)
                .setCorrector(InfiniteOrPositiveValidators.NEGATIVE_VALUE_CORRECTOR)
                .setValidator(InfiniteOrPositiveValidators.INT_INSTANCE)
                .setRestartAllServices()
                .build();

    /**
     * @see ActiveMQClient.DEFAULT_CONSUMER_WINDOW_SIZE
     */
        AttributeDefinition CONSUMER_WINDOW_SIZE = SimpleAttributeDefinitionBuilder.create("consumer-window-size", INT)
                .setDefaultValue(new ModelNode(1024 * 1024))
                .setMeasurementUnit(BYTES)
                .setRequired(false)
                .setAllowExpression(true)
                .setRestartAllServices()
                .build();

        StringListAttributeDefinition DESERIALIZATION_WHITELIST = new StringListAttributeDefinition.Builder("deserialization-white-list")
                .setRequired(false)
                .setAllowExpression(true)
                .setListValidator(Validators.noDuplicateElements(new StringLengthValidator(1, true, true)))
                .setAttributeParser(AttributeParser.STRING_LIST)
                .setAttributeMarshaller(AttributeMarshaller.STRING_LIST)
                .setRestartAllServices()
                .build();

        StringListAttributeDefinition DESERIALIZATION_BLACKLIST = new StringListAttributeDefinition.Builder("deserialization-black-list")
                .setRequired(false)
                .setAllowExpression(true)
                .setListValidator(Validators.noDuplicateElements(new StringLengthValidator(1, true, true)))
                .setAttributeParser(AttributeParser.STRING_LIST)
                .setAttributeMarshaller(AttributeMarshaller.STRING_LIST)
                .setRestartAllServices()
                .build();

        SimpleAttributeDefinition DISCOVERY_GROUP =  SimpleAttributeDefinitionBuilder.create(CommonAttributes.DISCOVERY_GROUP, STRING)
                .setRequired(true)
                .setAlternatives(CommonAttributes.CONNECTORS)
                .setRestartAllServices()
                .build();

    /**
     * @see ActiveMQClient.DEFAULT_ACK_BATCH_SIZE
     */
        AttributeDefinition DUPS_OK_BATCH_SIZE = SimpleAttributeDefinitionBuilder.create("dups-ok-batch-size", INT)
                .setDefaultValue(new ModelNode(1024 * 1024))
                .setRequired(false)
                .setAllowExpression(true)
                .setRestartAllServices()
                .build();

        StringListAttributeDefinition ENTRIES = new StringListAttributeDefinition.Builder(CommonAttributes.ENTRIES)
                .setRequired(true)
                .setAllowExpression(true)
                .setListValidator(Validators.noDuplicateElements(new StringLengthValidator(1, false, true)))
                .setAttributeParser(AttributeParser.STRING_LIST)
                .setAttributeMarshaller(AttributeMarshaller.STRING_LIST)
                .setRestartAllServices()
                .build();

    /**
     * @see ActiveMQClient.DEFAULT_FAILOVER_ON_INITIAL_CONNECTION
     */
        AttributeDefinition FAILOVER_ON_INITIAL_CONNECTION = SimpleAttributeDefinitionBuilder.create("failover-on-initial-connection", BOOLEAN)
                .setDefaultValue(ModelNode.FALSE)
                .setRequired(false)
                .setAllowExpression(true)
                .setRestartAllServices()
                .build();

        AttributeDefinition GROUP_ID = SimpleAttributeDefinitionBuilder.create("group-id", STRING)
                .setRequired(false)
                .setAllowExpression(true)
                .setRestartAllServices()
                .build();

        AttributeDefinition INITIAL_MESSAGE_PACKET_SIZE = create("initial-message-packet-size", INT)
                .setAllowExpression(true)
                .setRestartAllServices()
                .setRequired(false)
                .setDefaultValue(new ModelNode(1500))
                .setMeasurementUnit(BYTES)
                .build();

    /**
     * @see ActiveMQClient.DEFAULT_MAX_RETRY_INTERVAL
     */
        AttributeDefinition MAX_RETRY_INTERVAL = SimpleAttributeDefinitionBuilder.create("max-retry-interval", LONG)
                .setDefaultValue(new ModelNode(2000L))
                .setMeasurementUnit(MILLISECONDS)
                .setRequired(false)
                .setAllowExpression(true)
                .setRestartAllServices()
                .build();

    /**
     * @see ActiveMQClient.DEFAULT_MIN_LARGE_MESSAGE_SIZE
     */
        AttributeDefinition MIN_LARGE_MESSAGE_SIZE = SimpleAttributeDefinitionBuilder.create("min-large-message-size", INT)
                .setDefaultValue(new ModelNode().set(100 * 1024))
                .setMeasurementUnit(BYTES)
                .setRequired(false)
                .setAllowExpression(true)
                .setRestartAllServices()
                .build();

    /**
     * @see ActiveMQClient.DEFAULT_PRE_ACKNOWLEDGE
     */
        AttributeDefinition PRE_ACKNOWLEDGE = SimpleAttributeDefinitionBuilder.create("pre-acknowledge", BOOLEAN)
                .setDefaultValue(ModelNode.FALSE)
                .setRequired(false)
                .setAllowExpression(true)
                .setRestartAllServices()
                .build();

    /**
     * @see ActiveMQClient.DEFAULT_PRODUCER_MAX_RATE
     */
        AttributeDefinition PRODUCER_MAX_RATE = SimpleAttributeDefinitionBuilder.create("producer-max-rate", INT)
                .setDefaultValue(new ModelNode(-1))
                .setMeasurementUnit(PER_SECOND)
                .setRequired(false)
                .setAllowExpression(true)
                .setCorrector(InfiniteOrPositiveValidators.NEGATIVE_VALUE_CORRECTOR)
                .setValidator(InfiniteOrPositiveValidators.INT_INSTANCE)
                .setRestartAllServices()
                .build();

    /**
     * @see ActiveMQClient.DEFAULT_PRODUCER_WINDOW_SIZE
     */
        AttributeDefinition PRODUCER_WINDOW_SIZE = SimpleAttributeDefinitionBuilder.create("producer-window-size", INT)
                .setDefaultValue(new ModelNode(64 * 1024))
                .setMeasurementUnit(BYTES)
                .setRequired(false)
                .setAllowExpression(true)
                .setRestartAllServices()
                .build();

        AttributeDefinition PROTOCOL_MANAGER_FACTORY = SimpleAttributeDefinitionBuilder.create("protocol-manager-factory", STRING)
                .setRequired(false)
                .setRestartAllServices()
                .build();

        /**
         * @see ActiveMQClient.DEFAULT_RECONNECT_ATTEMPTS
         */
        SimpleAttributeDefinition RECONNECT_ATTEMPTS = create("reconnect-attempts", INT)
                .setDefaultValue(ModelNode.ZERO)
                .setRequired(false)
                .setAllowExpression(true)
                .setRestartAllServices()
                .build();

        /**
         * @see ActiveMQClient.DEFAULT_RETRY_INTERVAL
         */
        AttributeDefinition RETRY_INTERVAL = SimpleAttributeDefinitionBuilder.create("retry-interval", LONG)
                .setDefaultValue(new ModelNode(2000L))
                .setMeasurementUnit(MILLISECONDS)
                .setRequired(false)
                .setAllowExpression(true)
                .setRestartAllServices()
                .build();

        /**
         * @see ActiveMQClient.DEFAULT_RETRY_INTERVAL_MULTIPLIER
         */
        AttributeDefinition RETRY_INTERVAL_MULTIPLIER = create("retry-interval-multiplier", DOUBLE)
                .setDefaultValue(new ModelNode(1.0d))
                .setRequired(false)
                .setAllowExpression(true)
                .setRestartAllServices()
                .build();

        /**
         * @see ActiveMQDefaultConfiguration#getDefaultScheduledThreadPoolMaxSize
         */
        AttributeDefinition SCHEDULED_THREAD_POOL_MAX_SIZE = SimpleAttributeDefinitionBuilder.create("scheduled-thread-pool-max-size", INT)
                .setDefaultValue(new ModelNode(5))
                .setRequired(false)
                .setAllowExpression(true)
                .setCorrector(InfiniteOrPositiveValidators.NEGATIVE_VALUE_CORRECTOR)
                .setValidator(InfiniteOrPositiveValidators.INT_INSTANCE)
                .setRestartAllServices()
                .build();

        /**
         * @see ActiveMQDefaultConfiguration#getDefaultThreadPoolMaxSize
         */
        AttributeDefinition THREAD_POOL_MAX_SIZE = SimpleAttributeDefinitionBuilder.create("thread-pool-max-size", INT)
                .setDefaultValue(new ModelNode(30))
                .setRequired(false)
                .setAllowExpression(true)
                .setCorrector(InfiniteOrPositiveValidators.NEGATIVE_VALUE_CORRECTOR)
                .setValidator(InfiniteOrPositiveValidators.INT_INSTANCE)
                .setRestartAllServices()
                .build();

        /**
         * @see ActiveMQClient.DEFAULT_ACK_BATCH_SIZE
         */
        AttributeDefinition TRANSACTION_BATCH_SIZE = SimpleAttributeDefinitionBuilder.create("transaction-batch-size", INT)
                .setDefaultValue(new ModelNode(1024 * 1024))
                .setRequired(false)
                .setAllowExpression(true)
                .setRestartAllServices()
                .build();

        /**
         * @see ActiveMQClient.DEFAULT_USE_GLOBAL_POOLS
         */
        AttributeDefinition USE_GLOBAL_POOLS = SimpleAttributeDefinitionBuilder.create("use-global-pools", BOOLEAN)
                .setDefaultValue(ModelNode.TRUE)
                .setRequired(false)
                .setAllowExpression(true)
                .setRestartAllServices()
                .build();


        /**
         * @see ActiveMQClient.DEFAULT_USE_TOPOLOGY_FOR_LOADBALANCING
         */
        AttributeDefinition USE_TOPOLOGY = create("use-topology-for-load-balancing", BOOLEAN)
                .setDefaultValue(ModelNode.TRUE)
                .setRequired(false)
                .setAllowExpression(true)
                .setRestartAllServices()
                .build();

        /**
         * Attributes are defined in the <em>same order than in the XSD schema</em>
         */
        ConnectionFactoryAttribute[] ATTRIBUTES = {
                create(DISCOVERY_GROUP, null, false),
                create(CONNECTORS, null, false),
                create(ENTRIES, "entries", true),
                create(CommonAttributes.HA, "HA", true),
                create(CLIENT_FAILURE_CHECK_PERIOD, "clientFailureCheckPeriod", true),
                create(CONNECTION_TTL, "connectionTTL", true),
                create(CommonAttributes.CALL_TIMEOUT, "callTimeout", true),
                create(CommonAttributes.CALL_FAILOVER_TIMEOUT, "callFailoverTimeout", true),
                create(CONSUMER_WINDOW_SIZE, "consumerWindowSize", true),
                create(CONSUMER_MAX_RATE, "consumerMaxRate", true),
                create(CONFIRMATION_WINDOW_SIZE, "confirmationWindowSize", true),
                create(PRODUCER_WINDOW_SIZE, "producerWindowSize", true),
                create(PRODUCER_MAX_RATE, "producerMaxRate", true),
                create(PROTOCOL_MANAGER_FACTORY, "protocolManagerFactoryStr", true),
                create(COMPRESS_LARGE_MESSAGES, "compressLargeMessage", true),
                create(CACHE_LARGE_MESSAGE_CLIENT, "cacheLargeMessagesClient", true),
                create(CommonAttributes.MIN_LARGE_MESSAGE_SIZE, "minLargeMessageSize", true),
                create(CommonAttributes.CLIENT_ID, "clientID", true),
                create(DUPS_OK_BATCH_SIZE, "dupsOKBatchSize", true),
                create(TRANSACTION_BATCH_SIZE, "transactionBatchSize", true),
                create(BLOCK_ON_ACKNOWLEDGE, "blockOnAcknowledge", true),
                create(BLOCK_ON_NON_DURABLE_SEND, "blockOnNonDurableSend", true),
                create(BLOCK_ON_DURABLE_SEND, "blockOnDurableSend", true),
                create(AUTO_GROUP, "autoGroup", true),
                create(PRE_ACKNOWLEDGE, "preAcknowledge", true),
                create(RETRY_INTERVAL, "retryInterval", true),
                create(RETRY_INTERVAL_MULTIPLIER, "retryIntervalMultiplier", true),
                create(CommonAttributes.MAX_RETRY_INTERVAL, "maxRetryInterval", true),
                // the pooled CF has a different default value for the reconnect-attempts attribute.
                // the specific attribute is replaced when PooledConnectionFactoryDefinition#ATTRIBUTES is defined
                create(RECONNECT_ATTEMPTS, null, false),
                create(FAILOVER_ON_INITIAL_CONNECTION, "failoverOnInitialConnection", true),
                create(CONNECTION_LOAD_BALANCING_CLASS_NAME, "connectionLoadBalancingPolicyClassName", true),
                create(USE_GLOBAL_POOLS, "useGlobalPools", true),
                create(SCHEDULED_THREAD_POOL_MAX_SIZE, "scheduledThreadPoolMaxSize", true),
                create(THREAD_POOL_MAX_SIZE, "threadPoolMaxSize", true),
                create(GROUP_ID, "groupID", true),
                create(DESERIALIZATION_BLACKLIST, "deserializationBlackList", true),
                create(DESERIALIZATION_WHITELIST, "deserializationWhiteList", true),
                create(INITIAL_MESSAGE_PACKET_SIZE, "initialMessagePacketSize", true),
                create(USE_TOPOLOGY, "useTopologyForLoadBalancing", true)
        };
    }

    interface Regular {
        AttributeDefinition FACTORY_TYPE = create("factory-type", STRING)
                .setDefaultValue(new ModelNode().set(ConnectionFactoryType.GENERIC.toString()))
                .setValidator(ConnectionFactoryType.VALIDATOR)
                .setRequired(false)
                .setAllowExpression(true)
                .setRestartAllServices()
                .build();

        AttributeDefinition[] ATTRIBUTES = { FACTORY_TYPE } ;

    }

    interface Pooled {
        String ALLOW_LOCAL_TRANSACTIONS_PROP_NAME = "allowLocalTransactions";
        String USE_JNDI_PROP_NAME = "useJNDI";
        String SETUP_ATTEMPTS_PROP_NAME = "setupAttempts";
        String SETUP_INTERVAL_PROP_NAME = "setupInterval";
        String REBALANCE_CONNECTIONS_PROP_NAME = "rebalanceConnections";
        String RECONNECT_ATTEMPTS_PROP_NAME = "reconnectAttempts";
        String PASSWORD_PROP_NAME = "password";


        SimpleAttributeDefinition ALLOW_LOCAL_TRANSACTIONS = SimpleAttributeDefinitionBuilder.create("allow-local-transactions", BOOLEAN)
                .setAttributeGroup("outbound-config")
                .setRequired(false)
                .setAllowExpression(true)
                .setDefaultValue(ModelNode.FALSE)
                .setRestartAllServices()
                .build();

        ObjectTypeAttributeDefinition CREDENTIAL_REFERENCE =
                CredentialReference.getAttributeBuilder(true, false)
                        .setRestartAllServices()
                        .addAccessConstraint(MESSAGING_SECURITY_SENSITIVE_TARGET)
                        .setAlternatives(PASSWORD_PROP_NAME)
                        .build();

        SimpleAttributeDefinition ENLISTMENT_TRACE = SimpleAttributeDefinitionBuilder.create("enlistment-trace", BOOLEAN)
                .setRequired(false)
                .setAllowExpression(true)
                // no default value, this boolean is undefined
                .setRestartAllServices()
                .build();

        /**
         * @see ActiveMQClient.INITIAL_CONNECT_ATTEMPTS
         */
        SimpleAttributeDefinition INITIAL_CONNECT_ATTEMPTS = SimpleAttributeDefinitionBuilder.create("initial-connect-attempts", INT)
                .setRequired(false)
                .setAllowExpression(true)
                .setDefaultValue(new ModelNode(1))
                .setRestartAllServices()
                .build();

        SimpleAttributeDefinition JNDI_PARAMS = SimpleAttributeDefinitionBuilder.create("jndi-params", STRING)
                .setAttributeGroup("inbound-config")
                .setRequired(false)
                .setAllowExpression(true)
                .setRestartAllServices()
                .build();

        SimpleAttributeDefinition MANAGED_CONNECTION_POOL = SimpleAttributeDefinitionBuilder.create("managed-connection-pool", STRING)
                .setAllowExpression(true)
                .setRequired(false)
                .setRestartAllServices()
                .build();

        SimpleAttributeDefinition MAX_POOL_SIZE = SimpleAttributeDefinitionBuilder.create("max-pool-size", INT)
                .setDefaultValue(new ModelNode().set(20))
                .setRequired(false)
                .setAllowExpression(true)
                .setRestartAllServices()
                .build();

        SimpleAttributeDefinition MIN_POOL_SIZE = SimpleAttributeDefinitionBuilder.create("min-pool-size", INT)
                .setDefaultValue(ModelNode.ZERO)
                .setRequired(false)
                .setAllowExpression(true)
                .setRestartAllServices()
                .build();

        SimpleAttributeDefinition PASSWORD = SimpleAttributeDefinitionBuilder.create(PASSWORD_PROP_NAME, STRING)
                .setRequired(false)
                .setAllowExpression(true)
                .setRestartAllServices()
                .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.CREDENTIAL)
                .addAccessConstraint(MESSAGING_SECURITY_SENSITIVE_TARGET)
                .addAlternatives(CREDENTIAL_REFERENCE.getName())
                .build();

        SimpleAttributeDefinition REBALANCE_CONNECTIONS = SimpleAttributeDefinitionBuilder.create("rebalance-connections", BOOLEAN)
                .setRequired(false)
                .setAllowExpression(true)
                .setDefaultValue(ModelNode.FALSE)
                .setAttributeGroup("inbound-config")
                .setRestartAllServices()
                .build();

        /**
         * By default, the resource adapter must reconnect infinitely (see {@link org.apache.activemq.artemis.ra.ActiveMQResourceAdapter#setParams})
         */
        AttributeDefinition RECONNECT_ATTEMPTS = create("reconnect-attempts", INT)
                .setDefaultValue(new ModelNode().set(-1))
                .setRequired(false)
                .setAllowExpression(true)
                .build();

        SimpleAttributeDefinition SETUP_ATTEMPTS = SimpleAttributeDefinitionBuilder.create("setup-attempts", INT)
                .setAttributeGroup("inbound-config")
                .setRequired(false)
                .setAllowExpression(true)
                .setRestartAllServices()
                .build();

        SimpleAttributeDefinition SETUP_INTERVAL =  SimpleAttributeDefinitionBuilder.create("setup-interval", LONG)
                .setMeasurementUnit(MILLISECONDS)
                .setAttributeGroup("inbound-config")
                .setRequired(false)
                .setAllowExpression(true)
                .setRestartAllServices()
                .build();

        SimpleAttributeDefinition TRANSACTION = SimpleAttributeDefinitionBuilder.create("transaction", STRING)
                .setDefaultValue(new ModelNode().set(CommonAttributes.XA))
                .setCorrector(new ParameterCorrector() {
                    @Override
                    public ModelNode correct(ModelNode newValue, ModelNode currentValue) {
                        if (newValue.isDefined() && newValue.getType() != ModelType.EXPRESSION) {
                            switch (newValue.asString()) {
                                case CommonAttributes.LOCAL:
                                    return new ModelNode(CommonAttributes.LOCAL);
                                case CommonAttributes.NONE:
                                    return new ModelNode(CommonAttributes.NONE);
                                case CommonAttributes.XA:
                                    return new ModelNode(CommonAttributes.XA);
                                default:
                                    MessagingLogger.ROOT_LOGGER.invalidTransactionNameValue(newValue.asString(), "transaction", Arrays.asList(CommonAttributes.LOCAL, CommonAttributes.NONE, CommonAttributes.XA));
                                    return new ModelNode(CommonAttributes.XA);
                            }
                        }
                        return newValue;
                    }
                })
                .setValidator(new TransactionNameAllowedValuesValidator(CommonAttributes.LOCAL, CommonAttributes.NONE, CommonAttributes.XA))
                .setRequired(false)
                .setAllowExpression(true)
                .setRestartAllServices()
                .build();

        AttributeDefinition USE_AUTO_RECOVERY = SimpleAttributeDefinitionBuilder.create("use-auto-recovery", BOOLEAN)
                .setDefaultValue(ModelNode.TRUE) // ActiveMQQResourceAdapter.useAutoRecovery = true but is not exposed publicly
                .setRequired(false)
                .setAllowExpression(true)
                .setRestartAllServices()
                .build();

        AttributeDefinition USE_JNDI = SimpleAttributeDefinitionBuilder.create("use-jndi", BOOLEAN)
                .setAttributeGroup("inbound-config")
                .setRequired(false)
                .setAllowExpression(true)
                .setRestartAllServices()
                .build();

        AttributeDefinition USE_LOCAL_TX = SimpleAttributeDefinitionBuilder.create("use-local-tx", BOOLEAN)
                .setAttributeGroup("inbound-config")
                .setRequired(false)
                .setAllowExpression(true)
                .setRestartAllServices()
                .build();

        SimpleAttributeDefinition USER = SimpleAttributeDefinitionBuilder.create("user", STRING)
                .setRequired(false)
                .setAllowExpression(true)
                .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.CREDENTIAL)
                .addAccessConstraint(MESSAGING_SECURITY_SENSITIVE_TARGET)
                .build();

        SimpleAttributeDefinition STATISTICS_ENABLED = SimpleAttributeDefinitionBuilder.create(ModelDescriptionConstants.STATISTICS_ENABLED, BOOLEAN)
                .setDefaultValue(ModelNode.FALSE)
                .setRequired(false)
                .setAllowExpression(true)
                .build();

        /**
         * Attributes are defined in the <em>same order than in the XSD schema</em>
         */
        ConnectionFactoryAttribute[] ATTRIBUTES = {
                /* inbound config */
                create(USE_JNDI, USE_JNDI_PROP_NAME, true, INBOUND),
                create(JNDI_PARAMS, "jndiParams", true, INBOUND),
                create(REBALANCE_CONNECTIONS, REBALANCE_CONNECTIONS_PROP_NAME, true, INBOUND),
                create(USE_LOCAL_TX, "useLocalTx", true, INBOUND),
                create(SETUP_ATTEMPTS, SETUP_ATTEMPTS_PROP_NAME, true, INBOUND),
                create(SETUP_INTERVAL, SETUP_INTERVAL_PROP_NAME, true, INBOUND),
                /* outbound config */
                create(ALLOW_LOCAL_TRANSACTIONS, ALLOW_LOCAL_TRANSACTIONS_PROP_NAME, true, OUTBOUND),

                create(STATISTICS_ENABLED, null, false),
                create(TRANSACTION, null, false),
                create(USER, "userName", true),
                create(PASSWORD, "password", true),
                create(CREDENTIAL_REFERENCE, null, false),
                create(MANAGED_CONNECTION_POOL, null, false),
                create(ENLISTMENT_TRACE, null, false),
                create(MIN_POOL_SIZE, null, false),
                create(MAX_POOL_SIZE, null, false),
                create(USE_AUTO_RECOVERY, "useAutoRecovery", true),
                create(INITIAL_CONNECT_ATTEMPTS, "initialConnectAttempts", true),
        };
    }

    interface External {
        AttributeDefinition ENABLE_AMQ1_PREFIX = create("enable-amq1-prefix", BOOLEAN)
                .setDefaultValue(ModelNode.TRUE)
                .setRequired(false)
                .setAllowExpression(true)
                .setRestartAllServices()
                .build();

        AttributeDefinition[] ATTRIBUTES = { ENABLE_AMQ1_PREFIX } ;

    }
    static class TransactionNameAllowedValuesValidator extends StringAllowedValuesValidator {
        public TransactionNameAllowedValuesValidator(String... values) {
            super(values);
        }

        @Override
        public void validateParameter(String parameterName, ModelNode value) throws OperationFailedException {
            if (value.isDefined()) {
                if (!getAllowedValues().contains(value)) {
                    MessagingLogger.ROOT_LOGGER.invalidTransactionNameValue(value.asString(), parameterName, getAllowedValues());
                }
            }
        }
    }
}

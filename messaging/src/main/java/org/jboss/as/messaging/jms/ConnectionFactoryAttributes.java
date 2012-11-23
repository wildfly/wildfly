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
package org.jboss.as.messaging.jms;

import static org.hornetq.api.core.client.HornetQClient.DEFAULT_CONNECTION_TTL;
import static org.hornetq.api.core.client.HornetQClient.DEFAULT_MAX_RETRY_INTERVAL;
import static org.hornetq.api.config.HornetQDefaultConfiguration.DEFAULT_SCHEDULED_THREAD_POOL_MAX_SIZE;
import static org.hornetq.api.config.HornetQDefaultConfiguration.DEFAULT_THREAD_POOL_MAX_SIZE;
import static org.jboss.as.controller.SimpleAttributeDefinitionBuilder.create;
import static org.jboss.as.controller.client.helpers.MeasurementUnit.BYTES;
import static org.jboss.as.controller.client.helpers.MeasurementUnit.MILLISECONDS;
import static org.jboss.as.controller.client.helpers.MeasurementUnit.PER_SECOND;
import static org.jboss.as.messaging.AttributeMarshallers.NOOP_MARSHALLER;
import static org.jboss.as.messaging.MessagingExtension.VERSION_1_1_0;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttribute.create;
import static org.jboss.dmr.ModelType.BIG_DECIMAL;
import static org.jboss.dmr.ModelType.BOOLEAN;
import static org.jboss.dmr.ModelType.INT;
import static org.jboss.dmr.ModelType.LONG;
import static org.jboss.dmr.ModelType.STRING;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.hornetq.api.core.client.HornetQClient;
import org.hornetq.ra.HornetQResourceAdapter;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.AttributeMarshaller;
import org.jboss.as.controller.ListAttributeDefinition;
import org.jboss.as.controller.PrimitiveListAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleMapAttributeDefinition;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.messaging.AttributeMarshallers;
import org.jboss.as.messaging.CommonAttributes;
import org.jboss.as.messaging.Element;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

public interface ConnectionFactoryAttributes {

    interface Common {
        AttributeDefinition AUTO_GROUP = SimpleAttributeDefinitionBuilder.create("auto-group", BOOLEAN)
                .setDefaultValue(new ModelNode().set(HornetQClient.DEFAULT_AUTO_GROUP))
                .setAllowNull(true)
                .build();

        AttributeDefinition BLOCK_ON_ACKNOWLEDGE = SimpleAttributeDefinitionBuilder.create("block-on-acknowledge", BOOLEAN)
                .setDefaultValue(new ModelNode().set(HornetQClient.DEFAULT_BLOCK_ON_ACKNOWLEDGE))
                .setAllowNull(true)
                .build();

        AttributeDefinition BLOCK_ON_DURABLE_SEND = SimpleAttributeDefinitionBuilder.create("block-on-durable-send", BOOLEAN)
                .setDefaultValue(new ModelNode().set(HornetQClient.DEFAULT_BLOCK_ON_DURABLE_SEND))
                .setAllowNull(true)
                .build();

        AttributeDefinition BLOCK_ON_NON_DURABLE_SEND = SimpleAttributeDefinitionBuilder.create("block-on-non-durable-send", BOOLEAN)
                .setDefaultValue(new ModelNode().set(HornetQClient.DEFAULT_BLOCK_ON_NON_DURABLE_SEND))
                .setAllowNull(true)
                .build();

        AttributeDefinition CACHE_LARGE_MESSAGE_CLIENT = SimpleAttributeDefinitionBuilder.create("cache-large-message-client", BOOLEAN)
                .setDefaultValue(new ModelNode().set(HornetQClient.DEFAULT_CACHE_LARGE_MESSAGE_CLIENT))
                .setAllowNull(true)
                .build();

        AttributeDefinition CLIENT_FAILURE_CHECK_PERIOD =SimpleAttributeDefinitionBuilder.create("client-failure-check-period", LONG)
                .setDefaultValue(new ModelNode().set(HornetQClient.DEFAULT_CLIENT_FAILURE_CHECK_PERIOD))
                .setMeasurementUnit(MILLISECONDS)
                .setAllowNull(true)
                .build();

        AttributeDefinition COMPRESS_LARGE_MESSAGES = SimpleAttributeDefinitionBuilder.create("compress-large-messages", BOOLEAN)
                .setDefaultValue(new ModelNode().set(HornetQClient.DEFAULT_COMPRESS_LARGE_MESSAGES))
                .setAllowNull(true)
                .build();

        AttributeDefinition CONFIRMATION_WINDOW_SIZE = SimpleAttributeDefinitionBuilder.create("confirmation-window-size", INT)
                .setDefaultValue(new ModelNode().set(HornetQClient.DEFAULT_CONFIRMATION_WINDOW_SIZE))
                .setMeasurementUnit(BYTES)
                .setAllowNull(true)
                .build();

        AttributeDefinition CONNECTION_LOAD_BALANCING_CLASS_NAME = SimpleAttributeDefinitionBuilder.create("connection-load-balancing-policy-class-name", STRING)
                .setDefaultValue(new ModelNode().set(HornetQClient.DEFAULT_CONNECTION_LOAD_BALANCING_POLICY_CLASS_NAME))
                .setAllowNull(true)
                .build();

        AttributeDefinition CONNECTION_TTL = new SimpleAttributeDefinitionBuilder("connection-ttl", LONG)
                .setDefaultValue(new ModelNode().set(DEFAULT_CONNECTION_TTL))
                .setAllowNull(true)
                .setMeasurementUnit(MILLISECONDS)
                .build();

        AttributeDefinition CONNECTOR = new SimpleMapAttributeDefinition.Builder(CommonAttributes.CONNECTOR, true)
                .setAlternatives(CommonAttributes.DISCOVERY_GROUP_NAME)
                .setAttributeMarshaller(AttributeMarshallers.CONNECTORS_MARSHALLER)
                .build();

        AttributeDefinition CONSUMER_WINDOW_SIZE = SimpleAttributeDefinitionBuilder.create("consumer-window-size", INT)
                .setDefaultValue(new ModelNode().set(HornetQClient.DEFAULT_CONSUMER_WINDOW_SIZE))
                .setMeasurementUnit(BYTES)
                .setAllowNull(true)
                .build();

        SimpleAttributeDefinition DISCOVERY_GROUP_NAME =  SimpleAttributeDefinitionBuilder.create(CommonAttributes.DISCOVERY_GROUP_NAME, STRING)
                .setAllowNull(true)
                .setAlternatives(CommonAttributes.CONNECTOR)
                .setAttributeMarshaller(AttributeMarshallers.DISCOVERY_GROUP_MARSHALLER)
                .setRestartAllServices()
                .build();

        AttributeDefinition DISCOVERY_INITIAL_WAIT_TIMEOUT = SimpleAttributeDefinitionBuilder.create("discovery-initial-wait-timeout", LONG)
                .setMeasurementUnit(MILLISECONDS)
                .setAllowNull(true)
                .setRestartAllServices()
                .setDeprecated(VERSION_1_1_0)
                .setAttributeMarshaller(NOOP_MARSHALLER)
                .build();

        AttributeDefinition DUPS_OK_BATCH_SIZE = SimpleAttributeDefinitionBuilder.create("dups-ok-batch-size", INT)
                .setDefaultValue(new ModelNode().set(HornetQClient.DEFAULT_ACK_BATCH_SIZE))
                .setAllowNull(true)
                .build();

        ListAttributeDefinition ENTRIES = PrimitiveListAttributeDefinition.Builder.of(CommonAttributes.ENTRIES, ModelType.STRING)
                .setAllowNull(false)
                .setRestartAllServices()
                .setValidator(new StringLengthValidator(1))
                .setAttributeMarshaller(new AttributeMarshallers.JndiEntriesAttributeMarshaller(false))
                .build();

        AttributeDefinition FAILOVER_ON_INITIAL_CONNECTION = SimpleAttributeDefinitionBuilder.create("failover-on-initial-connection", BOOLEAN)
                .setDefaultValue(new ModelNode().set(HornetQClient.DEFAULT_FAILOVER_ON_INITIAL_CONNECTION))
                .setAllowNull(true)
                .build();

        AttributeDefinition FAILOVER_ON_SERVER_SHUTDOWN = SimpleAttributeDefinitionBuilder.create("failover-on-server-shutdown", BOOLEAN)
                .setAllowNull(true)
                .setRestartAllServices()
                .setDeprecated(VERSION_1_1_0)
                .setAttributeMarshaller(NOOP_MARSHALLER)
                .build();

        AttributeDefinition GROUP_ID = SimpleAttributeDefinitionBuilder.create("group-id", STRING)
                .setAllowNull(true)
                .build();

        AttributeDefinition MAX_RETRY_INTERVAL = SimpleAttributeDefinitionBuilder.create("max-retry-interval", LONG)
                .setDefaultValue(new ModelNode().set(DEFAULT_MAX_RETRY_INTERVAL))
                .setMeasurementUnit(MILLISECONDS)
                .setAllowNull(true)
                .build();

        AttributeDefinition MIN_LARGE_MESSAGE_SIZE = SimpleAttributeDefinitionBuilder.create("min-large-message-size", INT)
                .setDefaultValue(new ModelNode().set(HornetQClient.DEFAULT_MIN_LARGE_MESSAGE_SIZE))
                .setMeasurementUnit(BYTES)
                .setAllowNull(true)
                .build();

        AttributeDefinition PRE_ACKNOWLEDGE = SimpleAttributeDefinitionBuilder.create("pre-acknowledge", BOOLEAN)
                .setDefaultValue(new ModelNode().set(HornetQClient.DEFAULT_PRE_ACKNOWLEDGE))
                .setAllowNull(true)
                .build();

        AttributeDefinition PRODUCER_MAX_RATE = SimpleAttributeDefinitionBuilder.create("producer-max-rate", INT)
                .setDefaultValue(new ModelNode().set(HornetQClient.DEFAULT_PRODUCER_MAX_RATE))
                .setMeasurementUnit(PER_SECOND)
                .setAllowNull(true)
                .build();

        AttributeDefinition PRODUCER_WINDOW_SIZE = SimpleAttributeDefinitionBuilder.create("producer-window-size", INT)
                .setDefaultValue(new ModelNode().set(HornetQClient.DEFAULT_PRODUCER_WINDOW_SIZE))
                .setMeasurementUnit(BYTES)
                .setAllowNull(true)
                .build();

        AttributeDefinition RETRY_INTERVAL = SimpleAttributeDefinitionBuilder.create("retry-interval", LONG)
                .setDefaultValue(new ModelNode().set(HornetQClient.DEFAULT_RETRY_INTERVAL))
                .setMeasurementUnit(MILLISECONDS)
                .setAllowNull(true)
                .build();

        AttributeDefinition RETRY_INTERVAL_MULTIPLIER = create("retry-interval-multiplier", BIG_DECIMAL)
                .setDefaultValue(new ModelNode().set(HornetQClient.DEFAULT_RETRY_INTERVAL_MULTIPLIER))
                .setAllowNull(true)
                .build();

        AttributeDefinition SCHEDULED_THREAD_POOL_MAX_SIZE = SimpleAttributeDefinitionBuilder.create("scheduled-thread-pool-max-size", INT)
                .setDefaultValue(new ModelNode().set(DEFAULT_SCHEDULED_THREAD_POOL_MAX_SIZE))
                .setAllowNull(true)
                .setAllowExpression(true)
                .build();

        AttributeDefinition THREAD_POOL_MAX_SIZE = SimpleAttributeDefinitionBuilder.create("thread-pool-max-size", INT)
                .setDefaultValue(new ModelNode().set(DEFAULT_THREAD_POOL_MAX_SIZE))
                .setAllowNull(true)
                .setAllowExpression(true)
                .build();

        AttributeDefinition TRANSACTION_BATCH_SIZE = SimpleAttributeDefinitionBuilder.create("transaction-batch-size", INT)
                .setDefaultValue(new ModelNode().set(HornetQClient.DEFAULT_ACK_BATCH_SIZE))
                .setAllowNull(true)
                .build();

        AttributeDefinition USE_GLOBAL_POOLS = SimpleAttributeDefinitionBuilder.create("use-global-pools", BOOLEAN)
                .setDefaultValue(new ModelNode().set(HornetQClient.DEFAULT_USE_GLOBAL_POOLS))
                .setAllowNull(true)
                .build();

        /**
         * Attributes are defined in the <em>same order than in the XSD schema</em>
         */
        ConnectionFactoryAttribute[] ATTRIBUTES = {
                create(DISCOVERY_GROUP_NAME, null, false),
                create(CONNECTOR, null, false),
                create(ENTRIES, null, false),
                create(CommonAttributes.HA, "HA", true),
                create(CLIENT_FAILURE_CHECK_PERIOD, "clientFailureCheckPeriod", true),
                create(CONNECTION_TTL, "connectionTTL", true),
                create(CommonAttributes.CALL_TIMEOUT, "callTimeout", true),
                create(CommonAttributes.CALL_FAILOVER_TIMEOUT, "callFailoverTimeout", true),
                create(CONSUMER_WINDOW_SIZE, "consumerWindowSize", true),
                create(CommonAttributes.CONSUMER_MAX_RATE, "consumerMaxRate", true),
                create(CONFIRMATION_WINDOW_SIZE, "confirmationWindowSize", true),
                create(PRODUCER_WINDOW_SIZE, "producerWindowSize", true),
                create(PRODUCER_MAX_RATE, "producerMaxRate", true),
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
                create(CommonAttributes.RECONNECT_ATTEMPTS, null, false),
                create(FAILOVER_ON_INITIAL_CONNECTION, "failoverOnInitialConnection", true),
                create(FAILOVER_ON_SERVER_SHUTDOWN, "failoverOnServerShutdown", false), // TODO HornetQResourceAdapter does not have this method
                create(CONNECTION_LOAD_BALANCING_CLASS_NAME, "connectionLoadBalancingPolicyClassName", true),
                create(USE_GLOBAL_POOLS, "useGlobalPools", true),
                create(SCHEDULED_THREAD_POOL_MAX_SIZE, "scheduledThreadPoolMaxSize", true),
                create(THREAD_POOL_MAX_SIZE, "threadPoolMaxSize", true),
                create(GROUP_ID, "groupID", true),

                create(DISCOVERY_INITIAL_WAIT_TIMEOUT, null, false), // Not used since messaging 1.2, we keep it for compatibility sake
        };

    }

    interface Regular {
        AttributeDefinition FACTORY_TYPE = create("factory-type", STRING)
                .setDefaultValue(new ModelNode().set(ConnectionFactoryType.GENERIC.toString()))
                .setValidator(ConnectionFactoryType.VALIDATOR)
                .setAllowNull(true)
                .setRestartAllServices()
                .build();

        AttributeDefinition[] ATTRIBUTES = { FACTORY_TYPE } ;

        AttributeDefinition INITIAL_MESSAGE_PACKET_SIZE = create("initial-message-packet-size", INT)
                .setStorageRuntime()
                .build();
    }

    interface Pooled {
        String USE_JNDI_PROP_NAME = "useJNDI";
        String SETUP_ATTEMPTS_PROP_NAME = "setupAttempts";
        String SETUP_INTERVAL_PROP_NAME = "setupInterval";
        String RECONNECT_ATTEMPTS_PROP_NAME = "reconnectAttempts";

        SimpleAttributeDefinition INITIAL_CONNECT_ATTEMPTS = SimpleAttributeDefinitionBuilder.create("initial-connect-attempts", INT)
                .setAllowNull(true)
                .setDefaultValue(new ModelNode(HornetQClient.INITIAL_CONNECT_ATTEMPTS))
                .setRestartAllServices()
                .build();

        SimpleAttributeDefinition INITIAL_MESSAGE_PACKET_SIZE = SimpleAttributeDefinitionBuilder.create("initial-message-packet-size", INT)
                .setAllowNull(true)
                .setDefaultValue(new ModelNode(HornetQClient.DEFAULT_INITIAL_MESSAGE_PACKET_SIZE))
                .setRestartAllServices()
                .build();

        SimpleAttributeDefinition JNDI_PARAMS = SimpleAttributeDefinitionBuilder.create("jndi-params", STRING)
                .setAllowNull(true)
                .setRestartAllServices()
                .build();

        SimpleAttributeDefinition MAX_POOL_SIZE = SimpleAttributeDefinitionBuilder.create("max-pool-size", INT)
                .setDefaultValue(new ModelNode().set(-1))
                .setAllowNull(true)
                .setAllowExpression(true)
                .setRestartAllServices()
                .build();

        SimpleAttributeDefinition MIN_POOL_SIZE = SimpleAttributeDefinitionBuilder.create("min-pool-size", INT)
                .setDefaultValue(new ModelNode().set(-1))
                .setAllowNull(true)
                .setAllowExpression(true)
                .setRestartAllServices()
                .build();

        SimpleAttributeDefinition PASSWORD = SimpleAttributeDefinitionBuilder.create("password", STRING)
                .setAllowNull(true)
                .setAllowExpression(true)
                .setRestartAllServices()
                .build();

        /**
         * By default, the resource adapter must reconnect infinitely (see {@link HornetQResourceAdapter#setParams})
         */
        AttributeDefinition RECONNECT_ATTEMPTS = create("reconnect-attempts", INT)
                .setDefaultValue(new ModelNode().set(-1))
                .setAllowNull(true)
                .build();

        SimpleAttributeDefinition SETUP_ATTEMPTS = SimpleAttributeDefinitionBuilder.create("setup-attempts", INT)
                .setAllowNull(true)
                .setRestartAllServices()
                .build();

        SimpleAttributeDefinition SETUP_INTERVAL =  SimpleAttributeDefinitionBuilder.create("setup-interval", LONG)
                .setMeasurementUnit(MILLISECONDS)
                .setAllowNull(true)
                .setRestartAllServices()
                .build();

        // FIXME use an enum for allowed values
        SimpleAttributeDefinition TRANSACTION = SimpleAttributeDefinitionBuilder.create("transaction", STRING)
                .setDefaultValue(new ModelNode().set("transaction"))
                .setAllowNull(true)
                .setAttributeMarshaller(new AttributeMarshaller() {
                    public void marshallAsElement(AttributeDefinition attribute, ModelNode resourceModel, boolean marshallDefault, XMLStreamWriter writer) throws XMLStreamException {
                        if (isMarshallable(attribute, resourceModel)) {
                            writer.writeStartElement(attribute.getXmlName());
                            writer.writeAttribute(Element.MODE.getLocalName(), resourceModel.get(attribute.getName()).asString());
                            writer.writeEndElement();
                        }
                    };
                })
                .setRestartAllServices()
                .build();

        AttributeDefinition USE_AUTO_RECOVERY = SimpleAttributeDefinitionBuilder.create("use-auto-recovery", BOOLEAN)
                .setDefaultValue(new ModelNode().set(true)) // HornetQResourceAdapter.useAutoRecovery = true but is not exposed publicly
                .setAllowNull(true)
                .setRestartAllServices()
                .build();

        AttributeDefinition USE_JNDI = SimpleAttributeDefinitionBuilder.create("use-jndi", BOOLEAN)
                .setAllowNull(true)
                .setRestartAllServices()
                .build();

        AttributeDefinition USE_LOCAL_TX = SimpleAttributeDefinitionBuilder.create("use-local-tx", BOOLEAN)
                .setAllowNull(true)
                .setRestartAllServices()
                .build();

        SimpleAttributeDefinition USER = SimpleAttributeDefinitionBuilder.create("user", STRING)
                .setAllowNull(true)
                .setAllowExpression(true)
                .build();

        /**
         * Attributes are defined in the <em>same order than in the XSD schema</em>
         */
        ConnectionFactoryAttribute[] ATTRIBUTES = {
                /* inbound config */
                create(USE_JNDI, USE_JNDI_PROP_NAME, true, true),
                create(JNDI_PARAMS, "jndiParams", true, true),
                create(USE_LOCAL_TX, "useLocalTx", true, true),
                create(SETUP_ATTEMPTS, SETUP_ATTEMPTS_PROP_NAME, true, true),
                create(SETUP_INTERVAL, SETUP_INTERVAL_PROP_NAME, true, true),

                create(TRANSACTION, null, false),
                create(USER, "userName", true),
                create(PASSWORD, "password", true),
                create(MIN_POOL_SIZE, null, false),
                create(MAX_POOL_SIZE, null, false),
                create(USE_AUTO_RECOVERY, "useAutoRecovery", true),
                create(INITIAL_MESSAGE_PACKET_SIZE, "initialMessagePacketSize", true),
                create(INITIAL_CONNECT_ATTEMPTS, "initialConnectAttempts", true),
        };
    }
}

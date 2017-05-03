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

import static org.jboss.as.controller.SimpleAttributeDefinitionBuilder.create;
import static org.jboss.as.controller.client.helpers.MeasurementUnit.BYTES;
import static org.jboss.as.controller.client.helpers.MeasurementUnit.MILLISECONDS;
import static org.jboss.as.controller.client.helpers.MeasurementUnit.PER_SECOND;
import static org.jboss.as.messaging.AttributeMarshallers.NOOP_MARSHALLER;
import static org.jboss.as.messaging.CommonAttributes.MESSAGING_SECURITY_DEF;
import static org.jboss.as.messaging.MessagingExtension.VERSION_1_1_0;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttribute.create;
import static org.jboss.dmr.ModelType.BOOLEAN;
import static org.jboss.dmr.ModelType.DOUBLE;
import static org.jboss.dmr.ModelType.INT;
import static org.jboss.dmr.ModelType.LONG;
import static org.jboss.dmr.ModelType.STRING;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.AttributeMarshaller;
import org.jboss.as.controller.ListAttributeDefinition;
import org.jboss.as.controller.ParameterCorrector;
import org.jboss.as.controller.PrimitiveListAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleMapAttributeDefinition;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.messaging.AttributeMarshallers;
import org.jboss.as.messaging.CommonAttributes;
import org.jboss.as.messaging.Element;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

public interface ConnectionFactoryAttributes {

    interface Common {
        AttributeDefinition AUTO_GROUP = SimpleAttributeDefinitionBuilder.create("auto-group", BOOLEAN)
                .setDefaultValue(new ModelNode().set(false))
                .setRequired(false)
                .setAllowExpression(true)
                .build();

        AttributeDefinition BLOCK_ON_ACKNOWLEDGE = SimpleAttributeDefinitionBuilder.create("block-on-acknowledge", BOOLEAN)
                .setDefaultValue(new ModelNode().set(false))
                .setRequired(false)
                .setAllowExpression(true)
                .build();

        AttributeDefinition BLOCK_ON_DURABLE_SEND = SimpleAttributeDefinitionBuilder.create("block-on-durable-send", BOOLEAN)
                .setDefaultValue(new ModelNode().set(true))
                .setRequired(false)
                .setAllowExpression(true)
                .build();

        AttributeDefinition BLOCK_ON_NON_DURABLE_SEND = SimpleAttributeDefinitionBuilder.create("block-on-non-durable-send", BOOLEAN)
                .setDefaultValue(new ModelNode().set(false))
                .setRequired(false)
                .setAllowExpression(true)
                .build();

        AttributeDefinition CACHE_LARGE_MESSAGE_CLIENT = SimpleAttributeDefinitionBuilder.create("cache-large-message-client", BOOLEAN)
                .setDefaultValue(new ModelNode().set(false))
                .setRequired(false)
                .setAllowExpression(true)
                .build();

        AttributeDefinition CLIENT_FAILURE_CHECK_PERIOD =SimpleAttributeDefinitionBuilder.create("client-failure-check-period", LONG)
                .setDefaultValue(new ModelNode().set(30000L))
                .setMeasurementUnit(MILLISECONDS)
                .setRequired(false)
                .setAllowExpression(true)
                .build();

        AttributeDefinition COMPRESS_LARGE_MESSAGES = SimpleAttributeDefinitionBuilder.create("compress-large-messages", BOOLEAN)
                .setDefaultValue(new ModelNode().set(false))
                .setRequired(false)
                .setAllowExpression(true)
                .build();

        AttributeDefinition CONFIRMATION_WINDOW_SIZE = SimpleAttributeDefinitionBuilder.create("confirmation-window-size", INT)
                .setDefaultValue(new ModelNode().set(-1))
                .setMeasurementUnit(BYTES)
                .setRequired(false)
                .setAllowExpression(true)
                .build();

        AttributeDefinition CONNECTION_LOAD_BALANCING_CLASS_NAME = SimpleAttributeDefinitionBuilder.create("connection-load-balancing-policy-class-name", STRING)
                .setDefaultValue(new ModelNode().set("org.hornetq.api.core.client.loadbalance.RoundRobinConnectionLoadBalancingPolicy"))
                .setRequired(false)
                .setAllowExpression(false)
                .build();

        AttributeDefinition CONNECTION_TTL = new SimpleAttributeDefinitionBuilder("connection-ttl", LONG)
                .setDefaultValue(new ModelNode().set(60000L))
                .setRequired(false)
                .setAllowExpression(true)
                .setMeasurementUnit(MILLISECONDS)
                .build();

        AttributeDefinition CONNECTOR = new SimpleMapAttributeDefinition.Builder(CommonAttributes.CONNECTOR, true)
                .setAlternatives(CommonAttributes.DISCOVERY_GROUP_NAME)
                .setAttributeMarshaller(AttributeMarshallers.CONNECTORS_MARSHALLER)
                .setCorrector(new ParameterCorrector() {
                    /*
                     * https://issues.jboss.org/browse/WFLY-1796
                     *
                     * For backwards compatibility, the connector attribute must be a map where the key is a
                     * connector name and the value is not taken into account (in previous HornetQ versions, the value
                     * was the backup's server connector).
                     *
                     * This is a source of confusion when creating resources with connector: users expect to pass a
                     * list of connectors and this fails as they must pass a map with undefined values.
                     *
                     * This corrector will replace a list with the map expected to populate the model.
                     */
                    @Override
                    public ModelNode correct(ModelNode newValue, ModelNode currentValue) {
                        if (newValue.getType() != ModelType.LIST) {
                            return newValue;
                        } else {
                            ModelNode correctValue = new ModelNode();
                            for (ModelNode node : newValue.asList()) {
                                correctValue.get(node.asString());
                            }
                            return correctValue;
                        }
                    }
                })
                .setRestartAllServices()
                .build();

        AttributeDefinition CONSUMER_MAX_RATE = SimpleAttributeDefinitionBuilder.create("consumer-max-rate", INT)
                .setDefaultValue(new ModelNode(-1))
                .setMeasurementUnit(PER_SECOND)
                .setRequired(false)
                .setAllowExpression(true)
                .build();

        AttributeDefinition CONSUMER_WINDOW_SIZE = SimpleAttributeDefinitionBuilder.create("consumer-window-size", INT)
                .setDefaultValue(new ModelNode().set(1048576))
                .setMeasurementUnit(BYTES)
                .setRequired(false)
                .setAllowExpression(true)
                .build();

        SimpleAttributeDefinition DISCOVERY_GROUP_NAME =  SimpleAttributeDefinitionBuilder.create(CommonAttributes.DISCOVERY_GROUP_NAME, STRING)
                .setRequired(false)
                .setAlternatives(CommonAttributes.CONNECTOR)
                .setAttributeMarshaller(AttributeMarshallers.DISCOVERY_GROUP_MARSHALLER)
                .setRestartAllServices()
                .build();

        AttributeDefinition DISCOVERY_INITIAL_WAIT_TIMEOUT = SimpleAttributeDefinitionBuilder.create("discovery-initial-wait-timeout", LONG)
                .setMeasurementUnit(MILLISECONDS)
                .setRequired(false)
                .setRestartAllServices()
                .setDeprecated(VERSION_1_1_0)
                .setAttributeMarshaller(NOOP_MARSHALLER)
                .build();

        AttributeDefinition DUPS_OK_BATCH_SIZE = SimpleAttributeDefinitionBuilder.create("dups-ok-batch-size", INT)
                .setDefaultValue(new ModelNode().set(1048576))
                .setRequired(false)
                .setAllowExpression(true)
                .build();

        ListAttributeDefinition ENTRIES = PrimitiveListAttributeDefinition.Builder.of(CommonAttributes.ENTRIES, ModelType.STRING)
                .setRequired(true)
                .setAllowExpression(true)
                .setRestartAllServices()
                .setListValidator(Validators.noDuplicateElements(new StringLengthValidator(1, false, true)))
                .setAttributeMarshaller(new AttributeMarshallers.JndiEntriesAttributeMarshaller(false))
                .build();

        AttributeDefinition FAILOVER_ON_INITIAL_CONNECTION = SimpleAttributeDefinitionBuilder.create("failover-on-initial-connection", BOOLEAN)
                .setDefaultValue(new ModelNode().set(false))
                .setRequired(false)
                .setAllowExpression(true)
                .build();

        AttributeDefinition FAILOVER_ON_SERVER_SHUTDOWN = SimpleAttributeDefinitionBuilder.create("failover-on-server-shutdown", BOOLEAN)
                .setRequired(false)
                .setRestartAllServices()
                .setDeprecated(VERSION_1_1_0)
                .build();

        AttributeDefinition GROUP_ID = SimpleAttributeDefinitionBuilder.create("group-id", STRING)
                .setRequired(false)
                .setAllowExpression(true)
                .build();

        AttributeDefinition MAX_RETRY_INTERVAL = SimpleAttributeDefinitionBuilder.create("max-retry-interval", LONG)
                .setDefaultValue(new ModelNode().set(2000L))
                .setMeasurementUnit(MILLISECONDS)
                .setRequired(false)
                .setAllowExpression(true)
                .build();

        AttributeDefinition MIN_LARGE_MESSAGE_SIZE = SimpleAttributeDefinitionBuilder.create("min-large-message-size", INT)
                .setDefaultValue(new ModelNode().set(102400))
                .setMeasurementUnit(BYTES)
                .setRequired(false)
                .setAllowExpression(true)
                .build();

        AttributeDefinition PRE_ACKNOWLEDGE = SimpleAttributeDefinitionBuilder.create("pre-acknowledge", BOOLEAN)
                .setDefaultValue(new ModelNode().set(false))
                .setRequired(false)
                .setAllowExpression(true)
                .build();

        AttributeDefinition PRODUCER_MAX_RATE = SimpleAttributeDefinitionBuilder.create("producer-max-rate", INT)
                .setDefaultValue(new ModelNode().set(-1))
                .setMeasurementUnit(PER_SECOND)
                .setRequired(false)
                .setAllowExpression(true)
                .build();

        AttributeDefinition PRODUCER_WINDOW_SIZE = SimpleAttributeDefinitionBuilder.create("producer-window-size", INT)
                .setDefaultValue(new ModelNode().set(65536))
                .setMeasurementUnit(BYTES)
                .setRequired(false)
                .setAllowExpression(true)
                .build();


        AttributeDefinition RECONNECT_ATTEMPTS = create("reconnect-attempts", INT)
                .setDefaultValue(new ModelNode().set(0))
                .setRequired(false)
                .setAllowExpression(true)
                .build();

        AttributeDefinition RETRY_INTERVAL = SimpleAttributeDefinitionBuilder.create("retry-interval", LONG)
                .setDefaultValue(new ModelNode().set(2000L))
                .setMeasurementUnit(MILLISECONDS)
                .setRequired(false)
                .setAllowExpression(true)
                .build();

        AttributeDefinition RETRY_INTERVAL_MULTIPLIER = create("retry-interval-multiplier", DOUBLE)
                .setDefaultValue(new ModelNode().set(1.0))
                .setRequired(false)
                .setAllowExpression(true)
                .build();

        AttributeDefinition SCHEDULED_THREAD_POOL_MAX_SIZE = SimpleAttributeDefinitionBuilder.create("scheduled-thread-pool-max-size", INT)
                .setDefaultValue(new ModelNode().set(5))
                .setRequired(false)
                .setAllowExpression(true)
                .build();

        AttributeDefinition THREAD_POOL_MAX_SIZE = SimpleAttributeDefinitionBuilder.create("thread-pool-max-size", INT)
                .setDefaultValue(new ModelNode().set(30))
                .setRequired(false)
                .setAllowExpression(true)
                .build();

        AttributeDefinition TRANSACTION_BATCH_SIZE = SimpleAttributeDefinitionBuilder.create("transaction-batch-size", INT)
                .setDefaultValue(new ModelNode().set(1048576))
                .setRequired(false)
                .setAllowExpression(true)
                .build();

        AttributeDefinition USE_GLOBAL_POOLS = SimpleAttributeDefinitionBuilder.create("use-global-pools", BOOLEAN)
                .setDefaultValue(new ModelNode().set(true))
                .setRequired(false)
                .setAllowExpression(true)
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
                create(CONSUMER_MAX_RATE, "consumerMaxRate", true),
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
                create(RECONNECT_ATTEMPTS, null, false),
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
                .setRequired(false)
                .setAllowExpression(true)
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
                .setRequired(false)
                .setAllowExpression(true)
                .setDefaultValue(new ModelNode(1))
                .setRestartAllServices()
                .build();

        SimpleAttributeDefinition INITIAL_MESSAGE_PACKET_SIZE = SimpleAttributeDefinitionBuilder.create("initial-message-packet-size", INT)
                .setRequired(false)
                .setAllowExpression(true)
                .setDefaultValue(new ModelNode(1500))
                .setRestartAllServices()
                .build();

        SimpleAttributeDefinition JNDI_PARAMS = SimpleAttributeDefinitionBuilder.create("jndi-params", STRING)
                .setRequired(false)
                .setAllowExpression(true)
                .setRestartAllServices()
                .build();

        SimpleAttributeDefinition MAX_POOL_SIZE = SimpleAttributeDefinitionBuilder.create("max-pool-size", INT)
                .setDefaultValue(new ModelNode().set(-1))
                .setRequired(false)
                .setAllowExpression(true)
                .setRestartAllServices()
                .build();

        SimpleAttributeDefinition MIN_POOL_SIZE = SimpleAttributeDefinitionBuilder.create("min-pool-size", INT)
                .setDefaultValue(new ModelNode().set(-1))
                .setRequired(false)
                .setAllowExpression(true)
                .setRestartAllServices()
                .build();

        SimpleAttributeDefinition PASSWORD = SimpleAttributeDefinitionBuilder.create("password", STRING)
                .setRequired(false)
                .setAllowExpression(true)
                .setRestartAllServices()
                .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.CREDENTIAL)
                .addAccessConstraint(MESSAGING_SECURITY_DEF)
                .build();

        /**
         * By default, the resource adapter must reconnect infinitely.
         */
        AttributeDefinition RECONNECT_ATTEMPTS = create("reconnect-attempts", INT)
                .setDefaultValue(new ModelNode().set(-1))
                .setRequired(false)
                .setAllowExpression(true)
                .build();

        SimpleAttributeDefinition SETUP_ATTEMPTS = SimpleAttributeDefinitionBuilder.create("setup-attempts", INT)
                .setRequired(false)
                .setAllowExpression(true)
                .setRestartAllServices()
                .build();

        SimpleAttributeDefinition SETUP_INTERVAL =  SimpleAttributeDefinitionBuilder.create("setup-interval", LONG)
                .setMeasurementUnit(MILLISECONDS)
                .setRequired(false)
                .setAllowExpression(true)
                .setRestartAllServices()
                .build();

        // FIXME use an enum for allowed values
        SimpleAttributeDefinition TRANSACTION = SimpleAttributeDefinitionBuilder.create("transaction", STRING)
                .setDefaultValue(new ModelNode().set("transaction"))
                .setRequired(false)
                .setAllowExpression(true)
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
                .setRequired(false)
                .setAllowExpression(true)
                .setRestartAllServices()
                .build();

        AttributeDefinition USE_JNDI = SimpleAttributeDefinitionBuilder.create("use-jndi", BOOLEAN)
                .setRequired(false)
                .setAllowExpression(true)
                .setRestartAllServices()
                .build();

        AttributeDefinition USE_LOCAL_TX = SimpleAttributeDefinitionBuilder.create("use-local-tx", BOOLEAN)
                .setRequired(false)
                .setAllowExpression(true)
                .setRestartAllServices()
                .build();

        SimpleAttributeDefinition USER = SimpleAttributeDefinitionBuilder.create("user", STRING)
                .setRequired(false)
                .setAllowExpression(true)
                .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.CREDENTIAL)
                .addAccessConstraint(MESSAGING_SECURITY_DEF)
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

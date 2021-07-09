/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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
package org.wildfly.extension.messaging.activemq.jms.legacy;

import static org.jboss.as.controller.SimpleAttributeDefinitionBuilder.create;
import static org.jboss.as.controller.client.helpers.MeasurementUnit.BYTES;
import static org.jboss.as.controller.client.helpers.MeasurementUnit.MILLISECONDS;
import static org.jboss.as.controller.client.helpers.MeasurementUnit.PER_SECOND;
import static org.jboss.dmr.ModelType.BOOLEAN;
import static org.jboss.dmr.ModelType.DOUBLE;
import static org.jboss.dmr.ModelType.INT;
import static org.jboss.dmr.ModelType.LONG;
import static org.jboss.dmr.ModelType.STRING;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import org.apache.activemq.artemis.api.config.ActiveMQDefaultConfiguration;
import org.apache.activemq.artemis.api.core.client.ActiveMQClient;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.AttributeMarshaller;
import org.jboss.as.controller.AttributeParser;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.RuntimePackageDependency;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.extension.messaging.activemq.CommonAttributes;
import org.wildfly.extension.messaging.activemq.InfiniteOrPositiveValidators;
import org.wildfly.extension.messaging.activemq.MessagingExtension;
import org.wildfly.extension.messaging.activemq.jms.Validators;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2015 Red Hat inc.
 */
public class LegacyConnectionFactoryDefinition extends PersistentResourceDefinition {

    /**
     * @see HornetQClient.DEFAULT_AUTO_GROUP
     */
    public static final AttributeDefinition AUTO_GROUP = SimpleAttributeDefinitionBuilder.create("auto-group", BOOLEAN)
            .setDefaultValue(ModelNode.FALSE)
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    /**
     * @see HornetQClient.DEFAULT_BLOCK_ON_ACKNOWLEDGE
     */
    public static final AttributeDefinition BLOCK_ON_ACKNOWLEDGE = SimpleAttributeDefinitionBuilder.create("block-on-acknowledge", BOOLEAN)
            .setDefaultValue(ModelNode.FALSE)
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    /**
     * @see HornetQClient.DEFAULT_BLOCK_ON_DURABLE_SEND
     */
    public static final AttributeDefinition BLOCK_ON_DURABLE_SEND = SimpleAttributeDefinitionBuilder.create("block-on-durable-send", BOOLEAN)
            .setDefaultValue(ModelNode.TRUE)
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    /**
     * @see HornetQClient.DEFAULT_BLOCK_ON_NON_DURABLE_SEND
     */
    public static final AttributeDefinition BLOCK_ON_NON_DURABLE_SEND = SimpleAttributeDefinitionBuilder.create("block-on-non-durable-send", BOOLEAN)
            .setDefaultValue(ModelNode.FALSE)
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    /**
     * @see HornetQClient.DEFAULT_CACHE_LARGE_MESSAGE_CLIENT
     */
    public static final AttributeDefinition CACHE_LARGE_MESSAGE_CLIENT = SimpleAttributeDefinitionBuilder.create("cache-large-message-client", BOOLEAN)
            .setDefaultValue(ModelNode.FALSE)
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    /**
     * @see HornetQClient.DEFAULT_CLIENT_FAILURE_CHECK_PERIOD
     */
    public static final AttributeDefinition CLIENT_FAILURE_CHECK_PERIOD = SimpleAttributeDefinitionBuilder.create("client-failure-check-period", LONG)
            .setDefaultValue(new ModelNode(30000L))
            .setMeasurementUnit(MILLISECONDS)
            .setRequired(false)
            .setAllowExpression(true)
            .setValidator(InfiniteOrPositiveValidators.LONG_INSTANCE)
            .setRestartAllServices()
            .build();

    /**
     * @see HornetQClient.DEFAULT_COMPRESS_LARGE_MESSAGES
     */
    public static final AttributeDefinition COMPRESS_LARGE_MESSAGES = SimpleAttributeDefinitionBuilder.create("compress-large-messages", BOOLEAN)
            .setDefaultValue(ModelNode.FALSE)
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    /**
     * @see HornetQClient.DEFAULT_CONFIRMATION_WINDOW_SIZE
     */
    public static final AttributeDefinition CONFIRMATION_WINDOW_SIZE = SimpleAttributeDefinitionBuilder.create("confirmation-window-size", INT)
            .setDefaultValue(new ModelNode(-1))
            .setMeasurementUnit(BYTES)
            .setRequired(false)
            .setAllowExpression(true)
            .setCorrector(InfiniteOrPositiveValidators.NEGATIVE_VALUE_CORRECTOR)
            .setValidator(InfiniteOrPositiveValidators.INT_INSTANCE)
            .setRestartAllServices()
            .build();

    /**
     * @see HornetQClient.DEFAULT_CONNECTION_LOAD_BALANCING_POLICY_CLASS_NAME
     */
    public static final AttributeDefinition CONNECTION_LOAD_BALANCING_CLASS_NAME = SimpleAttributeDefinitionBuilder.create("connection-load-balancing-policy-class-name", STRING)
            .setDefaultValue(new ModelNode("org.hornetq.api.core.client.loadbalance.RoundRobinConnectionLoadBalancingPolicy"))
            .setRequired(false)
            .setAllowExpression(false)
            .setRestartAllServices()
            .build();

    /**
     * @see HornetQClient.DEFAULT_CONNECTION_TTL
     */
    public static final AttributeDefinition CONNECTION_TTL = new SimpleAttributeDefinitionBuilder("connection-ttl", LONG)
            .setDefaultValue(new ModelNode(60000L))
            .setRequired(false)
            .setAllowExpression(true)
            .setValidator(InfiniteOrPositiveValidators.LONG_INSTANCE)
            .setMeasurementUnit(MILLISECONDS)
            .setRestartAllServices()
            .build();

    /**
     * @see HornetQClient.DEFAULT_CONSUMER_MAX_RATE
     */
    public static final AttributeDefinition CONSUMER_MAX_RATE = SimpleAttributeDefinitionBuilder.create("consumer-max-rate", INT)
            .setDefaultValue(new ModelNode(-1))
            .setMeasurementUnit(PER_SECOND)
            .setRequired(false)
            .setAllowExpression(true)
            .setCorrector(InfiniteOrPositiveValidators.NEGATIVE_VALUE_CORRECTOR)
            .setValidator(InfiniteOrPositiveValidators.INT_INSTANCE)
            .setRestartAllServices()
            .build();

    /**
     * @see HornetQClient.DEFAULT_CONSUMER_WINDOW_SIZE
     */
    public static final AttributeDefinition CONSUMER_WINDOW_SIZE = SimpleAttributeDefinitionBuilder.create("consumer-window-size", INT)
            .setDefaultValue(new ModelNode(1024 * 1024))
            .setMeasurementUnit(BYTES)
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    /**
     * @see HornetQClient.DEFAULT_ACK_BATCH_SIZE
     */
    public static final AttributeDefinition DUPS_OK_BATCH_SIZE = SimpleAttributeDefinitionBuilder.create("dups-ok-batch-size", INT)
            .setDefaultValue(new ModelNode(1024 * 1024))
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static final StringListAttributeDefinition ENTRIES = new StringListAttributeDefinition.Builder(CommonAttributes.ENTRIES)
            .setRequired(true)
            .setAllowExpression(true)
            .setListValidator(Validators.noDuplicateElements(new StringLengthValidator(1, false, true)))
            .setAttributeParser(AttributeParser.STRING_LIST)
            .setAttributeMarshaller(AttributeMarshaller.STRING_LIST)
            .setRestartAllServices()
            .build();

    public static final AttributeDefinition FACTORY_TYPE = create("factory-type", STRING)
            .setDefaultValue(new ModelNode().set(HornetQConnectionFactoryType.GENERIC.toString()))
            .setValidator(HornetQConnectionFactoryType.VALIDATOR)
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    /**
     * @see HornetQClient.DEFAULT_FAILOVER_ON_INITIAL_CONNECTION
     */
    public static final AttributeDefinition FAILOVER_ON_INITIAL_CONNECTION = SimpleAttributeDefinitionBuilder.create("failover-on-initial-connection", BOOLEAN)
            .setDefaultValue(ModelNode.FALSE)
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static final AttributeDefinition GROUP_ID = SimpleAttributeDefinitionBuilder.create("group-id", STRING)
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition HA = create("ha", BOOLEAN)
            .setDefaultValue(ModelNode.FALSE)
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    /**
     * @see HornetQClient.INITIAL_CONNECT_ATTEMPTS
     */
    public static final AttributeDefinition INITIAL_CONNECT_ATTEMPTS = SimpleAttributeDefinitionBuilder.create("initial-connect-attempts", INT)
            .setDefaultValue(new ModelNode(1))
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    /**
     * @see HornetQClient.DEFAULT_INITIAL_MESSAGE_PACKET_SIZE
     */
    public static final AttributeDefinition INITIAL_MESSAGE_PACKET_SIZE = SimpleAttributeDefinitionBuilder.create("initial-message-packet-size", INT)
            .setDefaultValue(new ModelNode(1500))
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    /**
     * @see HornetQClient.DEFAULT_MAX_RETRY_INTERVAL
     */
    public static final AttributeDefinition MAX_RETRY_INTERVAL = SimpleAttributeDefinitionBuilder.create("max-retry-interval", LONG)
            .setDefaultValue(new ModelNode(2000L))
            .setMeasurementUnit(MILLISECONDS)
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    /**
     * @see HornetQClient.DEFAULT_MIN_LARGE_MESSAGE_SIZE
     */
    public static final AttributeDefinition MIN_LARGE_MESSAGE_SIZE = SimpleAttributeDefinitionBuilder.create("min-large-message-size", INT)
            .setDefaultValue(new ModelNode(100 * 1024))
            .setMeasurementUnit(BYTES)
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    /**
     * @see HornetQClient.DEFAULT_PRE_ACKNOWLEDGE
     */
    public static final AttributeDefinition PRE_ACKNOWLEDGE = SimpleAttributeDefinitionBuilder.create("pre-acknowledge", BOOLEAN)
            .setDefaultValue(ModelNode.FALSE)
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    /**
     * @see HornetQClient.DEFAULT_PRODUCER_MAX_RATE
     */
    public static final AttributeDefinition PRODUCER_MAX_RATE = SimpleAttributeDefinitionBuilder.create("producer-max-rate", INT)
            .setDefaultValue(new ModelNode(-1))
            .setMeasurementUnit(PER_SECOND)
            .setRequired(false)
            .setAllowExpression(true)
            .setCorrector(InfiniteOrPositiveValidators.NEGATIVE_VALUE_CORRECTOR)
            .setValidator(InfiniteOrPositiveValidators.INT_INSTANCE)
            .setRestartAllServices()
            .build();

    /**
     * @see HornetQClient.DEFAULT_PRODUCER_WINDOW_SIZE
     */
    public static final AttributeDefinition PRODUCER_WINDOW_SIZE = SimpleAttributeDefinitionBuilder.create("producer-window-size", INT)
            .setDefaultValue(new ModelNode(64 * 1024))
            .setMeasurementUnit(BYTES)
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    /**
     * @see HornetQClient.DEFAULT_RECONNECT_ATTEMPTS
     */
    public static final AttributeDefinition RECONNECT_ATTEMPTS = create("reconnect-attempts", INT)
            .setDefaultValue(ModelNode.ZERO)
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    /**
     * @see HornetQClient.DEFAULT_RETRY_INTERVAL
     */
    public static final AttributeDefinition RETRY_INTERVAL = SimpleAttributeDefinitionBuilder.create("retry-interval", LONG)
            .setDefaultValue(new ModelNode(2000L))
            .setMeasurementUnit(MILLISECONDS)
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    /**
     * @see HornetQClient.DEFAULT_RETRY_INTERVAL_MULTIPLIER
     */
    public static final AttributeDefinition RETRY_INTERVAL_MULTIPLIER = create("retry-interval-multiplier", DOUBLE)
            .setDefaultValue(new ModelNode(1.0d))
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    /**
     * @see ActiveMQDefaultConfiguration#getDefaultScheduledThreadPoolMaxSize
     */
    public static final AttributeDefinition SCHEDULED_THREAD_POOL_MAX_SIZE = SimpleAttributeDefinitionBuilder.create("scheduled-thread-pool-max-size", INT)
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
    public static final AttributeDefinition THREAD_POOL_MAX_SIZE = SimpleAttributeDefinitionBuilder.create("thread-pool-max-size", INT)
            .setDefaultValue(new ModelNode(30))
            .setRequired(false)
            .setAllowExpression(true)
            .setCorrector(InfiniteOrPositiveValidators.NEGATIVE_VALUE_CORRECTOR)
            .setValidator(InfiniteOrPositiveValidators.INT_INSTANCE)
            .setRestartAllServices()
            .build();

    /**
     * @see HornetQClient#DEFAULT_ACK_BATCH_SIZE
     */
    public static final AttributeDefinition TRANSACTION_BATCH_SIZE = SimpleAttributeDefinitionBuilder.create("transaction-batch-size", INT)
            .setDefaultValue(new ModelNode().set(1024 * 1024))
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    /**
     * @see HornetQClient#DEFAULT_USE_GLOBAL_POOLS
     */
    public static final AttributeDefinition USE_GLOBAL_POOLS = SimpleAttributeDefinitionBuilder.create("use-global-pools", BOOLEAN)
            .setDefaultValue(ModelNode.TRUE)
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static final AttributeDefinition DISCOVERY_GROUP = SimpleAttributeDefinitionBuilder.create(CommonAttributes.DISCOVERY_GROUP, STRING)
            .setAlternatives(CommonAttributes.CONNECTORS)
            .setRequired(false)
            .setRestartAllServices()
            .build();

    public static final StringListAttributeDefinition CONNECTORS = new StringListAttributeDefinition.Builder(CommonAttributes.CONNECTORS)
            .setAlternatives(CommonAttributes.DISCOVERY_GROUP)
            .setRequired(false)
            .setAttributeParser(AttributeParser.STRING_LIST)
            .setAttributeMarshaller(AttributeMarshaller.STRING_LIST)
            .setRestartAllServices()
            .build();

    /**
     * @see ActiveMQClient.DEFAULT_CALL_TIMEOUT
     */
    public static final AttributeDefinition CALL_TIMEOUT = create("call-timeout", LONG)
            .setDefaultValue(new ModelNode(30000L))
            .setMeasurementUnit(MILLISECONDS)
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition CALL_FAILOVER_TIMEOUT = create("call-failover-timeout", LONG)
            // ActiveMQClient.DEFAULT_CALL_FAILOVER_TIMEOUT was changed from -1 to 30000 in ARTEMIS-255
            .setDefaultValue(new ModelNode(-1L))
            .setRequired(false)
            .setAllowExpression(true)
            .setMeasurementUnit(MILLISECONDS)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition CLIENT_ID = create("client-id", ModelType.STRING)
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    static final AttributeDefinition[] ATTRIBUTES = {
        ENTRIES,
        AUTO_GROUP,
        BLOCK_ON_ACKNOWLEDGE,
        BLOCK_ON_DURABLE_SEND,
        BLOCK_ON_NON_DURABLE_SEND,
        CACHE_LARGE_MESSAGE_CLIENT,
        CALL_FAILOVER_TIMEOUT,
        CALL_TIMEOUT,
        CLIENT_FAILURE_CHECK_PERIOD,
        CLIENT_ID,
        COMPRESS_LARGE_MESSAGES,
        CONFIRMATION_WINDOW_SIZE,
        CONNECTION_LOAD_BALANCING_CLASS_NAME,
        CONNECTION_TTL,
        CONSUMER_MAX_RATE,
        CONSUMER_WINDOW_SIZE,
        DUPS_OK_BATCH_SIZE,
        FACTORY_TYPE,
        FAILOVER_ON_INITIAL_CONNECTION,
        GROUP_ID,
        HA,
        INITIAL_CONNECT_ATTEMPTS,
        INITIAL_MESSAGE_PACKET_SIZE,
        MAX_RETRY_INTERVAL,
        MIN_LARGE_MESSAGE_SIZE,
        PRE_ACKNOWLEDGE,
        PRODUCER_MAX_RATE,
        PRODUCER_WINDOW_SIZE,
        RECONNECT_ATTEMPTS,
        RETRY_INTERVAL,
        RETRY_INTERVAL_MULTIPLIER,
        SCHEDULED_THREAD_POOL_MAX_SIZE,
        THREAD_POOL_MAX_SIZE,
        TRANSACTION_BATCH_SIZE,
        USE_GLOBAL_POOLS,
        DISCOVERY_GROUP,
        CONNECTORS
    };

    public static final LegacyConnectionFactoryDefinition INSTANCE = new LegacyConnectionFactoryDefinition();

    protected LegacyConnectionFactoryDefinition() {
        super(new SimpleResourceDefinition.Parameters(
                MessagingExtension.LEGACY_CONNECTION_FACTORY_PATH,
                MessagingExtension.getResourceDescriptionResolver(CommonAttributes.CONNECTION_FACTORY))
                .setAddHandler(LegacyConnectionFactoryAdd.INSTANCE)
                .setRemoveHandler(LegacyConnectionFactoryRemove.INSTANCE)
                .setAdditionalPackages(RuntimePackageDependency.required("org.hornetq.client")));
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Arrays.asList(ATTRIBUTES);
    }

    public enum HornetQConnectionFactoryType {
        GENERIC,
        TOPIC,
        QUEUE,
        XA_GENERIC,
        XA_QUEUE,
        XA_TOPIC;

        static final ParameterValidator VALIDATOR = new EnumValidator<>(HornetQConnectionFactoryType.class, EnumSet.allOf(HornetQConnectionFactoryType.class));
    }
}

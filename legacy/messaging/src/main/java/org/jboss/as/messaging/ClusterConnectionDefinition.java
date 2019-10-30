/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

import static org.jboss.as.controller.SimpleAttributeDefinitionBuilder.create;
import static org.jboss.as.controller.client.helpers.MeasurementUnit.MILLISECONDS;
import static org.jboss.as.messaging.CommonAttributes.CONNECTOR_REF_STRING;
import static org.jboss.as.messaging.CommonAttributes.STATIC_CONNECTORS;
import static org.jboss.dmr.ModelType.BOOLEAN;
import static org.jboss.dmr.ModelType.DOUBLE;
import static org.jboss.dmr.ModelType.INT;
import static org.jboss.dmr.ModelType.LONG;
import static org.jboss.dmr.ModelType.STRING;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelOnlyAddStepHandler;
import org.jboss.as.controller.ModelOnlyResourceDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PrimitiveListAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;


/**
 * Cluster connection resource definition
 *
 * @author <a href="http://jmesnil.net">Jeff Mesnil</a> (c) 2012 Red Hat Inc.
 */
public class ClusterConnectionDefinition extends ModelOnlyResourceDefinition {

    public static final PathElement PATH = PathElement.pathElement(CommonAttributes.CLUSTER_CONNECTION);

    public static final SimpleAttributeDefinition ADDRESS = create("cluster-connection-address", STRING)
            .setXmlName(CommonAttributes.ADDRESS)
            .setDefaultValue(null)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition ALLOW_DIRECT_CONNECTIONS_ONLY = create("allow-direct-connections-only", BOOLEAN)
            .setDefaultValue(new ModelNode(false))
            .setRequired(false)
            .setAllowExpression(true)
            .setAlternatives(CommonAttributes.DISCOVERY_GROUP_NAME)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition CHECK_PERIOD = create("check-period", LONG)
            .setDefaultValue(new ModelNode(30000L))
            .setRequired(false)
            .setAllowExpression(true)
            .setMeasurementUnit(MILLISECONDS)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition CONNECTION_TTL = create("connection-ttl", LONG)
            .setDefaultValue(new ModelNode(60000L))
            .setMeasurementUnit(MILLISECONDS)
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition CONNECTOR_REF = create("connector-ref", STRING)
            .setRestartAllServices()
            .build();

    public static final PrimitiveListAttributeDefinition CONNECTOR_REFS = PrimitiveListAttributeDefinition.Builder.of(STATIC_CONNECTORS, STRING)
            .setRequired(false)
            .setElementValidator(new StringLengthValidator(1))
            .setXmlName(CONNECTOR_REF_STRING)
            .setAttributeMarshaller(new AttributeMarshallers.WrappedListAttributeMarshaller(null))
            .setAlternatives(CommonAttributes.DISCOVERY_GROUP_NAME)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition DISCOVERY_GROUP_NAME = create(CommonAttributes.DISCOVERY_GROUP_NAME, STRING)
            .setRequired(false)
            .setAlternatives(ALLOW_DIRECT_CONNECTIONS_ONLY.getName(), CONNECTOR_REFS.getName())
            .setAttributeMarshaller(AttributeMarshallers.DISCOVERY_GROUP_MARSHALLER)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition FORWARD_WHEN_NO_CONSUMERS = create("forward-when-no-consumers", BOOLEAN)
            .setDefaultValue(new ModelNode(false))
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition INITIAL_CONNECT_ATTEMPTS = create("initial-connect-attempts", INT)
            .setDefaultValue(new ModelNode(-1))
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition MAX_HOPS = create("max-hops", INT)
            .setDefaultValue(new ModelNode(1))
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition MAX_RETRY_INTERVAL = create("max-retry-interval", LONG)
            .setDefaultValue(new ModelNode(2000L))
            .setRequired(false)
            .setAllowExpression(true)
            .setMeasurementUnit(MILLISECONDS)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition NOTIFICATION_ATTEMPTS = create("notification-attempts", INT)
            .setDefaultValue(new ModelNode(2))
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition NOTIFICATION_INTERVAL = create("notification-interval",LONG)
            .setDefaultValue(new ModelNode(1000L))
            .setMeasurementUnit(MILLISECONDS)
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition RETRY_INTERVAL = create("retry-interval", LONG)
            .setDefaultValue(new ModelNode(500L))
            .setMeasurementUnit(MILLISECONDS)
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition RECONNECT_ATTEMPTS = create("reconnect-attempts", INT)
            .setDefaultValue(new ModelNode(-1))
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition RETRY_INTERVAL_MULTIPLIER = create("retry-interval-multiplier", DOUBLE)
            .setDefaultValue(new ModelNode(1.0))
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition USE_DUPLICATE_DETECTION = create("use-duplicate-detection", BOOLEAN)
            .setDefaultValue(new ModelNode(true))
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static final AttributeDefinition[] ATTRIBUTES = {
            ADDRESS, CONNECTOR_REF,
            CHECK_PERIOD,
            CONNECTION_TTL,
            CommonAttributes.MIN_LARGE_MESSAGE_SIZE,
            CommonAttributes.CALL_TIMEOUT,
            CommonAttributes.CALL_FAILOVER_TIMEOUT,
            RETRY_INTERVAL, RETRY_INTERVAL_MULTIPLIER, MAX_RETRY_INTERVAL,
            INITIAL_CONNECT_ATTEMPTS,
            RECONNECT_ATTEMPTS, USE_DUPLICATE_DETECTION,
            FORWARD_WHEN_NO_CONSUMERS, MAX_HOPS,
            CommonAttributes.BRIDGE_CONFIRMATION_WINDOW_SIZE,
            NOTIFICATION_ATTEMPTS,
            NOTIFICATION_INTERVAL,
            CONNECTOR_REFS,
            ALLOW_DIRECT_CONNECTIONS_ONLY,
            DISCOVERY_GROUP_NAME,
    };

    public static final AttributeDefinition[] ATTRIBUTES_WITH_EXPRESSION_ALLOWED_IN_1_2_0 = {
            ADDRESS,
            ALLOW_DIRECT_CONNECTIONS_ONLY, CHECK_PERIOD, CONNECTION_TTL, FORWARD_WHEN_NO_CONSUMERS, MAX_HOPS,
            MAX_RETRY_INTERVAL,
            CommonAttributes.MIN_LARGE_MESSAGE_SIZE, RETRY_INTERVAL, RETRY_INTERVAL_MULTIPLIER,
            USE_DUPLICATE_DETECTION, CommonAttributes.CALL_TIMEOUT,
            RECONNECT_ATTEMPTS, CommonAttributes.BRIDGE_CONFIRMATION_WINDOW_SIZE
    };

    public static final SimpleAttributeDefinition NODE_ID = create("node-id", STRING)
            .setStorageRuntime()
            .build();

    public static final SimpleAttributeDefinition TOPOLOGY = create("topology", STRING)
            .setStorageRuntime()
            .build();

    static final ClusterConnectionDefinition INSTANCE = new ClusterConnectionDefinition();

    private ClusterConnectionDefinition() {
        super(PATH,
                MessagingExtension.getResourceDescriptionResolver(CommonAttributes.CLUSTER_CONNECTION),
                new ModelOnlyAddStepHandler(ATTRIBUTES) {
                    @Override
                    protected void populateModel(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
                        super.populateModel(context, operation, resource);
                        AlternativeAttributeCheckHandler.checkAlternatives(operation, CONNECTOR_REFS.getName(), (DISCOVERY_GROUP_NAME.getName()), true);
                    }
                },
                ATTRIBUTES);
        setDeprecated(MessagingExtension.DEPRECATED_SINCE);
    }
}

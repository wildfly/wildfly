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
import static org.jboss.as.messaging.CommonAttributes.CONNECTOR_REF_STRING;
import static org.jboss.as.messaging.CommonAttributes.MESSAGING_SECURITY_DEF;
import static org.jboss.as.messaging.CommonAttributes.STATIC_CONNECTORS;
import static org.jboss.dmr.ModelType.BOOLEAN;
import static org.jboss.dmr.ModelType.INT;
import static org.jboss.dmr.ModelType.STRING;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelOnlyAddStepHandler;
import org.jboss.as.controller.ModelOnlyResourceDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PrimitiveListAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;

/**
 * Bridge resource definition
 *
 * @author <a href="http://jmesnil.net">Jeff Mesnil</a> (c) 2012 Red Hat Inc.
 */
public class BridgeDefinition extends ModelOnlyResourceDefinition {

    public static final PathElement PATH = PathElement.pathElement(CommonAttributes.BRIDGE);

    public static final PrimitiveListAttributeDefinition CONNECTOR_REFS = PrimitiveListAttributeDefinition.Builder.of(STATIC_CONNECTORS, STRING)
            .setRequired(false)
            .setAlternatives(CommonAttributes.DISCOVERY_GROUP_NAME)
            .setElementValidator(new StringLengthValidator(1))
            .setXmlName(CONNECTOR_REF_STRING)
            .setAttributeMarshaller(new AttributeMarshallers.WrappedListAttributeMarshaller(STATIC_CONNECTORS))
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition DISCOVERY_GROUP_NAME = create(CommonAttributes.DISCOVERY_GROUP_NAME, STRING)
            .setRequired(false)
            .setAlternatives(STATIC_CONNECTORS)
            .setAttributeMarshaller(AttributeMarshallers.DISCOVERY_GROUP_MARSHALLER)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition INITIAL_CONNECT_ATTEMPTS = create("initial-connect-attempts", INT)
            .setRequired(false)
            .setDefaultValue(new ModelNode().set(-1))
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition QUEUE_NAME = create(CommonAttributes.QUEUE_NAME, STRING)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition PASSWORD = create("password", STRING)
            .setRequired(false)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode().set("CHANGE ME!!"))
            .setRestartAllServices()
            .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.CREDENTIAL)
            .addAccessConstraint(MESSAGING_SECURITY_DEF)
            .build();

    public static final SimpleAttributeDefinition USER = create("user", STRING)
            .setRequired(false)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode().set("HORNETQ.CLUSTER.ADMIN.USER"))
            .setRestartAllServices()
            .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.CREDENTIAL)
            .addAccessConstraint(MESSAGING_SECURITY_DEF)
            .build();

    public static final SimpleAttributeDefinition USE_DUPLICATE_DETECTION = create("use-duplicate-detection", BOOLEAN)
            .setRequired(false)
            .setDefaultValue(new ModelNode().set(true))
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition RECONNECT_ATTEMPTS = create("reconnect-attempts", INT)
            .setRequired(false)
            .setDefaultValue(new ModelNode().set(-1))
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition RECONNECT_ATTEMPTS_ON_SAME_NODE = create("reconnect-attempts-on-same-node", INT)
            .setRequired(false)
            .setDefaultValue(new ModelNode().set(10))
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition FORWARDING_ADDRESS = create("forwarding-address", STRING)
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static final AttributeDefinition[] ATTRIBUTES = {
            QUEUE_NAME, FORWARDING_ADDRESS, CommonAttributes.HA,
            CommonAttributes.FILTER, CommonAttributes.TRANSFORMER_CLASS_NAME,
            CommonAttributes.MIN_LARGE_MESSAGE_SIZE, CommonAttributes.CHECK_PERIOD, CommonAttributes.CONNECTION_TTL,
            CommonAttributes.RETRY_INTERVAL, CommonAttributes.RETRY_INTERVAL_MULTIPLIER, CommonAttributes.MAX_RETRY_INTERVAL,
            INITIAL_CONNECT_ATTEMPTS,
            RECONNECT_ATTEMPTS,
            RECONNECT_ATTEMPTS_ON_SAME_NODE,
            CommonAttributes.FAILOVER_ON_SERVER_SHUTDOWN,
            USE_DUPLICATE_DETECTION, CommonAttributes.BRIDGE_CONFIRMATION_WINDOW_SIZE,
            USER, PASSWORD,
            CONNECTOR_REFS, DISCOVERY_GROUP_NAME
    };

    public static final AttributeDefinition[] ATTRIBUTES_WITH_EXPRESSION_ALLOWED_IN_1_2_0 = { QUEUE_NAME, USE_DUPLICATE_DETECTION,
            RECONNECT_ATTEMPTS, FORWARDING_ADDRESS,
            CommonAttributes.FILTER, CommonAttributes.HA, CommonAttributes.MIN_LARGE_MESSAGE_SIZE,
            CommonAttributes.CHECK_PERIOD, CommonAttributes.CONNECTION_TTL,
            CommonAttributes.RETRY_INTERVAL, CommonAttributes.RETRY_INTERVAL_MULTIPLIER, CommonAttributes.MAX_RETRY_INTERVAL,
            CommonAttributes.BRIDGE_CONFIRMATION_WINDOW_SIZE };

    static final BridgeDefinition INSTANCE = new BridgeDefinition();

    private BridgeDefinition() {
        super(PATH,
                MessagingExtension.getResourceDescriptionResolver(CommonAttributes.BRIDGE),
                new ModelOnlyAddStepHandler(ATTRIBUTES) {
                    @Override
                    protected void populateModel(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
                        super.populateModel(context, operation, resource);
                        AlternativeAttributeCheckHandler.checkAlternatives(operation, CONNECTOR_REFS.getName(), DISCOVERY_GROUP_NAME.getName(), false);
                    }
                },
                ATTRIBUTES);
        setDeprecated(MessagingExtension.DEPRECATED_SINCE);
    }
}

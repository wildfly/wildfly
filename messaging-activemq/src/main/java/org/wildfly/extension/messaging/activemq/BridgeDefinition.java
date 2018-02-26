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

package org.wildfly.extension.messaging.activemq;

import static org.jboss.as.controller.SimpleAttributeDefinitionBuilder.create;
import static org.jboss.as.controller.client.helpers.MeasurementUnit.BYTES;
import static org.jboss.dmr.ModelType.BOOLEAN;
import static org.jboss.dmr.ModelType.INT;
import static org.jboss.dmr.ModelType.STRING;
import static org.wildfly.extension.messaging.activemq.MessagingExtension.MESSAGING_SECURITY_SENSITIVE_TARGET;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.STATIC_CONNECTORS;

import java.util.Arrays;
import java.util.Collection;

import org.apache.activemq.artemis.api.config.ActiveMQDefaultConfiguration;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.AttributeMarshaller;
import org.jboss.as.controller.AttributeParser;
import org.jboss.as.controller.ObjectTypeAttributeDefinition;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.PrimitiveListAttributeDefinition;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.security.CredentialReference;
import org.jboss.dmr.ModelNode;

/**
 * Bridge resource definition
 *
 * @author <a href="http://jmesnil.net">Jeff Mesnil</a> (c) 2012 Red Hat Inc.
 */
public class BridgeDefinition extends PersistentResourceDefinition {

    public static final PrimitiveListAttributeDefinition CONNECTOR_REFS = new StringListAttributeDefinition.Builder(CommonAttributes.STATIC_CONNECTORS)
            .setRequired(true)
            .setElementValidator(new StringLengthValidator(1))
            .setAttributeParser(AttributeParser.STRING_LIST)
            .setAttributeMarshaller(AttributeMarshaller.STRING_LIST)
            .setAllowExpression(false)
            .setAlternatives(CommonAttributes.DISCOVERY_GROUP)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition DISCOVERY_GROUP_NAME = create(CommonAttributes.DISCOVERY_GROUP, STRING)
            .setRequired(true)
            .setAlternatives(STATIC_CONNECTORS)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition INITIAL_CONNECT_ATTEMPTS = create("initial-connect-attempts", INT)
            .setRequired(false)
            .setDefaultValue(new ModelNode().set(ActiveMQDefaultConfiguration.getDefaultBridgeInitialConnectAttempts()))
            .setAllowExpression(true)
            .setCorrector(InfiniteOrPositiveValidators.NEGATIVE_VALUE_CORRECTOR)
            .setValidator(InfiniteOrPositiveValidators.INT_INSTANCE)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition QUEUE_NAME = create(CommonAttributes.QUEUE_NAME, STRING)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition PRODUCER_WINDOW_SIZE = create("producer-window-size", INT)
            .setDefaultValue(new ModelNode(ActiveMQDefaultConfiguration.getDefaultBridgeProducerWindowSize()))
            .setMeasurementUnit(BYTES)
            .setRequired(false)
            .setAllowExpression(true)
            .setCorrector(InfiniteOrPositiveValidators.NEGATIVE_VALUE_CORRECTOR)
            .setValidator(InfiniteOrPositiveValidators.INT_INSTANCE)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition PASSWORD = create("password", STRING, true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode().set(ActiveMQDefaultConfiguration.getDefaultClusterPassword()))
            .setRestartAllServices()
            .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.CREDENTIAL)
            .addAccessConstraint(MESSAGING_SECURITY_SENSITIVE_TARGET)
            .setAlternatives(CredentialReference.CREDENTIAL_REFERENCE)
            .build();

    public static final SimpleAttributeDefinition USER = create("user", STRING)
            .setRequired(false)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode().set(ActiveMQDefaultConfiguration.getDefaultClusterUser()))
            .setRestartAllServices()
            .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.CREDENTIAL)
            .addAccessConstraint(MESSAGING_SECURITY_SENSITIVE_TARGET)
            .build();

    public static final ObjectTypeAttributeDefinition CREDENTIAL_REFERENCE =
            CredentialReference.getAttributeBuilder(true, false)
                    .setRestartAllServices()
                    .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.CREDENTIAL)
                    .addAccessConstraint(MESSAGING_SECURITY_SENSITIVE_TARGET)
                    .setAlternatives(PASSWORD.getName())
                    .build();

    public static final SimpleAttributeDefinition USE_DUPLICATE_DETECTION = create("use-duplicate-detection", BOOLEAN)
            .setRequired(false)
            .setDefaultValue(new ModelNode().set(ActiveMQDefaultConfiguration.isDefaultBridgeDuplicateDetection()))
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition RECONNECT_ATTEMPTS = create("reconnect-attempts", INT)
            .setRequired(false)
            .setDefaultValue(new ModelNode().set(ActiveMQDefaultConfiguration.getDefaultBridgeReconnectAttempts()))
            .setAllowExpression(true)
            .setCorrector(InfiniteOrPositiveValidators.NEGATIVE_VALUE_CORRECTOR)
            .setValidator(InfiniteOrPositiveValidators.INT_INSTANCE)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition RECONNECT_ATTEMPTS_ON_SAME_NODE = create("reconnect-attempts-on-same-node", INT)
            .setRequired(false)
            .setDefaultValue(new ModelNode().set(ActiveMQDefaultConfiguration.getDefaultBridgeConnectSameNode()))
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
            USE_DUPLICATE_DETECTION,
            PRODUCER_WINDOW_SIZE,
            CommonAttributes.BRIDGE_CONFIRMATION_WINDOW_SIZE,
            USER, PASSWORD, CREDENTIAL_REFERENCE,
            CONNECTOR_REFS, DISCOVERY_GROUP_NAME
    };

    private final boolean registerRuntimeOnly;

    BridgeDefinition(boolean registerRuntimeOnly) {
        super(MessagingExtension.BRIDGE_PATH,
                MessagingExtension.getResourceDescriptionResolver(CommonAttributes.BRIDGE),
                BridgeAdd.INSTANCE,
                BridgeRemove.INSTANCE);
        this.registerRuntimeOnly = registerRuntimeOnly;
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Arrays.asList(ATTRIBUTES);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration registry) {
        ReloadRequiredWriteAttributeHandler reloadRequiredWriteAttributeHandler = new ReloadRequiredWriteAttributeHandler(ATTRIBUTES);
        for (AttributeDefinition attr : ATTRIBUTES) {
            if (!attr.getFlags().contains(AttributeAccess.Flag.STORAGE_RUNTIME)) {
                registry.registerReadWriteAttribute(attr, null, reloadRequiredWriteAttributeHandler);
            }
        }

        BridgeControlHandler.INSTANCE.registerAttributes(registry);
    }

    @Override
    public void registerOperations(ManagementResourceRegistration registry) {
        super.registerOperations(registry);
        if (registerRuntimeOnly) {
            BridgeControlHandler.INSTANCE.registerOperations(registry, getResourceDescriptionResolver());
        }
    }
}

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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.START;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STOP;
import static org.jboss.as.controller.registry.AttributeAccess.Flag.RESTART_ALL_SERVICES;
import static org.jboss.dmr.ModelType.BOOLEAN;
import static org.jboss.dmr.ModelType.INT;
import static org.jboss.dmr.ModelType.STRING;

import java.util.EnumSet;

import org.hornetq.core.config.impl.ConfigurationImpl;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.DefaultOperationDescriptionProvider;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelNode;


/**
 * Bridge resource definition
 *
 * @author <a href="http://jmesnil.net">Jeff Mesnil</a> (c) 2012 Red Hat Inc.
 */
public class BridgeDefinition extends SimpleResourceDefinition {

    public static final String[] OPERATIONS = { START, STOP };

    private final boolean registerRuntimeOnly;

    public static final SimpleAttributeDefinition QUEUE_NAME = create(CommonAttributes.QUEUE_NAME, STRING)
            .setFlags(RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition PASSWORD = create("password", STRING)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode().set(ConfigurationImpl.DEFAULT_CLUSTER_PASSWORD))
            .setFlags(RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition USER = create("user", STRING)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode().set(ConfigurationImpl.DEFAULT_CLUSTER_USER))
            .setFlags(RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition USE_DUPLICATE_DETECTION = create("use-duplicate-detection", BOOLEAN)
            .setAllowNull(true)
            .setDefaultValue(new ModelNode().set(ConfigurationImpl.DEFAULT_BRIDGE_DUPLICATE_DETECTION))
            .setFlags(RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition RECONNECT_ATTEMPTS = create("reconnect-attempts", INT)
            .setAllowNull(true)
            .setDefaultValue(new ModelNode().set(ConfigurationImpl.DEFAULT_BRIDGE_RECONNECT_ATTEMPTS))
            .setFlags(RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition FORWARDING_ADDRESS = create("forwarding-address", STRING)
            .setAllowNull(true)
            .setFlags(RESTART_ALL_SERVICES)
            .build();

    public static final AttributeDefinition[] ATTRIBUTES = {
        BridgeDefinition.QUEUE_NAME, FORWARDING_ADDRESS, CommonAttributes.HA,
        CommonAttributes.FILTER, CommonAttributes.TRANSFORMER_CLASS_NAME,
        CommonAttributes.MIN_LARGE_MESSAGE_SIZE, CommonAttributes.CONNECTION_TTL,
        CommonAttributes.RETRY_INTERVAL, CommonAttributes.RETRY_INTERVAL_MULTIPLIER, CommonAttributes.MAX_RETRY_INTERVAL,
        CommonAttributes.CHECK_PERIOD,
        RECONNECT_ATTEMPTS,
        CommonAttributes.FAILOVER_ON_SERVER_SHUTDOWN, // FIXME this attribute is not handled by BridgeConfiguration
        USE_DUPLICATE_DETECTION, CommonAttributes.BRIDGE_CONFIRMATION_WINDOW_SIZE,
        USER, PASSWORD,
        ConnectorRefsAttribute.BRIDGE_CONNECTORS, CommonAttributes.DISCOVERY_GROUP_NAME
    };

    public BridgeDefinition(final boolean registerRuntimeOnly) {
        super(PathElement.pathElement(CommonAttributes.BRIDGE),
                MessagingExtension.getResourceDescriptionResolver(CommonAttributes.BRIDGE),
                BridgeAdd.INSTANCE,
                BridgeRemove.INSTANCE);
        this.registerRuntimeOnly = registerRuntimeOnly;
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration registry) {
        super.registerAttributes(registry);
        for (AttributeDefinition attr : ATTRIBUTES) {
            if (registerRuntimeOnly || !attr.getFlags().contains(AttributeAccess.Flag.STORAGE_RUNTIME)) {
                registry.registerReadWriteAttribute(attr, null, BridgeWriteAttributeHandler.INSTANCE);
            }
        }

        registry.registerReadOnlyAttribute(BridgeControlHandler.STARTED, BridgeControlHandler.INSTANCE);
    }

    @Override
    public void registerOperations(ManagementResourceRegistration registry) {
        if (registerRuntimeOnly) {
            for (String operation : OPERATIONS) {
                final DescriptionProvider desc = new DefaultOperationDescriptionProvider(operation, getResourceDescriptionResolver());
                registry.registerOperationHandler(operation,
                        BridgeControlHandler.INSTANCE,
                        desc,
                        EnumSet.of(OperationEntry.Flag.READ_ONLY, OperationEntry.Flag.RUNTIME_ONLY));
            }
        }

        super.registerOperations(registry);
    }
}
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
import static org.jboss.as.controller.registry.AttributeAccess.Flag.RESTART_ALL_SERVICES;
import static org.jboss.as.controller.registry.AttributeAccess.Flag.STORAGE_RUNTIME;
import static org.jboss.as.messaging.CommonAttributes.CONNECTORS;
import static org.jboss.as.messaging.CommonAttributes.CONNECTOR_REF_STRING;
import static org.jboss.as.messaging.CommonAttributes.GROUP_ADDRESS;
import static org.jboss.as.messaging.CommonAttributes.GROUP_PORT;
import static org.jboss.as.messaging.CommonAttributes.JGROUPS_CHANNEL;
import static org.jboss.as.messaging.CommonAttributes.JGROUPS_STACK;
import static org.jboss.as.messaging.CommonAttributes.LOCAL_BIND_ADDRESS;
import static org.jboss.as.messaging.CommonAttributes.LOCAL_BIND_PORT;
import static org.jboss.as.messaging.CommonAttributes.SOCKET_BINDING_ALTERNATIVE;
import static org.jboss.dmr.ModelType.LONG;
import static org.jboss.dmr.ModelType.STRING;

import java.util.EnumSet;

import org.hornetq.core.config.impl.ConfigurationImpl;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PrimitiveListAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleOperationDefinition;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelNode;

/**
 * Broadcast group resource definition
 *
 * @author <a href="http://jmesnil.net">Jeff Mesnil</a> (c) 2012 Red Hat Inc.
 */
public class BroadcastGroupDefinition extends SimpleResourceDefinition {

    public static final PrimitiveListAttributeDefinition CONNECTOR_REFS = PrimitiveListAttributeDefinition.Builder.of(CONNECTORS, STRING)
            .setAllowNull(true)
            .setValidator(new StringLengthValidator(1))
            .setXmlName(CONNECTOR_REF_STRING)
            .setAttributeMarshaller(new AttributeMarshallers.WrappedListAttributeMarshaller(null))
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition BROADCAST_PERIOD = create("broadcast-period", LONG)
            .setDefaultValue(new ModelNode().set(ConfigurationImpl.DEFAULT_BROADCAST_PERIOD))
            .setMeasurementUnit(MILLISECONDS)
            .setAllowNull(true)
            .setFlags(RESTART_ALL_SERVICES)
            .build();

    public static final AttributeDefinition[] ATTRIBUTES = { JGROUPS_STACK, JGROUPS_CHANNEL, SOCKET_BINDING_ALTERNATIVE, LOCAL_BIND_ADDRESS, LOCAL_BIND_PORT,
        GROUP_ADDRESS, GROUP_PORT, BROADCAST_PERIOD, CONNECTOR_REFS };

    public static final String GET_CONNECTOR_PAIRS_AS_JSON = "get-connector-pairs-as-json";

    private final boolean registerRuntimeOnly;

    public BroadcastGroupDefinition(boolean registerRuntimeOnly) {
        super(PathElement.pathElement(CommonAttributes.BROADCAST_GROUP),
                MessagingExtension.getResourceDescriptionResolver(CommonAttributes.BROADCAST_GROUP),
                BroadcastGroupAdd.INSTANCE,
                BroadcastGroupRemove.INSTANCE);
        this.registerRuntimeOnly = registerRuntimeOnly;
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration registry) {
        super.registerAttributes(registry);
        for (AttributeDefinition attr : ATTRIBUTES) {
            if (registerRuntimeOnly || !attr.getFlags().contains(STORAGE_RUNTIME)) {
                registry.registerReadWriteAttribute(attr, null, BroadcastGroupWriteAttributeHandler.INSTANCE);
            }
        }

        if (registerRuntimeOnly) {
            BroadcastGroupControlHandler.INSTANCE.registerAttributes(registry);
        }
    }

    @Override
    public void registerOperations(ManagementResourceRegistration registry) {
        if (registerRuntimeOnly) {
            BroadcastGroupControlHandler.INSTANCE.registerOperations(registry, getResourceDescriptionResolver());

            SimpleOperationDefinition op = new SimpleOperationDefinitionBuilder(GET_CONNECTOR_PAIRS_AS_JSON, getResourceDescriptionResolver())
                .withFlags(EnumSet.of(OperationEntry.Flag.READ_ONLY, OperationEntry.Flag.RUNTIME_ONLY))
                .setReplyType(STRING)
                .build();
            registry.registerOperationHandler(op, BroadcastGroupControlHandler.INSTANCE);
        }
        super.registerOperations(registry);
    }
}
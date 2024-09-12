/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.messaging.activemq;

import static org.jboss.as.controller.SimpleAttributeDefinitionBuilder.create;
import static org.jboss.as.controller.client.helpers.MeasurementUnit.MILLISECONDS;
import static org.jboss.as.controller.registry.AttributeAccess.Flag.STORAGE_RUNTIME;
import static org.jboss.dmr.ModelType.LONG;
import static org.jboss.dmr.ModelType.STRING;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.CONNECTORS;

import java.util.Arrays;
import java.util.Collection;

import org.apache.activemq.artemis.api.config.ActiveMQDefaultConfiguration;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.AttributeMarshaller;
import org.jboss.as.controller.AttributeParser;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.PrimitiveListAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleOperationDefinition;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.capability.BinaryCapabilityNameResolver;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.server.service.ClusteringServiceDescriptor;
import org.wildfly.subsystem.resource.capability.CapabilityReferenceRecorder;
/**
 * Broadcast group definition using Jgroups.
 *
 * @author Emmanuel Hugonnet (c) 2019 Red Hat, Inc.
 */
public class JGroupsBroadcastGroupDefinition extends PersistentResourceDefinition {

    public static final RuntimeCapability<Void> CAPABILITY = RuntimeCapability.Builder.of("org.wildfly.messaging.activemq.jgroups-broadcast-group", true)
            .setDynamicNameMapper(BinaryCapabilityNameResolver.PARENT_CHILD)
            .addRequirements(ClusteringServiceDescriptor.DEFAULT_COMMAND_DISPATCHER_FACTORY.getName())
            .build();

    public static final PrimitiveListAttributeDefinition CONNECTOR_REFS = new StringListAttributeDefinition.Builder(CONNECTORS)
            .setRequired(true)
            .setMinSize(1)
            .setElementValidator(new StringLengthValidator(1))
            .setAttributeParser(AttributeParser.STRING_LIST)
            .setAttributeMarshaller(AttributeMarshaller.STRING_LIST)
            .setAllowExpression(false)
            .setRestartAllServices()
            .build();

    /**
     * @see ActiveMQDefaultConfiguration#getDefaultBroadcastPeriod
     */
    public static final SimpleAttributeDefinition BROADCAST_PERIOD = create("broadcast-period", LONG)
            .setDefaultValue(new ModelNode(2000L))
            .setMeasurementUnit(MILLISECONDS)
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    @Deprecated
    public static final SimpleAttributeDefinition JGROUPS_CHANNEL_FACTORY = create(CommonAttributes.JGROUPS_CHANNEL_FACTORY)
            .setCapabilityReference("org.wildfly.clustering.jgroups.channel-factory")
            .build();

    public static final SimpleAttributeDefinition JGROUPS_CLUSTER = create(CommonAttributes.JGROUPS_CLUSTER)
            .setRequired(true)
            .setAlternatives(new String[0])
            .build();

    public static final SimpleAttributeDefinition JGROUPS_CHANNEL = create(CommonAttributes.JGROUPS_CHANNEL)
            .setCapabilityReference(CapabilityReferenceRecorder.builder(CAPABILITY, ClusteringServiceDescriptor.COMMAND_DISPATCHER_FACTORY).build())
            .build();

    public static final AttributeDefinition[] ATTRIBUTES = {JGROUPS_CHANNEL_FACTORY, JGROUPS_CHANNEL, JGROUPS_CLUSTER,
        BROADCAST_PERIOD, CONNECTOR_REFS};

    public static final String GET_CONNECTOR_PAIRS_AS_JSON = "get-connector-pairs-as-json";

    private final boolean registerRuntimeOnly;

    JGroupsBroadcastGroupDefinition(boolean registerRuntimeOnly) {
        super(new SimpleResourceDefinition.Parameters(
                MessagingExtension.JGROUPS_BROADCAST_GROUP_PATH,
                MessagingExtension.getResourceDescriptionResolver(CommonAttributes.BROADCAST_GROUP))
                    .setAddHandler(JGroupsBroadcastGroupAdd.INSTANCE)
                    .setRemoveHandler(JGroupsBroadcastGroupRemove.INSTANCE)
                    .addCapabilities(CAPABILITY));
        this.registerRuntimeOnly = registerRuntimeOnly;
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Arrays.asList(ATTRIBUTES);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration registry) {
        for (AttributeDefinition attr : ATTRIBUTES) {
            if (!attr.getFlags().contains(STORAGE_RUNTIME)) {
                registry.registerReadWriteAttribute(attr, null, BroadcastGroupWriteAttributeHandler.JGROUP_INSTANCE);
            }
        }

        BroadcastGroupControlHandler.INSTANCE.registerAttributes(registry);
    }

    @Override
    public void registerOperations(ManagementResourceRegistration registry) {
        super.registerOperations(registry);

        if (registerRuntimeOnly) {
            BroadcastGroupControlHandler.INSTANCE.registerOperations(registry, getResourceDescriptionResolver());

            SimpleOperationDefinition op = new SimpleOperationDefinitionBuilder(GET_CONNECTOR_PAIRS_AS_JSON, getResourceDescriptionResolver())
                    .setReadOnly()
                    .setRuntimeOnly()
                    .setReplyType(STRING)
                    .build();
            registry.registerOperationHandler(op, BroadcastGroupControlHandler.INSTANCE);
        }
    }

}

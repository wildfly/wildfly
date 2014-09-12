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

package org.jboss.as.clustering.infinispan.subsystem;

import org.jboss.as.clustering.controller.ReloadRequiredAddStepHandler;
import org.jboss.as.clustering.controller.transform.SimpleAttributeConverter;
import org.jboss.as.clustering.controller.transform.SimpleAttributeConverter.ModelOperationConverter;
import org.jboss.as.clustering.jgroups.subsystem.ChannelResourceDefinition;
import org.jboss.as.clustering.jgroups.subsystem.JGroupsSubsystemResourceDefinition;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource.NoSuchResourceException;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Resource description for the addressable resource /subsystem=infinispan/cache-container=X/transport=TRANSPORT
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class TransportResourceDefinition extends SimpleResourceDefinition {

    static final PathElement PATH = PathElement.pathElement(ModelKeys.TRANSPORT, ModelKeys.TRANSPORT_NAME);

    static final SimpleAttributeDefinition CHANNEL = new SimpleAttributeDefinitionBuilder(ModelKeys.CHANNEL, ModelType.STRING, true)
            .setXmlName(Attribute.CHANNEL.getLocalName())
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build()
    ;

    static final SimpleAttributeDefinition EXECUTOR = new SimpleAttributeDefinitionBuilder(ModelKeys.EXECUTOR, ModelType.STRING, true)
            .setXmlName(Attribute.EXECUTOR.getLocalName())
            .setAllowExpression(false)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    static final SimpleAttributeDefinition LOCK_TIMEOUT = new SimpleAttributeDefinitionBuilder(ModelKeys.LOCK_TIMEOUT, ModelType.LONG, true)
            .setXmlName(Attribute.LOCK_TIMEOUT.getLocalName())
            .setMeasurementUnit(MeasurementUnit.MILLISECONDS)
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setDefaultValue(new ModelNode().set(240000L))
            .build();

    @Deprecated
    static final SimpleAttributeDefinition STACK = new SimpleAttributeDefinitionBuilder(ModelKeys.STACK, ModelType.STRING, true)
            .setXmlName(Attribute.STACK.getLocalName())
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setDeprecated(InfinispanModel.VERSION_3_0_0.getVersion())
            .build();

    @Deprecated
    static final SimpleAttributeDefinition CLUSTER = new SimpleAttributeDefinitionBuilder(ModelKeys.CLUSTER, ModelType.STRING, true)
            .setXmlName(Attribute.CLUSTER.getLocalName())
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setDeprecated(InfinispanModel.VERSION_3_0_0.getVersion())
            .build();

    static final AttributeDefinition[] ATTRIBUTES = new AttributeDefinition[] { CHANNEL, STACK, EXECUTOR, LOCK_TIMEOUT };

    static void buildTransformation(ModelVersion version, ResourceTransformationDescriptionBuilder parent) {
        ResourceTransformationDescriptionBuilder builder = parent.addChildResource(PATH);

        if (InfinispanModel.VERSION_3_0_0.requiresTransformation(version)) {
            ModelOperationConverter converter = new ModelOperationConverter() {
                @Override
                public void convert(PathAddress address, String name, ModelNode value, ModelNode model, TransformationContext context) {
                    if (!value.isDefined()) {
                        PathAddress rootAddress = address.subAddress(0, address.size() - 3);
                        PathAddress subsystemAddress = rootAddress.append(JGroupsSubsystemResourceDefinition.PATH);
                        ModelNode subsystemModel = context.readResourceFromRoot(subsystemAddress).getModel();
                        String channelName = null;
                        if (model.hasDefined(CHANNEL.getName())) {
                            channelName = model.get(CHANNEL.getName()).asString();
                        } else {
                            if (subsystemModel.hasDefined(JGroupsSubsystemResourceDefinition.DEFAULT_CHANNEL.getName())) {
                                channelName = subsystemModel.get(JGroupsSubsystemResourceDefinition.DEFAULT_CHANNEL.getName()).asString();
                            }
                        }
                        if (channelName != null) {
                            String stack = null;
                            PathAddress channelAddress = subsystemAddress.append(ChannelResourceDefinition.pathElement(channelName));
                            try {
                                ModelNode channel = context.readResourceFromRoot(channelAddress).getModel();
                                if (channel.hasDefined(ChannelResourceDefinition.STACK.getName())) {
                                     stack = channel.get(ChannelResourceDefinition.STACK.getName()).asString();
                                } else {
                                    if (subsystemModel.hasDefined(JGroupsSubsystemResourceDefinition.DEFAULT_STACK.getName())) {
                                        stack = subsystemModel.get(JGroupsSubsystemResourceDefinition.DEFAULT_STACK.getName()).asString();
                                    }
                                }
                            } catch (NoSuchResourceException e) {
                                // Ignore
                            }
                            if (stack != null) {
                                value.set(stack);
                            }
                        }
                    }
                }
            };
            builder.getAttributeBuilder()
                    .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, CHANNEL)
                    .setValueConverter(new SimpleAttributeConverter(converter), STACK)
                    .addRejectCheck(RejectAttributeChecker.UNDEFINED, STACK)
                    .addRename(CHANNEL, CLUSTER.getName())
                    .end();
        }

        if (InfinispanModel.VERSION_1_4_0.requiresTransformation(version)) {
            builder.getAttributeBuilder().addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, STACK, EXECUTOR, LOCK_TIMEOUT);
        }
    }

    TransportResourceDefinition() {
        super(PATH, InfinispanExtension.getResourceDescriptionResolver(ModelKeys.TRANSPORT), new ReloadRequiredAddStepHandler(ATTRIBUTES), ReloadRequiredRemoveStepHandler.INSTANCE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        // check that we don't need a special handler here?
        final OperationStepHandler writeHandler = new ReloadRequiredWriteAttributeHandler(ATTRIBUTES);
        for (AttributeDefinition attr : ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(attr, null, writeHandler);
        }
    }
}

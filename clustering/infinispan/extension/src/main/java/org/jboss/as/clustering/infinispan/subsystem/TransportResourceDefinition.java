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

import java.util.Set;

import org.jboss.as.clustering.controller.ReloadRequiredAddStepHandler;
import org.jboss.as.clustering.controller.transform.SimpleAttributeConverter;
import org.jboss.as.clustering.controller.transform.SimpleAttributeConverter.Converter;
import org.jboss.as.clustering.controller.transform.SimpleRejectAttributeChecker;
import org.jboss.as.clustering.controller.transform.SimpleRejectAttributeChecker.Rejecter;
import org.jboss.as.clustering.infinispan.InfinispanLogger;
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
            // We need to reject if we cannot determine the underlying stack via the jgroups subsystem
            Rejecter stackRejecter = new Rejecter() {
                @Override
                public boolean reject(PathAddress address, String name, ModelNode value, ModelNode model, TransformationContext context) {
                    if (value.isDefined()) return false;
                    PathAddress rootAddress = address.subAddress(0, address.size() - 3);
                    PathAddress subsystemAddress = rootAddress.append(JGroupsSubsystemResourceDefinition.PATH);
                    ModelNode subsystemModel = context.readResourceFromRoot(subsystemAddress).getModel();
                    String channelName = null;
                    if (model.hasDefined(CHANNEL.getName())) {
                        ModelNode channel = model.get(CHANNEL.getName());
                        if (channel.getType() == ModelType.STRING) {
                            channelName = channel.asString();
                        }
                    } else if (subsystemModel.hasDefined(JGroupsSubsystemResourceDefinition.DEFAULT_CHANNEL.getName())) {
                        ModelNode defaultChannel = subsystemModel.get(JGroupsSubsystemResourceDefinition.DEFAULT_CHANNEL.getName());
                        if (defaultChannel.getType() == ModelType.STRING) {
                            channelName = defaultChannel.asString();
                        }
                    }
                    if (channelName == null) return true;
                    String stackName = null;
                    PathAddress channelAddress = subsystemAddress.append(ChannelResourceDefinition.pathElement(channelName));
                    try {
                        ModelNode channel = context.readResourceFromRoot(channelAddress).getModel();
                        if (channel.hasDefined(ChannelResourceDefinition.STACK.getName())) {
                            ModelNode stack = channel.get(ChannelResourceDefinition.STACK.getName());
                            if (stack.getType() == ModelType.STRING) {
                                stackName = stack.asString();
                            }
                        } else if (subsystemModel.hasDefined(JGroupsSubsystemResourceDefinition.DEFAULT_STACK.getName())) {
                            ModelNode defaultStack = subsystemModel.get(JGroupsSubsystemResourceDefinition.DEFAULT_STACK.getName());
                            if (defaultStack.getType() == ModelType.STRING) {
                                stackName = defaultStack.asString();
                            }
                        }
                    } catch (NoSuchResourceException e) {
                        // Ignore
                    }
                    return (stackName == null);
                }

                @Override
                public String getRejectedMessage(Set<String> attributes) {
                    return InfinispanLogger.ROOT_LOGGER.indeterminiteStack();
                }
            };
            // Lookup the stack via the jgroups channel resource, if necessary
            Converter stackConverter = new Converter() {
                @Override
                public void convert(PathAddress address, String name, ModelNode value, ModelNode model, TransformationContext context) {
                    if (!value.isDefined()) {
                        PathAddress rootAddress = address.subAddress(0, address.size() - 3);
                        PathAddress subsystemAddress = rootAddress.append(JGroupsSubsystemResourceDefinition.PATH);
                        ModelNode subsystemModel = context.readResourceFromRoot(subsystemAddress).getModel();
                        String channelName = null;
                        if (model.hasDefined(CHANNEL.getName())) {
                            ModelNode channel = model.get(CHANNEL.getName());
                            if (channel.getType() == ModelType.STRING) {
                                channelName = channel.asString();
                            }
                        } else if (subsystemModel.hasDefined(JGroupsSubsystemResourceDefinition.DEFAULT_CHANNEL.getName())) {
                            ModelNode defaultChannel = subsystemModel.get(JGroupsSubsystemResourceDefinition.DEFAULT_CHANNEL.getName());
                            if (defaultChannel.getType() == ModelType.STRING) {
                                channelName = defaultChannel.asString();
                            }
                        }
                        if (channelName != null) {
                            PathAddress channelAddress = subsystemAddress.append(ChannelResourceDefinition.pathElement(channelName));
                            try {
                                ModelNode channel = context.readResourceFromRoot(channelAddress).getModel();
                                if (channel.hasDefined(ChannelResourceDefinition.STACK.getName())) {
                                    ModelNode stack = channel.get(ChannelResourceDefinition.STACK.getName());
                                    if (stack.getType() == ModelType.STRING) {
                                        value.set(stack.asString());
                                    }
                                } else if (subsystemModel.hasDefined(JGroupsSubsystemResourceDefinition.DEFAULT_STACK.getName())) {
                                    ModelNode defaultStack = subsystemModel.get(JGroupsSubsystemResourceDefinition.DEFAULT_STACK.getName());
                                    if (defaultStack.getType() == ModelType.STRING) {
                                        value.set(defaultStack.asString());
                                    }
                                }
                            } catch (NoSuchResourceException e) {
                                // Ignore
                            }
                        }
                    }
                }
            };
            builder.getAttributeBuilder()
                    .addRejectCheck(new SimpleRejectAttributeChecker(stackRejecter), STACK)
                    .setValueConverter(new SimpleAttributeConverter(stackConverter), STACK)
                    .addRename(CHANNEL, CLUSTER.getName())
                    .end();
        }

        if (InfinispanModel.VERSION_1_4_0.requiresTransformation(version)) {
            builder.getAttributeBuilder().addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, STACK, EXECUTOR, LOCK_TIMEOUT);
        }
    }

    TransportResourceDefinition() {
        super(PATH, new InfinispanResourceDescriptionResolver(ModelKeys.TRANSPORT), new ReloadRequiredAddStepHandler(ATTRIBUTES), ReloadRequiredRemoveStepHandler.INSTANCE);
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

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

import org.jboss.as.clustering.controller.AddStepHandler;
import org.jboss.as.clustering.controller.DefaultableCapabilityReference;
import org.jboss.as.clustering.controller.RemoveStepHandler;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.SimpleAliasEntry;
import org.jboss.as.clustering.controller.transform.SimpleAttributeConverter;
import org.jboss.as.clustering.controller.transform.SimpleAttributeConverter.Converter;
import org.jboss.as.clustering.controller.transform.SimpleRejectAttributeChecker;
import org.jboss.as.clustering.controller.transform.SimpleRejectAttributeChecker.Rejecter;
import org.jboss.as.clustering.infinispan.InfinispanLogger;
import org.jboss.as.clustering.jgroups.subsystem.ChannelResourceDefinition;
import org.jboss.as.clustering.jgroups.subsystem.JGroupsSubsystemResourceDefinition;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.CapabilityReferenceRecorder;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource.NoSuchResourceException;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.clustering.jgroups.spi.JGroupsRequirement;

/**
 * Resource description for the addressable resource and its alias
 *
 * /subsystem=infinispan/cache-container=X/transport=jgroups
 * /subsystem=infinispan/cache-container=X/transport=TRANSPORT
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class JGroupsTransportResourceDefinition extends TransportResourceDefinition {

    static final PathElement LEGACY_PATH = pathElement("TRANSPORT");
    static final PathElement PATH = pathElement("jgroups");

    enum Capability implements org.jboss.as.clustering.controller.Capability {
        TRANSPORT_CHANNEL_FACTORY("org.wildfly.clustering.infinispan.transport.channel-factory"),
        ;
        private final RuntimeCapability<Void> definition;

        Capability(String name) {
            this.definition = RuntimeCapability.Builder.of(name, true).build();
        }

        @Override
        public RuntimeCapability<Void> getDefinition() {
            return this.definition;
        }

        @Override
        public RuntimeCapability<?> resolve(PathAddress address) {
            return this.definition.fromBaseCapability(address.getParent().getLastElement().getValue());
        }
    }

    enum Attribute implements org.jboss.as.clustering.controller.Attribute {
        CHANNEL("channel", ModelType.STRING, new DefaultableCapabilityReference(Capability.TRANSPORT_CHANNEL_FACTORY, JGroupsRequirement.CHANNEL_FACTORY)),
        LOCK_TIMEOUT("lock-timeout", ModelType.LONG, new ModelNode(240000L)),
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type, CapabilityReferenceRecorder reference) {
            this.definition = createBuilder(name, type, null)
                    .setAllowExpression(false)
                    .setCapabilityReference(reference)
                    .build();
        }

        Attribute(String name, ModelType type, ModelNode defaultValue) {
            this.definition = createBuilder(name, type, defaultValue).build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    @Deprecated
    enum ExecutorAttribute implements org.jboss.as.clustering.controller.Attribute {
        TRANSPORT("executor"),
        ;
        private final AttributeDefinition definition;

        ExecutorAttribute(String name) {
            this.definition = createBuilder(name, ModelType.STRING, null).setAllowExpression(false).setDeprecated(InfinispanModel.VERSION_3_0_0.getVersion()).build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    @Deprecated
    enum DeprecatedAttribute implements org.jboss.as.clustering.controller.Attribute {
        CLUSTER("cluster", ModelType.STRING, null, InfinispanModel.VERSION_3_0_0),
        STACK("stack", ModelType.STRING, null, InfinispanModel.VERSION_3_0_0),
        ;
        private final AttributeDefinition definition;

        DeprecatedAttribute(String name, ModelType type, ModelNode defaultValue, InfinispanModel deprecation) {
            this.definition = createBuilder(name, type, defaultValue).setDeprecated(deprecation.getVersion()).build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    static SimpleAttributeDefinitionBuilder createBuilder(String name, ModelType type, ModelNode defaultValue) {
        return new SimpleAttributeDefinitionBuilder(name, type)
                .setAllowExpression(true)
                .setAllowNull(true)
                .setDefaultValue(defaultValue)
                .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                .setMeasurementUnit((type == ModelType.LONG) ? MeasurementUnit.MILLISECONDS : null)
                ;
    }

    @SuppressWarnings("deprecation")
    static void buildTransformation(ModelVersion version, ResourceTransformationDescriptionBuilder parent) {
        ResourceTransformationDescriptionBuilder builder = InfinispanModel.VERSION_4_0_0.requiresTransformation(version) ? parent.addChildRedirection(PATH, LEGACY_PATH) : parent.addChildResource(PATH);

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
                    if (model.hasDefined(Attribute.CHANNEL.getName())) {
                        ModelNode channel = model.get(Attribute.CHANNEL.getName());
                        if (channel.getType() == ModelType.STRING) {
                            channelName = channel.asString();
                        }
                    } else if (subsystemModel.hasDefined(JGroupsSubsystemResourceDefinition.Attribute.DEFAULT_CHANNEL.getName())) {
                        ModelNode defaultChannel = subsystemModel.get(JGroupsSubsystemResourceDefinition.Attribute.DEFAULT_CHANNEL.getName());
                        if (defaultChannel.getType() == ModelType.STRING) {
                            channelName = defaultChannel.asString();
                        }
                    }
                    if (channelName == null) return true;
                    String stackName = null;
                    PathAddress channelAddress = subsystemAddress.append(ChannelResourceDefinition.pathElement(channelName));
                    try {
                        ModelNode channel = context.readResourceFromRoot(channelAddress).getModel();
                        if (channel.hasDefined(ChannelResourceDefinition.Attribute.STACK.getName())) {
                            ModelNode stack = channel.get(ChannelResourceDefinition.Attribute.STACK.getName());
                            if (stack.getType() == ModelType.STRING) {
                                stackName = stack.asString();
                            }
                        } else if (subsystemModel.hasDefined(JGroupsSubsystemResourceDefinition.Attribute.DEFAULT_STACK.getName())) {
                            ModelNode defaultStack = subsystemModel.get(JGroupsSubsystemResourceDefinition.Attribute.DEFAULT_STACK.getName());
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
                        if (model.hasDefined(Attribute.CHANNEL.getName())) {
                            ModelNode channel = model.get(Attribute.CHANNEL.getName());
                            if (channel.getType() == ModelType.STRING) {
                                channelName = channel.asString();
                            }
                        } else if (subsystemModel.hasDefined(JGroupsSubsystemResourceDefinition.Attribute.DEFAULT_CHANNEL.getName())) {
                            ModelNode defaultChannel = subsystemModel.get(JGroupsSubsystemResourceDefinition.Attribute.DEFAULT_CHANNEL.getName());
                            if (defaultChannel.getType() == ModelType.STRING) {
                                channelName = defaultChannel.asString();
                            }
                        }
                        if (channelName != null) {
                            PathAddress channelAddress = subsystemAddress.append(ChannelResourceDefinition.pathElement(channelName));
                            try {
                                ModelNode channel = context.readResourceFromRoot(channelAddress).getModel();
                                if (channel.hasDefined(ChannelResourceDefinition.Attribute.STACK.getName())) {
                                    ModelNode stack = channel.get(ChannelResourceDefinition.Attribute.STACK.getName());
                                    if (stack.getType() == ModelType.STRING) {
                                        value.set(stack.asString());
                                    }
                                } else if (subsystemModel.hasDefined(JGroupsSubsystemResourceDefinition.Attribute.DEFAULT_STACK.getName())) {
                                    ModelNode defaultStack = subsystemModel.get(JGroupsSubsystemResourceDefinition.Attribute.DEFAULT_STACK.getName());
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
                    .addRejectCheck(new SimpleRejectAttributeChecker(stackRejecter), DeprecatedAttribute.STACK.getDefinition())
                    .setValueConverter(new SimpleAttributeConverter(stackConverter), DeprecatedAttribute.STACK.getDefinition())
                    .addRename(Attribute.CHANNEL.getDefinition(), DeprecatedAttribute.CLUSTER.getDefinition().getName())
                    .end();
        }
    }

    JGroupsTransportResourceDefinition() {
        super(PATH);
    }

    @Override
    public void register(ManagementResourceRegistration parentRegistration) {
        ManagementResourceRegistration registration = parentRegistration.registerSubModel(this);
        parentRegistration.registerAlias(LEGACY_PATH, new SimpleAliasEntry(registration));

        ResourceDescriptor descriptor = new ResourceDescriptor(this.getResourceDescriptionResolver())
                .addAttributes(Attribute.class)
                .addAttributes(ExecutorAttribute.class)
                .addAttributes(DeprecatedAttribute.class)
                .addCapabilities(Capability.class)
                ;
        ResourceServiceHandler handler = new JGroupsTransportServiceHandler();
        new AddStepHandler(descriptor, handler).register(registration);
        new RemoveStepHandler(descriptor, handler).register(registration);
    }
}

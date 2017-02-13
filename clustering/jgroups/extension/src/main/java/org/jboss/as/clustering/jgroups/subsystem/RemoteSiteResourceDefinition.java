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
package org.jboss.as.clustering.jgroups.subsystem;

import org.jboss.as.clustering.controller.CapabilityReference;
import org.jboss.as.clustering.controller.ChildResourceDefinition;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceBuilderFactory;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.RestartParentResourceRegistration;
import org.jboss.as.clustering.controller.SimpleResourceServiceHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.CapabilityReferenceRecorder;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.description.AttributeConverter;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.clustering.jgroups.spi.JGroupsRequirement;
import org.wildfly.clustering.jgroups.spi.RelayConfiguration;

/**
 * Resource definition for subsystem=jgroups/stack=X/relay=RELAY/remote-site=Y
 *
 * @author Paul Ferraro
 */
public class RemoteSiteResourceDefinition extends ChildResourceDefinition {

    static final PathElement WILDCARD_PATH = pathElement(PathElement.WILDCARD_VALUE);

    static PathElement pathElement(String name) {
        return PathElement.pathElement("remote-site", name);
    }

    enum Capability implements org.jboss.as.clustering.controller.Capability {
        RELAY_CHANNEL_SOURCE("org.wildfly.clustering.jgroups.stack.relay.site-channel-source"),
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
        public RuntimeCapability<Void> resolve(PathAddress address) {
            return this.definition.fromBaseCapability(address.getParent().getParent().getLastElement().getValue() + "." + address.getLastElement().getValue());
        }
    }

    enum Attribute implements org.jboss.as.clustering.controller.Attribute {
        CHANNEL("channel", ModelType.STRING, new CapabilityReference(Capability.RELAY_CHANNEL_SOURCE, JGroupsRequirement.CHANNEL_SOURCE)),
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type, CapabilityReferenceRecorder reference) {
            this.definition = createBuilder(name, type)
                    .setRequired(true)
                    .setCapabilityReference(reference)
                    .build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    enum DeprecatedAttribute implements org.jboss.as.clustering.controller.Attribute {
        STACK("stack", ModelType.STRING, JGroupsModel.VERSION_3_0_0),
        CLUSTER("cluster", ModelType.STRING, JGroupsModel.VERSION_3_0_0),
        ;
        private final AttributeDefinition definition;

        DeprecatedAttribute(String name, ModelType type, JGroupsModel deprecation) {
            this.definition = createBuilder(name, type).setRequired(false).setDeprecated(deprecation.getVersion()).build();
        }

        private static SimpleAttributeDefinitionBuilder createBuilder(String name, ModelType type) {
            return new SimpleAttributeDefinitionBuilder(name, ModelType.STRING)
                .setAllowExpression(true)
                .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            ;
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    static SimpleAttributeDefinitionBuilder createBuilder(String name, ModelType type) {
        return new SimpleAttributeDefinitionBuilder(name, ModelType.STRING)
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
        ;
    }

    @SuppressWarnings("deprecation")
    static void buildTransformation(ModelVersion version, ResourceTransformationDescriptionBuilder parent) {
        ResourceTransformationDescriptionBuilder builder = parent.addChildResource(WILDCARD_PATH);

        if (JGroupsModel.VERSION_3_0_0.requiresTransformation(version)) {
            AttributeConverter converter = new AttributeConverter() {
                @Override
                public void convertOperationParameter(PathAddress address, String name, ModelNode value, ModelNode operation, TransformationContext context) {
                    // Nothing to convert
                }

                @Override
                public void convertResourceAttribute(PathAddress address, String name, ModelNode value, TransformationContext context) {
                    ModelNode remoteSite = context.readResourceFromRoot(address).getModel();
                    String channelName = remoteSite.get(Attribute.CHANNEL.getName()).asString();
                    if (DeprecatedAttribute.STACK.getName().equals(name)) {
                        PathAddress subsystemAddress = address.subAddress(0, address.size() - 3);
                        PathAddress channelAddress = subsystemAddress.append(ChannelResourceDefinition.pathElement(channelName));
                        ModelNode channel = context.readResourceFromRoot(channelAddress).getModel();

                        if (channel.hasDefined(ChannelResourceDefinition.Attribute.STACK.getName())) {
                            value.set(channel.get(ChannelResourceDefinition.Attribute.STACK.getName()).asString());
                        } else {
                            ModelNode subsystem = context.readResourceFromRoot(subsystemAddress).getModel();
                            value.set(subsystem.get(JGroupsSubsystemResourceDefinition.Attribute.DEFAULT_STACK.getName()).asString());
                        }
                    } else if (DeprecatedAttribute.CLUSTER.getName().equals(name)) {
                        value.set(channelName);
                    } else {
                        throw new IllegalStateException();
                    }
                }
            };
            builder.getAttributeBuilder()
                    .setValueConverter(converter, DeprecatedAttribute.STACK.getDefinition(), DeprecatedAttribute.CLUSTER.getDefinition())
                    .setDiscard(DiscardAttributeChecker.ALWAYS, Attribute.CHANNEL.getDefinition())
                    .end();
        }
    }

    private final ResourceServiceBuilderFactory<RelayConfiguration> parentBuilderFactory;

    RemoteSiteResourceDefinition(ResourceServiceBuilderFactory<RelayConfiguration> parentBuilderFactory) {
        super(WILDCARD_PATH, new JGroupsResourceDescriptionResolver(WILDCARD_PATH));
        this.parentBuilderFactory = parentBuilderFactory;
    }

    @Override
    public void register(ManagementResourceRegistration parentRegistration) {
        ManagementResourceRegistration registration = parentRegistration.registerSubModel(this);

        ResourceDescriptor descriptor = new ResourceDescriptor(this.getResourceDescriptionResolver())
                .addAttributes(Attribute.class)
                .addAttributes(DeprecatedAttribute.class)
                .addCapabilities(Capability.class)
                ;
        ResourceServiceHandler handler = new SimpleResourceServiceHandler<>(address -> new RemoteSiteConfigurationBuilder(address));
        new RestartParentResourceRegistration<>(this.parentBuilderFactory, descriptor, handler).register(registration);
    }
}

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

import org.jboss.as.clustering.controller.ReloadRequiredAddStepHandler;
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
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.description.AttributeConverter;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Resource definition for subsystem=jgroups/stack=X/relay=RELAY/remote-site=Y
 *
 * @author Paul Ferraro
 */
public class RemoteSiteResourceDefinition extends SimpleResourceDefinition {

    static final PathElement WILDCARD_PATH = pathElement(PathElement.WILDCARD_VALUE);

    static PathElement pathElement(String name) {
        return PathElement.pathElement(ModelKeys.REMOTE_SITE, name);
    }

    static final SimpleAttributeDefinition CHANNEL = new SimpleAttributeDefinitionBuilder(ModelKeys.CHANNEL, ModelType.STRING, false)
            .setXmlName(Attribute.CHANNEL.getLocalName())
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .build();

    @Deprecated
    static final SimpleAttributeDefinition STACK = new SimpleAttributeDefinitionBuilder(ModelKeys.STACK, ModelType.STRING, true)
            .setXmlName(Attribute.STACK.getLocalName())
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .setDeprecated(JGroupsModel.VERSION_3_0_0.getVersion())
            .build();

    @Deprecated
    static final SimpleAttributeDefinition CLUSTER = new SimpleAttributeDefinitionBuilder(ModelKeys.CLUSTER, ModelType.STRING, true)
            .setXmlName(Attribute.CLUSTER.getLocalName())
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .setDeprecated(JGroupsModel.VERSION_3_0_0.getVersion())
            .build();

    static final AttributeDefinition[] ATTRIBUTES = new AttributeDefinition[] { CHANNEL, STACK };

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
                    String channelName = remoteSite.get(CHANNEL.getName()).asString();
                    if (STACK.getName().equals(name)) {
                        PathAddress subsystemAddress = address.subAddress(0, address.size() - 3);
                        PathAddress channelAddress = subsystemAddress.append(ChannelResourceDefinition.pathElement(channelName));
                        ModelNode channel = context.readResourceFromRoot(channelAddress).getModel();

                        if (channel.hasDefined(ChannelResourceDefinition.STACK.getName())) {
                            value.set(channel.get(ChannelResourceDefinition.STACK.getName()).asString());
                        } else {
                            ModelNode subsystem = context.readResourceFromRoot(subsystemAddress).getModel();
                            value.set(subsystem.get(JGroupsSubsystemResourceDefinition.DEFAULT_STACK.getName()).asString());
                        }
                    } else if (CLUSTER.getName().equals(name)) {
                        value.set(channelName);
                    } else {
                        throw new IllegalStateException();
                    }
                }
            };
            builder.getAttributeBuilder()
                    .setValueConverter(converter, STACK, CLUSTER)
                    .setDiscard(DiscardAttributeChecker.ALWAYS, CHANNEL)
                    .end();
        }
    }

    RemoteSiteResourceDefinition() {
        super(WILDCARD_PATH, JGroupsExtension.getResourceDescriptionResolver(ModelKeys.REMOTE_SITE), new ReloadRequiredAddStepHandler(ATTRIBUTES), ReloadRequiredRemoveStepHandler.INSTANCE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration registration) {
        final OperationStepHandler writeHandler = new ReloadRequiredWriteAttributeHandler(ATTRIBUTES);
        for (AttributeDefinition attribute: ATTRIBUTES) {
            registration.registerReadWriteAttribute(attribute, null, writeHandler);
        }
    }
}

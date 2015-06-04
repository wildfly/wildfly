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

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.DiscardPolicy;
import org.jboss.as.controller.transform.description.DynamicDiscardPolicy;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescription;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;


/**
 * The root resource of the JGroups subsystem.
 *
 * @author Richard Achmatowicz (c) 2012 Red Hat Inc.
 */
public class JGroupsSubsystemResourceDefinition extends SimpleResourceDefinition {

    public static final PathElement PATH = PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, JGroupsExtension.SUBSYSTEM_NAME);

    // attributes
    public static final SimpleAttributeDefinition DEFAULT_CHANNEL = new SimpleAttributeDefinitionBuilder(ModelKeys.DEFAULT_CHANNEL, ModelType.STRING, true)
            .setXmlName(Attribute.DEFAULT.getLocalName())
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition DEFAULT_STACK = new SimpleAttributeDefinitionBuilder(ModelKeys.DEFAULT_STACK, ModelType.STRING, true)
            .setXmlName(Attribute.DEFAULT.getLocalName())
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setDeprecated(JGroupsModel.VERSION_3_0_0.getVersion())
            .build()
    ;

    static final AttributeDefinition[] ATTRIBUTES = new AttributeDefinition[] { DEFAULT_CHANNEL, DEFAULT_STACK };

    private final boolean allowRuntimeOnlyRegistration;

    static TransformationDescription buildTransformers(ModelVersion version) {
        ResourceTransformationDescriptionBuilder builder = TransformationDescriptionBuilder.Factory.createSubsystemInstance();

        if (JGroupsModel.VERSION_3_0_0.requiresTransformation(version)) {
            builder.getAttributeBuilder()
                    .setDiscard(DiscardAttributeChecker.ALWAYS, DEFAULT_CHANNEL)
                    .end();

            DynamicDiscardPolicy channelDiscardRejectPolicy = new DynamicDiscardPolicy() {
                @Override
                public DiscardPolicy checkResource(TransformationContext context, PathAddress address) {
                    // Check whether all channel resources are used by the infinispan subsystem, and transformed
                    // by its corresponding transformers; reject otherwise

                    // n.b. we need to hardcode the values because otherwise we would end up with cyclical dependency

                    String channelName = address.getLastElement().getValue();

                    PathAddress rootAddress = address.subAddress(0, address.size() - 2);
                    PathAddress subsystemAddress = rootAddress.append(PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, "infinispan"));

                    Resource infinispanResource;
                    try {
                        infinispanResource = context.readResourceFromRoot(subsystemAddress);
                    } catch (Resource.NoSuchResourceException nsre) {
                        return DiscardPolicy.REJECT_AND_WARN;
                    }
                    ModelNode infinispanModel = Resource.Tools.readModel(infinispanResource);

                    if (infinispanModel.hasDefined("cache-container")) {
                        for (ModelNode container : infinispanModel.get("cache-container").asList()) {
                            ModelNode cacheContainer = container.get(0);
                            if (cacheContainer.hasDefined("transport")) {
                                ModelNode transport = cacheContainer.get("transport").get("TRANSPORT");
                                if (transport.hasDefined("channel")) {
                                    String channel = transport.get("channel").asString();
                                    if (channel.equals(channelName)) {
                                        return DiscardPolicy.SILENT;
                                    }
                                } else {
                                    // In that case, if this were the default channel, it can be discarded too
                                    ModelNode subsystem = context.readResourceFromRoot(address.subAddress(0, address.size() - 1)).getModel();
                                    if (subsystem.hasDefined(DEFAULT_CHANNEL.getName())) {
                                        if (subsystem.get(DEFAULT_CHANNEL.getName()).asString().equals(channelName)) {
                                            return DiscardPolicy.SILENT;
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // No references to this channel, we need to reject it.
                    return DiscardPolicy.REJECT_AND_WARN;
                }

            };
            builder.addChildResource(ChannelResourceDefinition.WILDCARD_PATH, channelDiscardRejectPolicy);
        } else {
            ChannelResourceDefinition.buildTransformation(version, builder);
        }

        StackResourceDefinition.buildTransformation(version, builder);

        return builder.build();
    }

    JGroupsSubsystemResourceDefinition(boolean allowRuntimeOnlyRegistration) {
        super(PATH, new JGroupsResourceDescriptionResolver(), new JGroupsSubsystemAddHandler(), new JGroupsSubsystemRemoveHandler(allowRuntimeOnlyRegistration));
        this.allowRuntimeOnlyRegistration = allowRuntimeOnlyRegistration;
    }

    @Override
    public void registerOperations(ManagementResourceRegistration registration) {
        super.registerOperations(registration);
        registration.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration registration) {
        OperationStepHandler writeHandler = new ReloadRequiredWriteAttributeHandler(ATTRIBUTES);
        for (AttributeDefinition attribute: ATTRIBUTES) {
            registration.registerReadWriteAttribute(attribute, null, writeHandler);
        }
    }

    @Override
    public void registerChildren(ManagementResourceRegistration registration) {
        registration.registerSubModel(new ChannelResourceDefinition(this.allowRuntimeOnlyRegistration));
        registration.registerSubModel(new StackResourceDefinition(this.allowRuntimeOnlyRegistration));
    }
}

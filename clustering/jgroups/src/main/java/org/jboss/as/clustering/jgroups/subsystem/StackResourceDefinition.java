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

import java.util.Collections;
import java.util.List;

import org.jboss.as.clustering.controller.Operations;
import org.jboss.as.clustering.controller.ReloadRequiredAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.ObjectListAttributeDefinition;
import org.jboss.as.controller.ObjectTypeAttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.transform.ResourceTransformationContext;
import org.jboss.as.controller.transform.ResourceTransformer;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;

/**
 * Resource description for the addressable resource /subsystem=jgroups/stack=X
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class StackResourceDefinition extends SimpleResourceDefinition {

    public static final PathElement WILDCARD_PATH = pathElement(PathElement.WILDCARD_VALUE);

    public static PathElement pathElement(String name) {
        return PathElement.pathElement(ModelKeys.STACK, name);
    }

    private final boolean allowRuntimeOnlyRegistration;

    @Deprecated
    static final ObjectTypeAttributeDefinition TRANSPORT = ObjectTypeAttributeDefinition.Builder.of(ModelKeys.TRANSPORT, ProtocolResourceDefinition.TYPE, TransportResourceDefinition.SHARED, ProtocolResourceDefinition.SOCKET_BINDING, TransportResourceDefinition.DIAGNOSTICS_SOCKET_BINDING, TransportResourceDefinition.DEFAULT_EXECUTOR, TransportResourceDefinition.OOB_EXECUTOR, TransportResourceDefinition.TIMER_EXECUTOR, TransportResourceDefinition.THREAD_FACTORY, TransportResourceDefinition.SITE, TransportResourceDefinition.RACK, TransportResourceDefinition.MACHINE, ProtocolResourceDefinition.PROPERTIES)
            .setDeprecated(JGroupsModel.VERSION_3_0_0.getVersion())
            .setAllowNull(true)
            .setSuffix(null)
            .build();

    @Deprecated
    static final ObjectTypeAttributeDefinition PROTOCOL = ObjectTypeAttributeDefinition.Builder.of(ModelKeys.PROTOCOL, ProtocolResourceDefinition.TYPE, ProtocolResourceDefinition.SOCKET_BINDING, ProtocolResourceDefinition.PROPERTIES)
            .setAllowNull(true)
            .setSuffix("protocol")
            .build();

    @Deprecated
    static final AttributeDefinition PROTOCOLS = ObjectListAttributeDefinition.Builder.of(ModelKeys.PROTOCOLS, PROTOCOL)
            .setDeprecated(JGroupsModel.VERSION_3_0_0.getVersion())
            .setAllowNull(true)
            .build();

    // operations
    @Deprecated
    static final OperationDefinition ADD_PROTOCOL = new SimpleOperationDefinitionBuilder(ModelKeys.ADD_PROTOCOL, JGroupsExtension.getResourceDescriptionResolver("stack"))
            .setParameters(ProtocolResourceDefinition.SOCKET_BINDING)
            .addParameter(ProtocolResourceDefinition.TYPE)
            .addParameter(ProtocolResourceDefinition.PROPERTIES)
            .setDeprecated(JGroupsModel.VERSION_3_0_0.getVersion())
            .build();

    @Deprecated
    static final OperationDefinition REMOVE_PROTOCOL = new SimpleOperationDefinitionBuilder(ModelKeys.REMOVE_PROTOCOL, JGroupsExtension.getResourceDescriptionResolver("stack"))
            .setParameters(ProtocolResourceDefinition.TYPE)
            .setDeprecated(JGroupsModel.VERSION_3_0_0.getVersion())
            .build();

    static void buildTransformation(ModelVersion version, ResourceTransformationDescriptionBuilder parent) {
        ResourceTransformationDescriptionBuilder builder = parent.addChildResource(WILDCARD_PATH);

        if (JGroupsModel.VERSION_3_0_0.requiresTransformation(version)) {
            // Create legacy "protocols" attributes, which lists protocols by name
            ResourceTransformer transformer = new ResourceTransformer() {
                @Override
                public void transformResource(ResourceTransformationContext context, PathAddress address, Resource resource) throws OperationFailedException {
                    for (String name : resource.getChildrenNames(ProtocolResourceDefinition.WILDCARD_PATH.getKey())) {
                        resource.getModel().get(PROTOCOLS.getName()).add(name);
                    }
                    context.addTransformedResource(PathAddress.EMPTY_ADDRESS, resource).processChildren(resource);
                }
            };
            builder.setCustomResourceTransformer(transformer);
        }

        if (JGroupsModel.VERSION_2_0_0.requiresTransformation(version)) {
            builder.rejectChildResource(RelayResourceDefinition.PATH);
        } else {
            RelayResourceDefinition.buildTransformation(version, builder);
        }

        TransportResourceDefinition.buildTransformation(version, builder);
        ProtocolResourceDefinition.buildTransformation(version, builder);
    }

    // registration
    public StackResourceDefinition(boolean allowRuntimeOnlyRegistration) {
        super(WILDCARD_PATH, JGroupsExtension.getResourceDescriptionResolver(ModelKeys.STACK), null, new StackRemoveHandler());
        this.allowRuntimeOnlyRegistration = allowRuntimeOnlyRegistration;
    }

    @Override
    public void registerOperations(ManagementResourceRegistration registration) {
        super.registerOperations(registration);

        OperationDefinition addOperation = new SimpleOperationDefinitionBuilder(ModelDescriptionConstants.ADD, JGroupsExtension.getResourceDescriptionResolver(ModelKeys.STACK))
                .addParameter(TRANSPORT) // Deprecated
                .addParameter(PROTOCOLS) // Deprecated
                .setAttributeResolver(JGroupsExtension.getResourceDescriptionResolver("stack.add"))
                .build();

        // Transform deprecated ADD parameters into individual add operations
        OperationStepHandler addHandler = new StackAddHandler() {
            @Override
            protected void populateModel(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
                PathAddress address = Operations.getPathAddress(operation);
                ModelNode transport = null;
                if (operation.hasDefined(TRANSPORT.getName())) {
                    transport = operation.remove(TRANSPORT.getName());
                }
                List<ModelNode> protocols = Collections.emptyList();
                if (operation.hasDefined(PROTOCOLS.getName())) {
                    protocols = operation.remove(PROTOCOLS.getName()).asList();
                }

                if (transport != null) {
                    String type = ProtocolResourceDefinition.TYPE.resolveModelAttribute(context, transport).asString();
                    PathAddress transportAddress = address.append(TransportResourceDefinition.pathElement(type));
                    ModelNode transportOperation = Util.createAddOperation(transportAddress);
                    for (AttributeDefinition attribute : TransportResourceDefinition.ATTRIBUTES) {
                        String name = attribute.getName();
                        if (transport.hasDefined(name)) {
                            transportOperation.get(name).set(transport.get(name));
                        }
                    }
                    context.addStep(transportOperation, new ReloadRequiredAddStepHandler(TransportResourceDefinition.ATTRIBUTES), OperationContext.Stage.MODEL);

                    if (transport.hasDefined(ProtocolResourceDefinition.PROPERTIES.getName())) {
                        for (Property property : operation.get(ProtocolResourceDefinition.PROPERTIES.getName()).asPropertyList()) {
                            ModelNode propertyOperation = Util.createAddOperation(transportAddress.append(property.getName()));
                            propertyOperation.set(PropertyResourceDefinition.VALUE.getName()).set(property.getValue());
                            context.addStep(propertyOperation, new ReloadRequiredAddStepHandler(PropertyResourceDefinition.VALUE), OperationContext.Stage.MODEL);
                        }
                    }
                }
                if (!protocols.isEmpty()) {
                    for (ModelNode protocol : protocols) {
                        String type = ProtocolResourceDefinition.TYPE.resolveModelAttribute(context, protocol).asString();
                        PathAddress protocolAddress = address.append(ProtocolResourceDefinition.pathElement(type));
                        ModelNode protocolOperation = Util.createAddOperation(protocolAddress);
                        for (AttributeDefinition attribute : ProtocolResourceDefinition.ATTRIBUTES) {
                            String name = attribute.getName();
                            if (protocol.hasDefined(name)) {
                                protocolOperation.get(name).set(protocol.get(name));
                            }
                        }
                        context.addStep(protocolOperation, new ReloadRequiredAddStepHandler(ProtocolResourceDefinition.ATTRIBUTES), OperationContext.Stage.MODEL);

                        if (protocol.hasDefined(ProtocolResourceDefinition.PROPERTIES.getName())) {
                            for (Property property : operation.get(ProtocolResourceDefinition.PROPERTIES.getName()).asPropertyList()) {
                                ModelNode propertyOperation = Util.createAddOperation(protocolAddress.append(property.getName()));
                                propertyOperation.set(PropertyResourceDefinition.VALUE.getName()).set(property.getValue());
                                context.addStep(propertyOperation, new ReloadRequiredAddStepHandler(PropertyResourceDefinition.VALUE), OperationContext.Stage.MODEL);
                            }
                        }
                    }
                }
            }
        };
        registration.registerOperationHandler(addOperation, addHandler);

        // Transform legacy /subsystem=jgroups/stack=*:add-protocol() operation -> /subsystem=jgroups/stack=*/protocol=*:add()
        OperationStepHandler legacyAddProtocolHandler = new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) {
                PathAddress address = Operations.getPathAddress(operation);
                String protocol = operation.require(ProtocolResourceDefinition.TYPE.getName()).asString();
                PathAddress protocolAddress = address.append(ProtocolResourceDefinition.pathElement(protocol));
                ModelNode protocolOperation = Util.createAddOperation(protocolAddress);
                for (AttributeDefinition attribute : ProtocolResourceDefinition.ATTRIBUTES) {
                    String name = attribute.getName();
                    if (operation.hasDefined(name)) {
                        protocolOperation.get(name).set(operation.get(name));
                    }
                }
                context.addStep(protocolOperation, new ReloadRequiredAddStepHandler(ProtocolResourceDefinition.ATTRIBUTES), OperationContext.Stage.MODEL);
                if (operation.hasDefined(ProtocolResourceDefinition.PROPERTIES.getName())) {
                    for (Property property : operation.get(ProtocolResourceDefinition.PROPERTIES.getName()).asPropertyList()) {
                        ModelNode addPropertyOperation = Util.createAddOperation(protocolAddress.append(PropertyResourceDefinition.pathElement(property.getName())));
                        addPropertyOperation.get(PropertyResourceDefinition.VALUE.getName()).set(property.getValue());
                        context.addStep(addPropertyOperation, new ReloadRequiredAddStepHandler(PropertyResourceDefinition.VALUE), OperationContext.Stage.MODEL);
                    }
                }
            }
        };
        registration.registerOperationHandler(ADD_PROTOCOL, legacyAddProtocolHandler);

        // Transform legacy /subsystem=jgroups/stack=*:remove-protocol() operation -> /subsystem=jgroups/stack=*/protocol=*:remove()
        OperationStepHandler legacyRemoveProtocolHandler = new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) {
                PathAddress address = Operations.getPathAddress(operation);
                String protocol = operation.require(ProtocolResourceDefinition.TYPE.getName()).asString();
                PathAddress protocolAddress = address.append(ProtocolResourceDefinition.pathElement(protocol));
                ModelNode removeOperation = Util.createRemoveOperation(protocolAddress);
                context.addStep(removeOperation, ReloadRequiredRemoveStepHandler.INSTANCE, context.getCurrentStage());
            }
        };
        registration.registerOperationHandler(REMOVE_PROTOCOL, legacyRemoveProtocolHandler);

        // register export-native-configuration
        if (this.allowRuntimeOnlyRegistration) {
            OperationDefinition exportOperation = new SimpleOperationDefinitionBuilder(ModelKeys.EXPORT_NATIVE_CONFIGURATION, JGroupsExtension.getResourceDescriptionResolver("stack")).setReplyType(ModelType.STRING).build();
            registration.registerOperationHandler(exportOperation, new ExportNativeConfiguration());
        }
    }

    @Override
    public void registerChildren(ManagementResourceRegistration registration) {
        registration.registerSubModel(new TransportResourceDefinition());
        registration.registerSubModel(new ProtocolResourceDefinition());
        registration.registerSubModel(new RelayResourceDefinition());
    }
}

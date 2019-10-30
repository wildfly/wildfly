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

import java.util.ArrayList;
import java.util.function.UnaryOperator;

import org.jboss.as.clustering.controller.ChildResourceDefinition;
import org.jboss.as.clustering.controller.Operations;
import org.jboss.as.clustering.controller.SimpleAttribute;
import org.jboss.as.clustering.controller.transform.PathAddressTransformer;
import org.jboss.as.clustering.controller.transform.SimpleAddOperationTransformer;
import org.jboss.as.clustering.controller.transform.SimpleDescribeOperationTransformer;
import org.jboss.as.clustering.controller.transform.SimpleReadAttributeOperationTransformer;
import org.jboss.as.clustering.controller.transform.SimpleRemoveOperationTransformer;
import org.jboss.as.clustering.controller.transform.SimpleResourceTransformer;
import org.jboss.as.clustering.controller.transform.SimpleUndefineAttributeOperationTransformer;
import org.jboss.as.clustering.controller.transform.SimpleWriteAttributeOperationTransformer;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.logging.ControllerLogger;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.operations.global.MapOperations;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.controller.registry.PlaceholderResource;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Resource description for the addressable resources:
 *
 *   /subsystem=jgroups/stack=X/transport=TRANSPORT/property=Z
 *   /subsystem=jgroups/stack=X/protocol=Y/property=Z
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
@Deprecated
public class PropertyResourceDefinition extends ChildResourceDefinition<ManagementResourceRegistration> {

    static final PathElement WILDCARD_PATH = pathElement(PathElement.WILDCARD_VALUE);

    static PathElement pathElement(String name) {
        return PathElement.pathElement(ModelDescriptionConstants.PROPERTY, name);
    }

    static final SimpleAttributeDefinition VALUE = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.VALUE, ModelType.STRING, false)
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    static void buildTransformation(ModelVersion version, ResourceTransformationDescriptionBuilder parent) {
        ResourceTransformationDescriptionBuilder builder = parent.addChildResource(WILDCARD_PATH);

        if (JGroupsModel.VERSION_3_0_0.requiresTransformation(version)) {
            builder.setCustomResourceTransformer(new SimpleResourceTransformer(LEGACY_ADDRESS_TRANSFORMER));
            builder.addOperationTransformationOverride(ModelDescriptionConstants.ADD).setCustomOperationTransformer(new SimpleAddOperationTransformer(LEGACY_ADDRESS_TRANSFORMER).addAttributes(new SimpleAttribute(VALUE))).inheritResourceAttributeDefinitions();
            builder.addOperationTransformationOverride(ModelDescriptionConstants.REMOVE).setCustomOperationTransformer(new SimpleRemoveOperationTransformer(LEGACY_ADDRESS_TRANSFORMER));
            builder.addOperationTransformationOverride(ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION).setCustomOperationTransformer(new SimpleReadAttributeOperationTransformer(LEGACY_ADDRESS_TRANSFORMER));
            builder.addOperationTransformationOverride(ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION).setCustomOperationTransformer(new SimpleWriteAttributeOperationTransformer(LEGACY_ADDRESS_TRANSFORMER));
            builder.addOperationTransformationOverride(ModelDescriptionConstants.UNDEFINE_ATTRIBUTE_OPERATION).setCustomOperationTransformer(new SimpleUndefineAttributeOperationTransformer(LEGACY_ADDRESS_TRANSFORMER));
            builder.addOperationTransformationOverride(ModelDescriptionConstants.DESCRIBE).setCustomOperationTransformer(new SimpleDescribeOperationTransformer(LEGACY_ADDRESS_TRANSFORMER));
        }
    }

    // Transform /subsystem=jgroups/stack=*/transport=*/property=* -> /subsystem=jgroups/stack=*/transport=TRANSPORT/property=*
    static final PathAddressTransformer LEGACY_ADDRESS_TRANSFORMER = new PathAddressTransformer() {
        @Override
        public PathAddress transform(PathAddress address) {
            PathAddress parentAddress = address.subAddress(0, address.size() - 1);
            return parentAddress.getLastElement().getKey().equals(TransportResourceDefinition.WILDCARD_PATH.getKey()) ? TransportResourceDefinition.LEGACY_ADDRESS_TRANSFORMER.transform(parentAddress).append(address.getLastElement()) : address;
        }
    };

    private static final UnaryOperator<OperationStepHandler> LEGACY_PROTOCOL_OPERATION_TRANSFORMATION = new UnaryOperator<OperationStepHandler>() {
        @Override
        public OperationStepHandler apply(OperationStepHandler handler) {
            return new OperationStepHandler() {
                @Override
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                    PathAddress address = context.getCurrentAddress();
                    PathAddress protocolAddress = address.getParent();
                    PathAddress parentAddress = protocolAddress.getParent();
                    PathElement protocolPath = protocolAddress.getLastElement();
                    String protocolName = protocolPath.getValue();
                    PathElement legacyProtocolPath = GenericProtocolResourceDefinition.pathElement(protocolName);
                    ImmutableManagementResourceRegistration protocolRegistration = context.getResourceRegistration().getParent().getParent().getSubModel(PathAddress.pathAddress(legacyProtocolPath));
                    // If legacy protocol is registered, transform this operation using the property resource of the legacy protocol as the target resource
                    if ((protocolRegistration != null) && !protocolRegistration.getPathAddress().getLastElement().isWildcard()) {
                        PathAddress legacyProtocolAddress = parentAddress.append(legacyProtocolPath);
                        Resource parent = context.readResourceFromRoot(parentAddress, false);
                        // If legacy protocol resource does not exist, we need to delete the protocol and re-add the legacy protocol
                        if (!parent.hasChild(legacyProtocolPath)) {
                            // Determine index of protocol
                            int index = new ArrayList<>(parent.getChildrenNames(protocolPath.getKey())).indexOf(protocolName);
                            ModelNode addOperation = protocolRegistration.isOrderedChildResource() ? Operations.createAddOperation(legacyProtocolAddress, index) : Util.createAddOperation(legacyProtocolAddress);
                            // Remove first, then add
                            context.addStep(addOperation, context.getRootResourceRegistration().getOperationHandler(legacyProtocolAddress, ModelDescriptionConstants.ADD), OperationContext.Stage.MODEL, true);
                            context.addStep(Util.createRemoveOperation(protocolAddress), context.getRootResourceRegistration().getOperationHandler(protocolAddress, ModelDescriptionConstants.REMOVE), OperationContext.Stage.MODEL, true);
                        }
                        PathAddress legacyAddress = legacyProtocolAddress.append(address.getLastElement());
                        Operations.setPathAddress(operation, legacyAddress);
                        String operationName = Operations.getName(operation);
                        context.addStep(operation, context.getRootResourceRegistration().getOperationHandler(legacyAddress, operationName), OperationContext.Stage.MODEL);
                    } else {
                        handler.execute(context, operation);
                    }
                }
            };
        }
    };

    PropertyResourceDefinition() {
        super(WILDCARD_PATH, JGroupsExtension.SUBSYSTEM_RESOLVER.createChildResolver(WILDCARD_PATH));
        this.setDeprecated(JGroupsModel.VERSION_3_0_0.getVersion());
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent) {
        ManagementResourceRegistration registration = parent.registerSubModel(this);

        // Delegate add of property to "properties" attribute of parent protocol
        AbstractAddStepHandler addHandler = new AbstractAddStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) {
                operationDeprecated(context, operation);
                context.addResource(PathAddress.EMPTY_ADDRESS, PlaceholderResource.INSTANCE);
                String name = context.getCurrentAddressValue();
                String value = operation.get(VALUE.getName()).asString();
                PathAddress protocolAddress = context.getCurrentAddress().getParent();
                ModelNode putOperation = Operations.createMapPutOperation(protocolAddress, AbstractProtocolResourceDefinition.Attribute.PROPERTIES, name, value);
                context.addStep(putOperation, MapOperations.MAP_PUT_HANDLER, context.getCurrentStage());
            }
        };
        registration.registerOperationHandler(new SimpleOperationDefinitionBuilder(ModelDescriptionConstants.ADD, this.getResourceDescriptionResolver()).addParameter(VALUE).withFlag(OperationEntry.Flag.RESTART_NONE).build(), LEGACY_PROTOCOL_OPERATION_TRANSFORMATION.apply(addHandler));

        // Delegate remove of property to "properties" attribute of parent protocol
        AbstractRemoveStepHandler removeHandler = new AbstractRemoveStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) {
                operationDeprecated(context, operation);
                context.removeResource(PathAddress.EMPTY_ADDRESS);
                String name = context.getCurrentAddressValue();
                PathAddress protocolAddress = context.getCurrentAddress().getParent();
                ModelNode putOperation = Operations.createMapRemoveOperation(protocolAddress, AbstractProtocolResourceDefinition.Attribute.PROPERTIES, name);
                context.addStep(putOperation, MapOperations.MAP_REMOVE_HANDLER, context.getCurrentStage());
            }
        };
        registration.registerOperationHandler(new SimpleOperationDefinitionBuilder(ModelDescriptionConstants.REMOVE, this.getResourceDescriptionResolver()).withFlag(OperationEntry.Flag.RESTART_RESOURCE_SERVICES).build(), LEGACY_PROTOCOL_OPERATION_TRANSFORMATION.apply(removeHandler));

        // Delegate read of property value to "properties" attribute of parent protocol
        OperationStepHandler readHandler = new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) {
                operationDeprecated(context, operation);
                PathAddress protocolAddress = context.getCurrentAddress().getParent();
                String key = context.getCurrentAddressValue();
                ModelNode getOperation = Operations.createMapGetOperation(protocolAddress, AbstractProtocolResourceDefinition.Attribute.PROPERTIES, key);
                context.addStep(getOperation, MapOperations.MAP_GET_HANDLER, context.getCurrentStage());
            }
        };
        // Delegate write of property value to "properties" attribute of parent protocol
        OperationStepHandler writeHandler = new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) {
                operationDeprecated(context, operation);
                PathAddress protocolAddress = context.getCurrentAddress().getParent();
                String key = context.getCurrentAddressValue();
                String value = Operations.getAttributeValue(operation).asString();
                ModelNode putOperation = Operations.createMapPutOperation(protocolAddress, AbstractProtocolResourceDefinition.Attribute.PROPERTIES, key, value);
                context.addStep(putOperation, MapOperations.MAP_PUT_HANDLER, context.getCurrentStage());
            }
        };
        registration.registerReadWriteAttribute(VALUE, LEGACY_PROTOCOL_OPERATION_TRANSFORMATION.apply(readHandler), LEGACY_PROTOCOL_OPERATION_TRANSFORMATION.apply(writeHandler));

        return registration;
    }

    static void operationDeprecated(OperationContext context, ModelNode operation) {
        ControllerLogger.DEPRECATED_LOGGER.operationDeprecated(Operations.getName(operation), context.getCurrentAddress().toCLIStyleString());
    }
}

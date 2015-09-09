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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;

import java.util.HashMap;
import java.util.Map;

import org.jboss.as.clustering.controller.ChildResourceDefinition;
import org.jboss.as.clustering.controller.Operations;
import org.jboss.as.clustering.controller.SimpleAttribute;
import org.jboss.as.clustering.controller.transform.OperationTransformer;
import org.jboss.as.clustering.controller.transform.PathAddressTransformer;
import org.jboss.as.clustering.controller.transform.InitialAttributeValueOperationContextAttachment;
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
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.operations.global.MapOperations;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.transform.OperationResultTransformer;
import org.jboss.as.controller.transform.ResourceTransformationContext;
import org.jboss.as.controller.transform.ResourceTransformer;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;

/**
 * Resource description for the addressable resources:
 *
 *   /subsystem=jgroups/stack=X/transport=TRANSPORT/property=Z
 *   /subsystem=jgroups/stack=X/protocol=Y/property=Z
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
@Deprecated
public class PropertyResourceDefinition extends ChildResourceDefinition {

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

    PropertyResourceDefinition() {
        super(WILDCARD_PATH, new JGroupsResourceDescriptionResolver(WILDCARD_PATH));
        this.setDeprecated(JGroupsModel.VERSION_3_0_0.getVersion());
    }

    @Override
    public void register(ManagementResourceRegistration parentRegistration) {
        ManagementResourceRegistration registration = parentRegistration.registerSubModel(this);

        // Delegate add of property to "properties" attribute of parent protocol
        AbstractAddStepHandler addHandler = new AbstractAddStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) {
                this.createResource(context);
                String name = context.getCurrentAddressValue();
                String value = operation.get(VALUE.getName()).asString();
                PathAddress storeAddress = context.getCurrentAddress().getParent();
                ModelNode putOperation = Operations.createMapPutOperation(storeAddress, ProtocolResourceDefinition.Attribute.PROPERTIES, name, value);
                context.addStep(putOperation, MapOperations.MAP_PUT_HANDLER, context.getCurrentStage());
            }
        };
        this.registerAddOperation(registration, addHandler);

        // Delegate remove of property to "properties" attribute of parent protocol
        AbstractRemoveStepHandler removeHandler = new AbstractRemoveStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) {
                context.removeResource(PathAddress.EMPTY_ADDRESS);
                String name = context.getCurrentAddressValue();
                PathAddress storeAddress = context.getCurrentAddress().getParent();
                ModelNode putOperation = Operations.createMapRemoveOperation(storeAddress, ProtocolResourceDefinition.Attribute.PROPERTIES, name);
                context.addStep(putOperation, MapOperations.MAP_REMOVE_HANDLER, context.getCurrentStage());
            }
        };
        this.registerRemoveOperation(registration, removeHandler);

        // Delegate read of property value to "properties" attribute of parent protocol
        OperationStepHandler readHandler = new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) {
                PathAddress address = context.getCurrentAddress().getParent();
                String key = context.getCurrentAddressValue();
                ModelNode getOperation = Operations.createMapGetOperation(address, ProtocolResourceDefinition.Attribute.PROPERTIES, key);
                context.addStep(getOperation, MapOperations.MAP_GET_HANDLER, context.getCurrentStage());
            }
        };
        // Delegate write of property value to "properties" attribute of parent protocol
        OperationStepHandler writeHandler = new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) {
                PathAddress address = context.getCurrentAddress().getParent();
                String key = context.getCurrentAddressValue();
                String value = Operations.getAttributeValue(operation).asString();
                ModelNode putOperation = Operations.createMapPutOperation(address, ProtocolResourceDefinition.Attribute.PROPERTIES, key, value);
                context.addStep(putOperation, MapOperations.MAP_PUT_HANDLER, context.getCurrentStage());
            }
        };
        registration.registerReadWriteAttribute(VALUE, readHandler, writeHandler);
    }

    static ResourceTransformer PROPERTIES_RESOURCE_TRANSFORMER = new ResourceTransformer() {
        @Override
        public void transformResource(ResourceTransformationContext context, PathAddress address, Resource resource) throws OperationFailedException {
            final ModelNode model = resource.getModel();
            final ModelNode properties = model.remove(ProtocolResourceDefinition.Attribute.PROPERTIES.getDefinition().getName());
            final ResourceTransformationContext childContext = context.addTransformedResourceFromRoot(address, resource);

            if (properties.isDefined()) {
                for (final Property property : properties.asPropertyList()) {
                    String key = property.getName();
                    ModelNode value = property.getValue();
                    Resource propertyResource = Resource.Factory.create();
                    propertyResource.getModel().get(VALUE.getName()).set(value);
                    PathAddress absoluteAddress = address.append(pathElement(key).getKey(), pathElement(key).getValue());
                    childContext.addTransformedResourceFromRoot(absoluteAddress, propertyResource);
                }
            }

            context.processChildren(resource);
        }
    };

    static OperationTransformer PROPERTIES_ADD_OP_TRANSFORMER = new OperationTransformer()  {
        @Override
        public ModelNode transformOperation(ModelNode operation) {
            if (operation.hasDefined(ProtocolResourceDefinition.Attribute.PROPERTIES.getDefinition().getName())) {
                final ModelNode addOp = operation.clone();
                final ModelNode properties = addOp.remove(ProtocolResourceDefinition.Attribute.PROPERTIES.getDefinition().getName());

                final ModelNode composite = new ModelNode();
                composite.get(OP).set(COMPOSITE);
                composite.get(OP_ADDR).setEmptyList();
                composite.get(STEPS).add(addOp);

                // Handle odd legacy case, when :add operation for the protocol is :add-protocol on the parent
                PathAddress propertyAddress = Operations.getName(addOp).equals("add-protocol") ? Operations.getPathAddress(addOp).append("protocol", addOp.get("type").asString()) : Operations.getPathAddress(addOp);

                for (final Property property : properties.asPropertyList()) {
                    String key = property.getName();
                    ModelNode value = property.getValue();
                    ModelNode propAddOp = Util.createAddOperation(propertyAddress.append(pathElement(key)));
                    propAddOp.get(VALUE.getName()).set(value);
                    composite.get(STEPS).add(propAddOp);
                }
                return composite;
            }
            return operation;
        }
    };

    static org.jboss.as.controller.transform.OperationTransformer PROPERTIES_OP_TRANSFORMER = new org.jboss.as.controller.transform.OperationTransformer() {

        @Override
        public TransformedOperation transformOperation(TransformationContext context, PathAddress address, ModelNode operation) throws OperationFailedException {
            if (operation.get(ModelDescriptionConstants.NAME).asString().equals(ProtocolResourceDefinition.Attribute.PROPERTIES.getDefinition().getName())) {

                InitialAttributeValueOperationContextAttachment attachment = context.getAttachment(InitialAttributeValueOperationContextAttachment.INITIAL_VALUES_ATTACHMENT);
                assert attachment != null;

                ModelNode initialValue = attachment.getInitialValue(address, Operations.getAttributeName(operation));
                ModelNode newValue = context.readResourceFromRoot(address).getModel().get(ProtocolResourceDefinition.Attribute.PROPERTIES.getDefinition().getName()).clone();

                if (initialValue.equals(newValue) || (initialValue.isDefined() && initialValue.asPropertyList().isEmpty() && !newValue.isDefined())) {
                    // There is nothing to do, discard this operation
                    return new TransformedOperation(null, DEFAULT_REJECTION_POLICY, SUCCESSFUL_RESULT);
                }

                final Map<String, ModelNode> oldMap = new HashMap<>();
                if (initialValue.isDefined()) {
                    for (Property property : initialValue.asPropertyList()) {
                        oldMap.put(property.getName(), property.getValue());
                    }
                }

                // Transformed address for all operations
                final PathAddress legacyAddress = Operations.getPathAddress(operation);

                // This may result as multiple operations on the legacy node
                final ModelNode composite = new ModelNode();
                composite.get(OP).set(COMPOSITE);
                composite.get(OP_ADDR).setEmptyList();

                if (newValue.isDefined()) {
                    for (Property property : newValue.asPropertyList()) {
                        String key = property.getName();
                        ModelNode value = property.getValue();

                        if (!oldMap.containsKey(key)) {
                            // This is a newly added property => :add operation
                            ModelNode addOp = Util.createAddOperation(legacyAddress.append(pathElement(key)));
                            addOp.get(VALUE.getName()).set(value);
                            composite.get(STEPS).add(addOp);
                        } else {
                            final ModelNode oldPropValue = oldMap.get(key);
                            if (!oldPropValue.equals(value)) {
                                // Property value is different => :write-attribute operation
                                ModelNode writeOp = Util.getWriteAttributeOperation(legacyAddress.append(pathElement(key)),
                                        VALUE.getName(), value);
                                composite.get(STEPS).add(writeOp);
                            }
                            // Otherwise both property name and value are the same => no operation

                            // Remove this key
                            oldMap.remove(key);
                        }
                    }
                }

                // Properties that were removed = :remove operation
                for (Map.Entry<String, ModelNode> prop : oldMap.entrySet()) {
                    ModelNode removeOperation = Util.createRemoveOperation(legacyAddress.append(pathElement(prop.getKey())));
                    composite.get(STEPS).add(removeOperation);
                }

                initialValue.set(newValue.clone());

                return new TransformedOperation(composite, OperationResultTransformer.ORIGINAL_RESULT);
            }
            return new TransformedOperation(operation, OperationResultTransformer.ORIGINAL_RESULT);
        }
    };
}

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

import org.jboss.as.clustering.controller.AttributeMarshallers;
import org.jboss.as.clustering.controller.Operations;
import org.jboss.as.clustering.controller.ReloadRequiredAddStepHandler;
import org.jboss.as.clustering.controller.transform.OperationTransformer;
import org.jboss.as.clustering.controller.transform.SimpleOperationTransformer;
import org.jboss.as.clustering.controller.validation.ModuleIdentifierValidator;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleMapAttributeDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
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
import org.jboss.as.controller.transform.TransformerOperationAttachment;
import org.jboss.as.controller.transform.description.AttributeConverter;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;
import org.wildfly.clustering.jgroups.spi.ProtocolConfiguration;

/**
 * Resource description for /subsystem=jgroups/stack=X/protocol=Y
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class ProtocolResourceDefinition extends SimpleResourceDefinition {

    static final PathElement WILDCARD_PATH = pathElement(PathElement.WILDCARD_VALUE);

    static PathElement pathElement(String name) {
        return PathElement.pathElement(ModelKeys.PROTOCOL, name);
    }

    @Deprecated
    static final SimpleAttributeDefinition TYPE = new SimpleAttributeDefinitionBuilder(ModelKeys.TYPE, ModelType.STRING, true)
            .setXmlName(Attribute.TYPE.getLocalName())
            .setAllowExpression(false)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setDeprecated(JGroupsModel.VERSION_3_0_0.getVersion())
            .build();

    static final SimpleAttributeDefinition SOCKET_BINDING = new SimpleAttributeDefinitionBuilder(ModelKeys.SOCKET_BINDING, ModelType.STRING, true)
            .setXmlName(Attribute.SOCKET_BINDING.getLocalName())
            .setAllowExpression(false)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.SOCKET_BINDING_REF)
            .build();

    static final SimpleAttributeDefinition MODULE = new SimpleAttributeDefinitionBuilder(ModelKeys.MODULE, ModelType.STRING, true)
            .setXmlName(Attribute.MODULE.getLocalName())
            .setDefaultValue(new ModelNode(ProtocolConfiguration.DEFAULT_MODULE.getName()))
            .setAllowExpression(false)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setValidator(new ModuleIdentifierValidator(true))
            .build();

    static final SimpleMapAttributeDefinition PROPERTIES = new SimpleMapAttributeDefinition.Builder("properties", true)
            .setAllowExpression(true)
            .setAttributeMarshaller(AttributeMarshallers.PROPERTY_LIST)
            .setDefaultValue(new ModelNode().setEmptyList())
            .build();

    static final AttributeDefinition[] ATTRIBUTES = new AttributeDefinition[] { TYPE, MODULE, SOCKET_BINDING, PROPERTIES };

    static void buildTransformation(ModelVersion version, ResourceTransformationDescriptionBuilder parent) {
        ResourceTransformationDescriptionBuilder builder = parent.addChildResource(WILDCARD_PATH);

        ProtocolResourceDefinition.addTransformations(version, builder);

        if (JGroupsModel.VERSION_3_0_0.requiresTransformation(version)) {
            // Translate /subsystem=jgroups/stack=*/protocol=*:add() -> /subsystem=jgroups/stack=*:add-protocol()
            OperationTransformer addTransformer = new OperationTransformer() {
                @SuppressWarnings("deprecation")
                @Override
                public ModelNode transformOperation(ModelNode operation) {
                    PathAddress address = Operations.getPathAddress(operation);
                    PathAddress stackAddress = address.subAddress(0, address.size() - 1);

                    ModelNode addProtocolOp = operation.clone();
                    addProtocolOp.get(ModelDescriptionConstants.OP_ADDR).set(stackAddress.toModelNode());
                    addProtocolOp.get(ModelDescriptionConstants.OP).set(ModelKeys.ADD_PROTOCOL);

                    addProtocolOp = PROPERTIES_ADD_OP_TRANSFORMER.transformOperation(addProtocolOp);

                    return addProtocolOp;
                }
            };
            builder.addOperationTransformationOverride(ModelDescriptionConstants.ADD).setCustomOperationTransformer(new SimpleOperationTransformer(addTransformer)).inheritResourceAttributeDefinitions();

            // Translate /subsystem=jgroups/stack=*/protocol=*:remove() -> /subsystem=jgroups/stack=*:remove-protocol()
            OperationTransformer removeTransformer = new OperationTransformer() {
                @SuppressWarnings("deprecation")
                @Override
                public ModelNode transformOperation(ModelNode operation) {
                    PathAddress address = Operations.getPathAddress(operation);
                    String protocol = address.getLastElement().getValue();
                    PathAddress stackAddress = address.subAddress(0, address.size() - 1);
                    ModelNode legacyOperation = Util.createOperation(ModelKeys.REMOVE_PROTOCOL, stackAddress);
                    legacyOperation.get(ProtocolResourceDefinition.TYPE.getName()).set(protocol);
                    return legacyOperation;
                }
            };
            builder.addOperationTransformationOverride(ModelDescriptionConstants.REMOVE).setCustomOperationTransformer(new SimpleOperationTransformer(removeTransformer));

            builder.setCustomResourceTransformer(PROPERTIES_RESOURCE_TRANSFORMER);
        }

        PropertyResourceDefinition.buildTransformation(version, builder);
    }

    /**
     * Builds transformations common to both protocols and transport.
     */
    static void addTransformations(ModelVersion version, ResourceTransformationDescriptionBuilder builder) {

        if (JGroupsModel.VERSION_3_0_0.requiresTransformation(version)) {
            AttributeConverter typeConverter = new AttributeConverter.DefaultAttributeConverter() {
                @Override
                protected void convertAttribute(PathAddress address, String name, ModelNode value, TransformationContext context) {
                    if (!value.isDefined()) {
                        value.set(address.getLastElement().getValue());
                    }
                }
            };
            builder.getAttributeBuilder()
                    .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(MODULE.getDefaultValue()), MODULE)
                    .addRejectCheck(RejectAttributeChecker.DEFINED, MODULE)
                    .setValueConverter(typeConverter, TYPE)
                    .end();

            OperationTransformer getPropertyTransformer = new OperationTransformer() {
                @Override
                public ModelNode transformOperation(ModelNode operation) {
                    if (operation.get(ModelDescriptionConstants.NAME).asString().equals(PROPERTIES.getName())) {
                        String key = operation.get("key").asString();
                        PathAddress address = TransportResourceDefinition.LEGACY_ADDRESS_TRANSFORMER.transform(Operations.getPathAddress(operation));
                        ModelNode transformedOperation = Util.createOperation(ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION, address.append(PropertyResourceDefinition.pathElement(key)));
                        transformedOperation.get(ModelDescriptionConstants.NAME).set(PropertyResourceDefinition.VALUE.getName());
                        return transformedOperation;
                    }
                    return operation;
                }
            };
            builder.addRawOperationTransformationOverride(MapOperations.MAP_GET_DEFINITION.getName(), new SimpleOperationTransformer(getPropertyTransformer));

            org.jboss.as.controller.transform.OperationTransformer propertiesOpTransformer = new org.jboss.as.controller.transform.OperationTransformer() {
                @Override
                @SuppressWarnings("deprecation")
                public TransformedOperation transformOperation(TransformationContext context, PathAddress address, ModelNode operation) throws OperationFailedException {
                    if (operation.get(ModelDescriptionConstants.NAME).asString().equals(PROPERTIES.getName())) {

                        PropertiesAttachment attachment = context.getAttachment(PropertiesAttachment.KEY);
                        ModelNode oldValue = attachment.getOldValue();
                        ModelNode newValue = context.readResourceFromRoot(address).getModel().get(PROPERTIES.getName());

                        if (oldValue.equals(newValue) || (oldValue.isDefined() && oldValue.asPropertyList().isEmpty() && !newValue.isDefined())) {
                            // There is nothing to do, discard this operation
                            return new TransformedOperation(null, DEFAULT_REJECTION_POLICY, SUCCESSFUL_RESULT);
                        }

                        final Map<String, ModelNode> oldMap = new HashMap<>();
                        if (oldValue.isDefined()) {
                            for (Property property : oldValue.asPropertyList()) {
                                oldMap.put(property.getName(), property.getValue());
                            }
                        }

                        // Transformed address for all operations
                        final PathAddress legacyAddress = TransportResourceDefinition.LEGACY_ADDRESS_TRANSFORMER.transform(Operations.getPathAddress(operation));

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
                                    ModelNode addOp = Util.createAddOperation(legacyAddress.append(PropertyResourceDefinition.pathElement(key)));
                                    addOp.get(PropertyResourceDefinition.VALUE.getName()).set(value);
                                    composite.get(STEPS).add(addOp);
                                } else {
                                    final ModelNode oldPropValue = oldMap.get(key);
                                    if (!oldPropValue.equals(value)) {
                                        // Property value is different => :write-attribute operation
                                        ModelNode writeOp = Util.getWriteAttributeOperation(legacyAddress.append(PropertyResourceDefinition.pathElement(key)),
                                                PropertyResourceDefinition.VALUE.getName(), value);
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
                            ModelNode removeOperation = Util.createRemoveOperation(legacyAddress.append(PropertyResourceDefinition.pathElement(prop.getKey())));
                            composite.get(STEPS).add(removeOperation);
                        }

                        return new TransformedOperation(composite, OperationResultTransformer.ORIGINAL_RESULT);
                    }
                    return new TransformedOperation(operation, OperationResultTransformer.ORIGINAL_RESULT);
                }
            };


            // map-based write ops
            builder
                    .addRawOperationTransformationOverride(MapOperations.MAP_PUT_DEFINITION.getName(), propertiesOpTransformer)
                    .addRawOperationTransformationOverride(MapOperations.MAP_REMOVE_DEFINITION.getName(), propertiesOpTransformer)
                    .addRawOperationTransformationOverride(MapOperations.MAP_CLEAR_DEFINITION.getName(), propertiesOpTransformer);
            // attribute-based write ops
            builder
                    .addRawOperationTransformationOverride(ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION, propertiesOpTransformer)
                    .addRawOperationTransformationOverride(ModelDescriptionConstants.UNDEFINE_ATTRIBUTE_OPERATION, propertiesOpTransformer);
        }
    }

    ProtocolResourceDefinition() {
        this(new ReloadRequiredAddStepHandler(ATTRIBUTES) {
            @Override
            protected ResourceCreator getResourceCreator() {
                //Set this up as an ordered child, which should have indexed adds
                return new OrderedResourceCreator(true);
            }
        });
    }

    ProtocolResourceDefinition(OperationStepHandler addHandler) {
        super(WILDCARD_PATH, new JGroupsResourceDescriptionResolver(ModelKeys.PROTOCOL), addHandler, new ReloadRequiredRemoveStepHandler());
    }

    @Override
    protected boolean isOrderedChildResource() {
        //Set this up as an ordered child resource so that the add handler gets the add-index parameter added
        return true;
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration registration) {
        final OperationStepHandler writeHandler = new ReloadRequiredWriteAttributeHandler(ATTRIBUTES);
        for (AttributeDefinition attr : ATTRIBUTES) {
            if (attr.equals(PROPERTIES)) {
                // We need to register a special write handler that stores attaches the previous model values
                // for subsequent transformations
                registration.registerReadWriteAttribute(attr, null, new ReloadRequiredWriteAttributeHandler(attr) {
                    @Override
                    protected void finishModelStage(OperationContext context, ModelNode operation, String attributeName, ModelNode newValue, ModelNode oldValue, Resource model) throws OperationFailedException {
                        super.finishModelStage(context, operation, attributeName, newValue, oldValue, model);
                        if (!context.isBooting()) {
                            TransformerOperationAttachment attachment = TransformerOperationAttachment.getOrCreate(context);
                            attachment.attachIfAbsent(PropertiesAttachment.KEY, new PropertiesAttachment(oldValue.clone()));
                        }
                    }
                });
            } else {
                registration.registerReadWriteAttribute(attr, null, writeHandler);
            }
        }
    }

    @Override
    public void registerChildren(ManagementResourceRegistration registration) {
        registration.registerSubModel(new PropertyResourceDefinition());
    }

    static class PropertiesAttachment {
        public static final OperationContext.AttachmentKey<PropertiesAttachment> KEY = OperationContext.AttachmentKey.create(PropertiesAttachment.class);

        public volatile ModelNode oldValue;

        public PropertiesAttachment(ModelNode oldValue) {
            this.oldValue = oldValue;
        }

        public ModelNode getOldValue() {
            return oldValue;
        }
    }

    static ResourceTransformer PROPERTIES_RESOURCE_TRANSFORMER = new ResourceTransformer() {
        @Override
        public void transformResource(ResourceTransformationContext context, PathAddress address, Resource resource) throws OperationFailedException {
            final ModelNode model = resource.getModel();
            final ModelNode properties = model.remove(PROPERTIES.getName());
            final ResourceTransformationContext childContext = context.addTransformedResourceFromRoot(address, resource);

            if (properties.isDefined()) {
                for (final Property property : properties.asPropertyList()) {
                    String key = property.getName();
                    ModelNode value = property.getValue();
                    Resource propertyResource = Resource.Factory.create();
                    propertyResource.getModel().get(PropertyResourceDefinition.VALUE.getName()).set(value);
                    PathAddress absoluteAddress = address.append(PropertyResourceDefinition.pathElement(key).getKey(), PropertyResourceDefinition.pathElement(key).getValue());
                    childContext.addTransformedResourceFromRoot(absoluteAddress, propertyResource);
                }
            }

            context.processChildren(resource);
        }
    };

    static OperationTransformer PROPERTIES_ADD_OP_TRANSFORMER = new OperationTransformer()  {
        @Override
        public ModelNode transformOperation(ModelNode operation) {
            if (operation.hasDefined(PROPERTIES.getName())) {
                final ModelNode addOp = operation.clone();
                final ModelNode properties = addOp.remove(PROPERTIES.getName());

                final ModelNode composite = new ModelNode();
                composite.get(OP).set(COMPOSITE);
                composite.get(OP_ADDR).setEmptyList();
                composite.get(STEPS).add(addOp);

                // Handle odd legacy case, when :add operation for the protocol is :add-protocol on the parent
                PathAddress propertyAddress = Operations.getName(addOp).equals(ModelKeys.ADD_PROTOCOL)
                        ? Operations.getPathAddress(addOp).append(ModelKeys.PROTOCOL, addOp.get(ModelKeys.TYPE).asString())
                        : Operations.getPathAddress(addOp);

                for (final Property property : properties.asPropertyList()) {
                    String key = property.getName();
                    ModelNode value = property.getValue();
                    ModelNode propAddOp = Util.createAddOperation(propertyAddress.append(PropertyResourceDefinition.pathElement(key)));
                    propAddOp.get(PropertyResourceDefinition.VALUE.getName()).set(value);
                    composite.get(STEPS).add(propAddOp);
                }
                return composite;
            }
            return operation;
        }
    };
}

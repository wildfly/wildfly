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

import org.jboss.as.clustering.controller.AttributeMarshallers;
import org.jboss.as.clustering.controller.Operations;
import org.jboss.as.clustering.controller.ReloadRequiredAddStepHandler;
import org.jboss.as.clustering.controller.transform.OperationTransformer;
import org.jboss.as.clustering.controller.transform.SimpleAddOperationTransformer;
import org.jboss.as.clustering.controller.transform.SimpleOperationTransformer;
import org.jboss.as.clustering.controller.validation.ModuleIdentifierValidator;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
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
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.description.AttributeConverter;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
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
                    return Util.createOperation(ModelKeys.ADD_PROTOCOL, stackAddress);
                }
            };
            builder.addOperationTransformationOverride(ModelDescriptionConstants.ADD).setCustomOperationTransformer(new SimpleAddOperationTransformer(addTransformer, ATTRIBUTES)).inheritResourceAttributeDefinitions();

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
        }

        PropertyResourceDefinition.buildTransformation(version, builder);
    }

    /*
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

            OperationTransformer putPropertyTransformer = new OperationTransformer() {
                @Override
                public ModelNode transformOperation(ModelNode operation) {
                    if (operation.get(ModelDescriptionConstants.NAME).asString().equals(PROPERTIES.getName())) {
                        String key = operation.get("key").asString();
                        ModelNode value = operation.get(ModelDescriptionConstants.VALUE);
                        PathAddress address = Operations.getPathAddress(operation);
                        ModelNode transformedOperation = Util.createAddOperation(address.append(PropertyResourceDefinition.pathElement(key)));
                        transformedOperation.get(PropertyResourceDefinition.VALUE.getName()).set(value);
                        return transformedOperation;
                    }
                    return operation;
                }
            };
            builder.addRawOperationTransformationOverride(MapOperations.MAP_PUT_DEFINITION.getName(), new SimpleOperationTransformer(putPropertyTransformer));

            OperationTransformer removePropertyTransformer = new OperationTransformer() {
                @Override
                public ModelNode transformOperation(ModelNode operation) {
                    if (operation.get(ModelDescriptionConstants.NAME).asString().equals(PROPERTIES.getName())) {
                        String key = operation.get("key").asString();
                        PathAddress address = Operations.getPathAddress(operation);
                        return Util.createRemoveOperation(address.append(PropertyResourceDefinition.pathElement(key)));
                    }
                    return operation;
                }
            };
            builder.addRawOperationTransformationOverride(MapOperations.MAP_PUT_DEFINITION.getName(), new SimpleOperationTransformer(removePropertyTransformer));
        }
    }

    ProtocolResourceDefinition() {
        this(new ReloadRequiredAddStepHandler(ATTRIBUTES));
    }

    ProtocolResourceDefinition(OperationStepHandler addHandler) {
        super(WILDCARD_PATH, new JGroupsResourceDescriptionResolver(ModelKeys.PROTOCOL), addHandler, new ReloadRequiredRemoveStepHandler());
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration registration) {
        final OperationStepHandler writeHandler = new ReloadRequiredWriteAttributeHandler(ATTRIBUTES);
        for (AttributeDefinition attr : ATTRIBUTES) {
            registration.registerReadWriteAttribute(attr, null, writeHandler);
        }
    }

    @Override
    public void registerChildren(ManagementResourceRegistration registration) {
        registration.registerSubModel(new PropertyResourceDefinition());
    }
}

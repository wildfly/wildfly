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
import org.jboss.as.clustering.controller.AttributeParsers;
import org.jboss.as.clustering.controller.CapabilityReference;
import org.jboss.as.clustering.controller.ChildResourceDefinition;
import org.jboss.as.clustering.controller.Operations;
import org.jboss.as.clustering.controller.RequiredCapability;
import org.jboss.as.clustering.controller.ResourceServiceBuilderFactory;
import org.jboss.as.clustering.controller.transform.OperationTransformer;
import org.jboss.as.clustering.controller.transform.SimpleAddOperationTransformer;
import org.jboss.as.clustering.controller.transform.SimpleOperationTransformer;
import org.jboss.as.clustering.controller.validation.ModuleIdentifierValidatorBuilder;
import org.jboss.as.clustering.controller.validation.ParameterValidatorBuilder;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.CapabilityReferenceRecorder;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleMapAttributeDefinition;
import org.jboss.as.controller.access.management.AccessConstraintDefinition;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
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
import org.jboss.as.network.SocketBinding;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.clustering.jgroups.spi.ChannelFactory;
import org.wildfly.clustering.jgroups.spi.ProtocolConfiguration;

/**
 * Resource description for /subsystem=jgroups/stack=X/protocol=Y
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 * @author Paul Ferraro
 */
public abstract class ProtocolResourceDefinition extends ChildResourceDefinition {

    static final PathElement WILDCARD_PATH = pathElement(PathElement.WILDCARD_VALUE);

    static PathElement pathElement(String name) {
        return PathElement.pathElement("protocol", name);
    }

    enum Capability implements org.jboss.as.clustering.controller.Capability {
        SOCKET_BINDING("org.wildfly.clustering.protocol.socket-binding", SocketBinding.class),
        ;
        private final RuntimeCapability<Void> definition;

        Capability(String name, Class<?> serviceType) {
            this.definition = RuntimeCapability.Builder.of(name, true).setServiceType(serviceType).build();
        }

        @Override
        public RuntimeCapability<Void> getDefinition() {
            return this.definition;
        }

        @Override
        public RuntimeCapability<Void> getRuntimeCapability(PathAddress address) {
            PathAddress stackAddress = address.getParent();
            return this.definition.fromBaseCapability(stackAddress.getLastElement().getValue() + "." + address.getLastElement().getValue());
        }
    }

    enum Attribute implements org.jboss.as.clustering.controller.Attribute {
        SOCKET_BINDING(ModelDescriptionConstants.SOCKET_BINDING, ModelType.STRING, SensitiveTargetAccessConstraintDefinition.SOCKET_BINDING_REF, new CapabilityReference(RequiredCapability.SOCKET_BINDING, Capability.SOCKET_BINDING)),
        MODULE(ModelDescriptionConstants.MODULE, ModelType.STRING, new ModelNode(ProtocolConfiguration.DEFAULT_MODULE.getName()), new ModuleIdentifierValidatorBuilder()),
        PROPERTIES(ModelDescriptionConstants.PROPERTIES),
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type, ModelNode defaultValue, ParameterValidatorBuilder validatorBuilder) {
            SimpleAttributeDefinitionBuilder builder = createBuilder(name, type, defaultValue);
            this.definition = builder.setValidator(validatorBuilder.configure(builder).build()).build();
        }

        Attribute(String name, ModelType type, AccessConstraintDefinition constraint, CapabilityReferenceRecorder reference) {
            this.definition = createBuilder(name, type, null).setAccessConstraints(constraint).setCapabilityReference(reference).build();
        }

        Attribute(String name) {
            this.definition = new SimpleMapAttributeDefinition.Builder(name, true)
                    .setAllowExpression(true)
                    .setAttributeMarshaller(AttributeMarshallers.PROPERTY_LIST)
                    .setAttributeParser(AttributeParsers.COLLECTION)
                    .setDefaultValue(new ModelNode().setEmptyList())
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    @Deprecated
    enum DeprecatedAttribute implements org.jboss.as.clustering.controller.Attribute {
        TYPE(ModelDescriptionConstants.TYPE, ModelType.STRING, null, JGroupsModel.VERSION_3_0_0),
        ;
        private final AttributeDefinition definition;

        DeprecatedAttribute(String name, ModelType type, ModelNode defaultValue, JGroupsModel deprecation) {
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
        ;
    }

    @SuppressWarnings("deprecation")
    static void buildTransformation(ModelVersion version, ResourceTransformationDescriptionBuilder parent) {
        ResourceTransformationDescriptionBuilder builder = parent.addChildResource(WILDCARD_PATH);

        ProtocolResourceDefinition.addTransformations(version, builder);

        if (JGroupsModel.VERSION_3_0_0.requiresTransformation(version)) {
            // Translate /subsystem=jgroups/stack=*/protocol=*:add() -> /subsystem=jgroups/stack=*:add-protocol()
            OperationTransformer addTransformer = new OperationTransformer() {
                @Override
                public ModelNode transformOperation(ModelNode operation) {
                    PathAddress address = Operations.getPathAddress(operation);
                    PathAddress stackAddress = address.subAddress(0, address.size() - 1);
                    return Util.createOperation("add-protocol", stackAddress);
                }
            };
            builder.addOperationTransformationOverride(ModelDescriptionConstants.ADD).setCustomOperationTransformer(new SimpleAddOperationTransformer(addTransformer).addAttributes(Attribute.class)).inheritResourceAttributeDefinitions();

            // Translate /subsystem=jgroups/stack=*/protocol=*:remove() -> /subsystem=jgroups/stack=*:remove-protocol()
            OperationTransformer removeTransformer = new OperationTransformer() {
                @Override
                public ModelNode transformOperation(ModelNode operation) {
                    PathAddress address = Operations.getPathAddress(operation);
                    String protocol = address.getLastElement().getValue();
                    PathAddress stackAddress = address.subAddress(0, address.size() - 1);
                    ModelNode legacyOperation = Util.createOperation("remove-protocol", stackAddress);
                    legacyOperation.get(ProtocolResourceDefinition.DeprecatedAttribute.TYPE.getDefinition().getName()).set(protocol);
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
    @SuppressWarnings("deprecation")
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
                    .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(Attribute.MODULE.getDefinition().getDefaultValue()), Attribute.MODULE.getDefinition())
                    .addRejectCheck(RejectAttributeChecker.DEFINED, Attribute.MODULE.getDefinition())
                    .setValueConverter(typeConverter, DeprecatedAttribute.TYPE.getDefinition())
                    .end();

            OperationTransformer putPropertyTransformer = new OperationTransformer() {
                @Override
                public ModelNode transformOperation(ModelNode operation) {
                    if (operation.get(ModelDescriptionConstants.NAME).asString().equals(Attribute.PROPERTIES.getDefinition().getName())) {
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
                    if (operation.get(ModelDescriptionConstants.NAME).asString().equals(Attribute.PROPERTIES.getDefinition().getName())) {
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

    final ResourceServiceBuilderFactory<ChannelFactory> parentBuilderFactory;

    ProtocolResourceDefinition(ResourceServiceBuilderFactory<ChannelFactory> parentBuilderFactory) {
        this(new Parameters(WILDCARD_PATH, new JGroupsResourceDescriptionResolver(WILDCARD_PATH)).setOrderedChild(), parentBuilderFactory);
    }

    ProtocolResourceDefinition(Parameters parameters, ResourceServiceBuilderFactory<ChannelFactory> parentBuilderFactory) {
        super(parameters);
        this.parentBuilderFactory = parentBuilderFactory;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void register(ManagementResourceRegistration registration) {
        new PropertyResourceDefinition().register(registration);
    }
}

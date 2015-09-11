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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.UNDEFINE_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.as.clustering.controller.CapabilityReference;
import org.jboss.as.clustering.controller.Operations;
import org.jboss.as.clustering.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.clustering.controller.RequiredCapability;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceBuilderFactory;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.RestartParentResourceAddStepHandler;
import org.jboss.as.clustering.controller.RestartParentResourceRemoveStepHandler;
import org.jboss.as.clustering.controller.SimpleAliasEntry;
import org.jboss.as.clustering.controller.SimpleResourceServiceHandler;
import org.jboss.as.clustering.controller.transform.ChainedOperationTransformer;
import org.jboss.as.clustering.controller.transform.ImplicitlyAddedResourceDynamicDiscardPolicy;
import org.jboss.as.clustering.controller.transform.PathAddressTransformer;
import org.jboss.as.clustering.controller.transform.SimpleDescribeOperationTransformer;
import org.jboss.as.clustering.controller.transform.SimpleOperationTransformer;
import org.jboss.as.clustering.controller.transform.SimplePathOperationTransformer;
import org.jboss.as.clustering.controller.transform.SimpleReadAttributeOperationTransformer;
import org.jboss.as.clustering.controller.transform.SimpleRemoveOperationTransformer;
import org.jboss.as.clustering.jgroups.logging.JGroupsLogger;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.CapabilityReferenceRecorder;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.access.management.AccessConstraintDefinition;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.operations.global.MapOperations;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.transform.OperationTransformer;
import org.jboss.as.controller.transform.ResourceTransformationContext;
import org.jboss.as.controller.transform.ResourceTransformer;
import org.jboss.as.controller.transform.description.AttributeConverter.DefaultValueAttributeConverter;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.network.SocketBinding;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.clustering.jgroups.spi.ChannelFactory;

/**
 * Resource description for /subsystem=jgroups/stack=X/transport=*
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 * @author Paul Ferraro
 */
public class TransportResourceDefinition extends ProtocolResourceDefinition {

    static final PathElement LEGACY_PATH = pathElement("TRANSPORT");
    static final PathElement WILDCARD_PATH = pathElement(PathElement.WILDCARD_VALUE);

    public static PathElement pathElement(String name) {
        return PathElement.pathElement("transport", name);
    }

    enum Capability implements org.jboss.as.clustering.controller.Capability {
        DIAGNOSTICS_SOCKET_BINDING("org.wildfly.clustering.transport.diagnostics-socket-binding", SocketBinding.class),
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
        SHARED("shared", ModelType.BOOLEAN, new ModelNode(false)),
        DIAGNOSTICS_SOCKET_BINDING("diagnostics-socket-binding", ModelType.STRING, SensitiveTargetAccessConstraintDefinition.SOCKET_BINDING_REF, new CapabilityReference(RequiredCapability.SOCKET_BINDING, Capability.DIAGNOSTICS_SOCKET_BINDING)),
        SITE("site", ModelType.STRING),
        RACK("rack", ModelType.STRING),
        MACHINE("machine", ModelType.STRING),
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type) {
            this.definition = createBuilder(name, type).build();
        }

        Attribute(String name, ModelType type, ModelNode defaultValue) {
            this.definition = createBuilder(name, type).setDefaultValue(defaultValue).build();
        }

        Attribute(String name, ModelType type, AccessConstraintDefinition constraint, CapabilityReferenceRecorder reference) {
            this.definition = createBuilder(name, type).setAccessConstraints(constraint).setCapabilityReference(reference).build();
        }

        private static SimpleAttributeDefinitionBuilder createBuilder(String name, ModelType type) {
            return new SimpleAttributeDefinitionBuilder(name, type)
                    .setAllowExpression(true)
                    .setAllowNull(true)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            ;
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    @Deprecated
    enum ThreadingAttribute implements org.jboss.as.clustering.controller.Attribute {
        DEFAULT_EXECUTOR("default-executor"),
        OOB_EXECUTOR("oob-executor"),
        TIMER_EXECUTOR("timer-executor"),
        THREAD_FACTORY("thread-factory"),
        ;
        private final AttributeDefinition definition;

        ThreadingAttribute(String name) {
            this.definition = new SimpleAttributeDefinitionBuilder(name, ModelType.STRING)
                    .setAllowExpression(false)
                    .setAllowNull(true)
                    .setDeprecated(JGroupsModel.VERSION_3_0_0.getVersion())
                    .setFlags(AttributeAccess.Flag.RESTART_NONE)
                    .build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    @SuppressWarnings("deprecation")
    static void buildTransformation(ModelVersion version, ResourceTransformationDescriptionBuilder parent) {
        ResourceTransformationDescriptionBuilder builder = parent.addChildResource(WILDCARD_PATH);

        ProtocolResourceDefinition.addTransformations(version, builder);

        if (JGroupsModel.VERSION_3_0_0.requiresTransformation(version)) {
            builder.getAttributeBuilder().setValueConverter(new DefaultValueAttributeConverter(Attribute.SHARED.getDefinition()), Attribute.SHARED.getDefinition());

            builder.setCustomResourceTransformer(new ResourceTransformer() {
                @Override
                public void transformResource(ResourceTransformationContext context, PathAddress address, Resource resource) throws OperationFailedException {
                    PropertyResourceDefinition.PROPERTIES_RESOURCE_TRANSFORMER.transformResource(context, LEGACY_ADDRESS_TRANSFORMER.transform(address), resource);
                }
            });
            builder.addOperationTransformationOverride(ModelDescriptionConstants.ADD).setCustomOperationTransformer(new SimpleOperationTransformer(new org.jboss.as.clustering.controller.transform.OperationTransformer() {
                @Override
                public ModelNode transformOperation(final ModelNode operation) {
                    operation.get(ModelDescriptionConstants.OP_ADDR).set(LEGACY_ADDRESS_TRANSFORMER.transform(Operations.getPathAddress(operation)).toModelNode());
                    return PropertyResourceDefinition.PROPERTIES_ADD_OP_TRANSFORMER.transformOperation(operation);
                }
            })).inheritResourceAttributeDefinitions();
            builder.addOperationTransformationOverride(ModelDescriptionConstants.REMOVE).setCustomOperationTransformer(new SimpleRemoveOperationTransformer(LEGACY_ADDRESS_TRANSFORMER));
            builder.addOperationTransformationOverride(ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION).setCustomOperationTransformer(new SimpleReadAttributeOperationTransformer(LEGACY_ADDRESS_TRANSFORMER));
            builder.addOperationTransformationOverride(ModelDescriptionConstants.DESCRIBE).setCustomOperationTransformer(new SimpleDescribeOperationTransformer(LEGACY_ADDRESS_TRANSFORMER));

            org.jboss.as.clustering.controller.transform.OperationTransformer getPropertyTransformer = new org.jboss.as.clustering.controller.transform.OperationTransformer() {
                @Override
                public ModelNode transformOperation(ModelNode operation) {
                    if (operation.get(ModelDescriptionConstants.NAME).asString().equals(ProtocolResourceDefinition.Attribute.PROPERTIES.getDefinition().getName())) {
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

            List<OperationTransformer> transformerChain = new LinkedList<>();
            transformerChain.add(new SimplePathOperationTransformer(LEGACY_ADDRESS_TRANSFORMER));
            transformerChain.add(PropertyResourceDefinition.PROPERTIES_OP_TRANSFORMER);
            ChainedOperationTransformer chainedTransformer = new ChainedOperationTransformer(transformerChain, false);

            Set<String> writeAttributeOperations = new HashSet(MapOperations.MAP_OPERATION_NAMES);
            writeAttributeOperations.remove(MapOperations.MAP_GET_DEFINITION.getName());
            writeAttributeOperations.add(WRITE_ATTRIBUTE_OPERATION);
            writeAttributeOperations.add(UNDEFINE_ATTRIBUTE_OPERATION);
            for (String opName : writeAttributeOperations) {
                builder.addOperationTransformationOverride(opName)
                        .inheritResourceAttributeDefinitions()
                        .setCustomOperationTransformer(chainedTransformer);
            }

            // Reject thread pool configuration, discard if undefined, support EAP 6.x slaves using deprecated attributes
            builder.addChildResource(ThreadPoolResourceDefinition.WILDCARD_PATH, new ImplicitlyAddedResourceDynamicDiscardPolicy());
        } else {
            Stream.of(ThreadPoolResourceDefinition.values()).forEach(p -> p.buildTransformation(version, parent));
        }

        PropertyResourceDefinition.buildTransformation(version, builder);
    }

    // Transform /subsystem=jgroups/stack=*/transport=* -> /subsystem=jgroups/stack=*/transport=TRANSPORT
    static final PathAddressTransformer LEGACY_ADDRESS_TRANSFORMER = new PathAddressTransformer() {
        @Override
        public PathAddress transform(PathAddress address) {
            return address.subAddress(0, address.size() - 1).append(LEGACY_PATH);
        }
    };

    TransportResourceDefinition(ResourceServiceBuilderFactory<ChannelFactory> parentBuilderFactory) {
        super(new Parameters(WILDCARD_PATH, new JGroupsResourceDescriptionResolver(WILDCARD_PATH, ProtocolResourceDefinition.WILDCARD_PATH)), parentBuilderFactory);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void register(ManagementResourceRegistration parentRegistration) {
        ManagementResourceRegistration registration = parentRegistration.registerSubModel(this);
        parentRegistration.registerAlias(LEGACY_PATH, new SimpleAliasEntry(registration));

        ResourceDescriptor descriptor = new ResourceDescriptor(this.getResourceDescriptionResolver())
                .addAttributes(Attribute.class)
                .addAttributes(ProtocolResourceDefinition.Attribute.class)
                .addExtraParameters(ThreadingAttribute.class)
                .addExtraParameters(ProtocolResourceDefinition.DeprecatedAttribute.class)
                .addCapabilities(Capability.class)
                .addCapabilities(ProtocolResourceDefinition.Capability.class)
                ;
        ResourceServiceHandler handler = new SimpleResourceServiceHandler<>(new TransportConfigurationBuilderFactory());
        new RestartParentResourceAddStepHandler<ChannelFactory>(this.parentBuilderFactory, descriptor, handler) {
            @Override
            protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
                super.populateModel(operation, model);
                // Add deprecated threading attributes to the model for now
                for (ThreadingAttribute attribute :  EnumSet.allOf(ThreadingAttribute.class)) {
                    attribute.getDefinition().validateAndSet(operation, model);
                }
            }
        }.register(registration);
        new RestartParentResourceRemoveStepHandler<>(this.parentBuilderFactory, descriptor, handler).register(registration);

        new ReloadRequiredWriteAttributeHandler(() -> EnumSet.allOf(ThreadingAttribute.class).stream().map(attribute -> attribute.getDefinition()).collect(Collectors.toList())) {
            @Override
            protected void validateUpdatedModel(OperationContext context, Resource model) throws OperationFailedException {
                // Add a new step to validate instead of doing it directly in this method.
                // This allows a composite op to change both attributes and then the
                // validation occurs after both have done their work.
                context.addStep(new OperationStepHandler() {
                    @Override
                    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                        ModelNode conf = context.readResource(PathAddress.EMPTY_ADDRESS).getModel();
                        // TODO doesn't cover the admin-only modes
                        if (context.getProcessType().isServer()) {
                            for (ThreadingAttribute attribute : EnumSet.allOf(ThreadingAttribute.class)) {
                                if (conf.hasDefined(attribute.getDefinition().getName())) {
                                    // That is not supported.
                                    throw new OperationFailedException(JGroupsLogger.ROOT_LOGGER.threadsAttributesUsedInRuntime());
                                }
                            }
                        }
                    }
                }, OperationContext.Stage.MODEL);
            }
        }.register(registration);

        EnumSet.allOf(ThreadPoolResourceDefinition.class).forEach(pool -> pool.register(registration));

        super.register(registration);
    }
}

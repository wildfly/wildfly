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

import java.util.function.UnaryOperator;

import org.jboss.as.clustering.controller.CapabilityProvider;
import org.jboss.as.clustering.controller.ChildResourceDefinition;
import org.jboss.as.clustering.controller.ManagementResourceRegistration;
import org.jboss.as.clustering.controller.OperationHandler;
import org.jboss.as.clustering.controller.Operations;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceConfiguratorFactory;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.SimpleResourceRegistration;
import org.jboss.as.clustering.controller.UnaryRequirementCapability;
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
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.logging.ControllerLogger;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.transform.ResourceTransformationContext;
import org.jboss.as.controller.transform.ResourceTransformer;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.clustering.jgroups.spi.JGroupsRequirement;
import org.wildfly.clustering.service.UnaryRequirement;

/**
 * Resource description for the addressable resource /subsystem=jgroups/stack=X
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 * @author Paul Ferraro
 */
public class StackResourceDefinition extends ChildResourceDefinition<ManagementResourceRegistration> {

    public static final PathElement WILDCARD_PATH = pathElement(PathElement.WILDCARD_VALUE);

    public static PathElement pathElement(String name) {
        return PathElement.pathElement("stack", name);
    }

    enum Attribute implements org.jboss.as.clustering.controller.Attribute, UnaryOperator<SimpleAttributeDefinitionBuilder> {
        STATISTICS_ENABLED(ModelDescriptionConstants.STATISTICS_ENABLED, ModelType.BOOLEAN) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setDefaultValue(new ModelNode(false));
            }
        },
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type) {
            this.definition = this.apply(new SimpleAttributeDefinitionBuilder(name, type)
                    .setRequired(false)
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    ).build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    enum Capability implements CapabilityProvider {
        JCHANNEL_FACTORY(JGroupsRequirement.CHANNEL_FACTORY),
        ;
        private final org.jboss.as.clustering.controller.Capability capability;

        Capability(UnaryRequirement requirement) {
            this.capability = new UnaryRequirementCapability(requirement);
        }

        @Override
        public org.jboss.as.clustering.controller.Capability getCapability() {
            return this.capability;
        }
    }

    @Deprecated
    static final ObjectTypeAttributeDefinition TRANSPORT = ObjectTypeAttributeDefinition.Builder.of(TransportResourceDefinition.WILDCARD_PATH.getKey(), AbstractProtocolResourceDefinition.DeprecatedAttribute.TYPE.getDefinition(), TransportResourceDefinition.Attribute.SHARED.getDefinition(), TransportResourceDefinition.Attribute.SOCKET_BINDING.getDefinition(), TransportResourceDefinition.Attribute.DIAGNOSTICS_SOCKET_BINDING.getDefinition(), TransportResourceDefinition.ThreadingAttribute.DEFAULT_EXECUTOR.getDefinition(), TransportResourceDefinition.ThreadingAttribute.OOB_EXECUTOR.getDefinition(), TransportResourceDefinition.ThreadingAttribute.TIMER_EXECUTOR.getDefinition(), TransportResourceDefinition.ThreadingAttribute.THREAD_FACTORY.getDefinition(), TransportResourceDefinition.Attribute.SITE.getDefinition(), TransportResourceDefinition.Attribute.RACK.getDefinition(), TransportResourceDefinition.Attribute.MACHINE.getDefinition(), AbstractProtocolResourceDefinition.Attribute.PROPERTIES.getDefinition())
            .setDeprecated(JGroupsModel.VERSION_3_0_0.getVersion())
            .setRequired(false)
            .setSuffix(null)
            .build();

    @Deprecated
    static final ObjectTypeAttributeDefinition PROTOCOL = ObjectTypeAttributeDefinition.Builder.of(ProtocolResourceDefinition.WILDCARD_PATH.getKey(), AbstractProtocolResourceDefinition.DeprecatedAttribute.TYPE.getDefinition(), GenericProtocolResourceDefinition.DeprecatedAttribute.SOCKET_BINDING.getDefinition(), AbstractProtocolResourceDefinition.Attribute.PROPERTIES.getDefinition())
            .setRequired(false)
            .setSuffix("protocol")
            .build();

    @Deprecated
    static final AttributeDefinition PROTOCOLS = ObjectListAttributeDefinition.Builder.of("protocols", PROTOCOL)
            .setDeprecated(JGroupsModel.VERSION_3_0_0.getVersion())
            .setRequired(false)
            .build();

    static void buildTransformation(ModelVersion version, ResourceTransformationDescriptionBuilder parent) {
        ResourceTransformationDescriptionBuilder builder = parent.addChildResource(WILDCARD_PATH);

        if (JGroupsModel.VERSION_4_1_0.requiresTransformation(version)) {
            builder.getAttributeBuilder()
                    .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(new ModelNode(true)), Attribute.STATISTICS_ENABLED.getDefinition())
                    .addRejectCheck(RejectAttributeChecker.UNDEFINED, Attribute.STATISTICS_ENABLED.getDefinition())
                    .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, Attribute.STATISTICS_ENABLED.getDefinition())
                    .addRejectCheck(new RejectAttributeChecker.SimpleRejectAttributeChecker(new ModelNode(false)), Attribute.STATISTICS_ENABLED.getDefinition())
                    .end();
        }

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
        ProtocolRegistration.buildTransformation(version, builder);
    }

    static class AddOperationTransformation implements UnaryOperator<OperationStepHandler> {
        @Override
        public OperationStepHandler apply(OperationStepHandler handler) {
            return new OperationStepHandler() {
                @SuppressWarnings("deprecation")
                @Override
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                    if (operation.hasDefined(TRANSPORT.getName())) {
                        PathAddress address = context.getCurrentAddress();
                        ModelNode transport = operation.get(TRANSPORT.getName());
                        String type = AbstractProtocolResourceDefinition.DeprecatedAttribute.TYPE.resolveModelAttribute(context, transport).asString();
                        PathElement transportPath = TransportResourceDefinition.pathElement(type);
                        PathAddress transportAddress = address.append(transportPath);
                        ModelNode transportOperation = Util.createAddOperation(transportAddress);
                        OperationEntry addOperationEntry = context.getResourceRegistration().getOperationEntry(PathAddress.pathAddress(TransportResourceDefinition.WILDCARD_PATH), ModelDescriptionConstants.ADD);
                        for (AttributeDefinition attribute : addOperationEntry.getOperationDefinition().getParameters()) {
                            String name = attribute.getName();
                            if (transport.hasDefined(name)) {
                                transportOperation.get(name).set(transport.get(name));
                            }
                        }
                        context.addStep(transportOperation, addOperationEntry.getOperationHandler(), OperationContext.Stage.MODEL);
                    }
                    if (operation.hasDefined(PROTOCOLS.getName())) {
                        PathAddress address = context.getCurrentAddress();
                        for (ModelNode protocol : operation.get(PROTOCOLS.getName()).asList()) {
                            String type = AbstractProtocolResourceDefinition.DeprecatedAttribute.TYPE.resolveModelAttribute(context, protocol).asString();
                            PathElement protocolPath = ProtocolResourceDefinition.pathElement(type);
                            PathAddress protocolAddress = address.append(protocolPath);
                            ModelNode protocolOperation = Util.createAddOperation(protocolAddress);
                            OperationEntry addOperationEntry = context.getResourceRegistration().getOperationEntry(PathAddress.pathAddress(ProtocolResourceDefinition.WILDCARD_PATH), ModelDescriptionConstants.ADD);
                            for (AttributeDefinition attribute : addOperationEntry.getOperationDefinition().getParameters()) {
                                String name = attribute.getName();
                                if (protocol.hasDefined(name)) {
                                    protocolOperation.get(name).set(protocol.get(name));
                                }
                            }
                            context.addStep(protocolOperation, addOperationEntry.getOperationHandler(), OperationContext.Stage.MODEL);
                        }
                    }
                    handler.execute(context, operation);
                }
            };
        }
    }

    private final ResourceServiceConfiguratorFactory serviceConfiguratorFactory = JChannelFactoryServiceConfigurator::new;

    // registration
    public StackResourceDefinition() {
        super(WILDCARD_PATH, JGroupsExtension.SUBSYSTEM_RESOLVER.createChildResolver(WILDCARD_PATH));
    }

    @SuppressWarnings("deprecation")
    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent) {
        ManagementResourceRegistration registration = parent.registerSubModel(this);

        ResourceDescriptor descriptor = new ResourceDescriptor(this.getResourceDescriptionResolver())
                .addAttributes(Attribute.class)
                .addExtraParameters(TRANSPORT, PROTOCOLS)
                .addCapabilities(Capability.class)
                .setAddOperationTransformation(new AddOperationTransformation())
                ;
        ResourceServiceHandler handler = new StackServiceHandler(this.serviceConfiguratorFactory);
        new SimpleResourceRegistration(descriptor, handler).register(registration);

        OperationDefinition legacyAddProtocolOperation = new SimpleOperationDefinitionBuilder("add-protocol", this.getResourceDescriptionResolver())
                .setParameters(SocketBindingProtocolResourceDefinition.Attribute.SOCKET_BINDING.getDefinition())
                .addParameter(AbstractProtocolResourceDefinition.DeprecatedAttribute.TYPE.getDefinition())
                .addParameter(AbstractProtocolResourceDefinition.Attribute.PROPERTIES.getDefinition())
                .setDeprecated(JGroupsModel.VERSION_3_0_0.getVersion())
                .build();
        // Transform legacy /subsystem=jgroups/stack=*:add-protocol() operation -> /subsystem=jgroups/stack=*/protocol=*:add()
        OperationStepHandler legacyAddProtocolHandler = new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) {
                operationDeprecated(context, operation);
                PathAddress address = context.getCurrentAddress();
                String protocol = operation.require(AbstractProtocolResourceDefinition.DeprecatedAttribute.TYPE.getName()).asString();
                PathElement protocolPath = ProtocolResourceDefinition.pathElement(protocol);
                PathAddress protocolAddress = address.append(protocolPath);
                ModelNode protocolOperation = Util.createAddOperation(protocolAddress);
                OperationEntry addOperationEntry = context.getResourceRegistration().getOperationEntry(PathAddress.pathAddress(protocolPath), ModelDescriptionConstants.ADD);
                for (AttributeDefinition attribute : addOperationEntry.getOperationDefinition().getParameters()) {
                    String name = attribute.getName();
                    if (operation.hasDefined(name)) {
                        protocolOperation.get(name).set(operation.get(name));
                    }
                }
                context.addStep(protocolOperation, addOperationEntry.getOperationHandler(), OperationContext.Stage.MODEL, true);
            }
        };
        registration.registerOperationHandler(legacyAddProtocolOperation, legacyAddProtocolHandler);

        OperationDefinition legacyRemoveProtocolOperation = new SimpleOperationDefinitionBuilder("remove-protocol", this.getResourceDescriptionResolver())
                .setParameters(AbstractProtocolResourceDefinition.DeprecatedAttribute.TYPE.getDefinition())
                .setDeprecated(JGroupsModel.VERSION_3_0_0.getVersion())
                .build();
        // Transform legacy /subsystem=jgroups/stack=*:remove-protocol() operation -> /subsystem=jgroups/stack=*/protocol=*:remove()
        OperationStepHandler legacyRemoveProtocolHandler = new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) {
                operationDeprecated(context, operation);
                PathAddress address = context.getCurrentAddress();
                String protocol = operation.require(AbstractProtocolResourceDefinition.DeprecatedAttribute.TYPE.getName()).asString();
                PathElement protocolPath = ProtocolResourceDefinition.pathElement(protocol);
                PathAddress protocolAddress = address.append(protocolPath);
                ModelNode removeOperation = Util.createRemoveOperation(protocolAddress);
                context.addStep(removeOperation, context.getResourceRegistration().getOperationHandler(PathAddress.pathAddress(protocolPath), ModelDescriptionConstants.REMOVE), context.getCurrentStage());
            }
        };
        registration.registerOperationHandler(legacyRemoveProtocolOperation, legacyRemoveProtocolHandler);

        if (registration.isRuntimeOnlyRegistrationValid()) {
            new OperationHandler<>(new StackOperationExecutor(), StackOperation.class).register(registration);
        }

        new TransportRegistration(this.serviceConfiguratorFactory).register(registration);
        new ProtocolRegistration(this.serviceConfiguratorFactory).register(registration);
        new RelayResourceDefinition(this.serviceConfiguratorFactory).register(registration);

        return registration;
    }

    static void operationDeprecated(OperationContext context, ModelNode operation) {
        ControllerLogger.DEPRECATED_LOGGER.operationDeprecated(Operations.getName(operation), context.getCurrentAddress().toCLIStyleString());
    }
}

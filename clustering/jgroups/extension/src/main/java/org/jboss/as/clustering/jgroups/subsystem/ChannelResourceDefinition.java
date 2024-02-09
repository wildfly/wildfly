/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.jgroups.subsystem;

import java.util.EnumSet;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import org.jboss.as.clustering.controller.CapabilityProvider;
import org.jboss.as.clustering.controller.CapabilityReference;
import org.jboss.as.clustering.controller.ChildResourceDefinition;
import org.jboss.as.clustering.controller.ManagementResourceRegistration;
import org.jboss.as.clustering.controller.MetricHandler;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.SimpleResourceRegistrar;
import org.jboss.as.clustering.controller.UnaryRequirementCapability;
import org.jboss.as.clustering.controller.validation.ModuleIdentifierValidatorBuilder;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jgroups.JChannel;
import org.wildfly.clustering.jgroups.spi.JGroupsRequirement;
import org.wildfly.clustering.server.service.ClusteringRequirement;
import org.wildfly.clustering.service.UnaryRequirement;
import org.wildfly.subsystem.service.capture.ServiceValueExecutorRegistry;

/**
 * Definition for /subsystem=jgroups/channel=* resources
 *
 * @author Paul Ferraro
 */
public class ChannelResourceDefinition extends ChildResourceDefinition<ManagementResourceRegistration> {

    public static final PathElement WILDCARD_PATH = pathElement(PathElement.WILDCARD_VALUE);

    public static PathElement pathElement(String name) {
        return PathElement.pathElement("channel", name);
    }

    enum Capability implements CapabilityProvider {
        JCHANNEL(JGroupsRequirement.CHANNEL),
        FORK_CHANNEL_FACTORY(JGroupsRequirement.CHANNEL_FACTORY),
        JCHANNEL_FACTORY(JGroupsRequirement.CHANNEL_SOURCE),
        JCHANNEL_MODULE(JGroupsRequirement.CHANNEL_MODULE),
        JCHANNEL_CLUSTER(JGroupsRequirement.CHANNEL_CLUSTER),
        ;
        private org.jboss.as.clustering.controller.Capability capability;

        Capability(UnaryRequirement requirement) {
            this.capability = new UnaryRequirementCapability(requirement);
        }

        @Override
        public org.jboss.as.clustering.controller.Capability getCapability() {
            return this.capability;
        }
    }

    public enum Attribute implements org.jboss.as.clustering.controller.Attribute, UnaryOperator<SimpleAttributeDefinitionBuilder> {
        STACK("stack", ModelType.STRING) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setRequired(true)
                        .setAllowExpression(false)
                        .setCapabilityReference(new CapabilityReference(Capability.JCHANNEL_FACTORY, JGroupsRequirement.CHANNEL_FACTORY))
                        ;
            }
        },
        MODULE(ModelDescriptionConstants.MODULE, ModelType.STRING) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setDefaultValue(new ModelNode("org.wildfly.clustering.server"))
                        .setValidator(new ModuleIdentifierValidatorBuilder().configure(builder).build())
                        ;
            }
        },
        CLUSTER("cluster", ModelType.STRING),
        STATISTICS_ENABLED(ModelDescriptionConstants.STATISTICS_ENABLED, ModelType.BOOLEAN) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setDefaultValue(ModelNode.FALSE);
            }
        },
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type) {
            this.definition = this.apply(new SimpleAttributeDefinitionBuilder(name, type)
                    .setAllowExpression(true)
                    .setRequired(false)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    ).build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }

        @Override
        public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
            return builder;
        }
    }

    ChannelResourceDefinition() {
        super(WILDCARD_PATH, JGroupsExtension.SUBSYSTEM_RESOLVER.createChildResolver(WILDCARD_PATH));
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent) {
        ManagementResourceRegistration registration = parent.registerSubModel(this);

        ServiceValueExecutorRegistry<JChannel> executors = ServiceValueExecutorRegistry.newInstance();
        ResourceDescriptor descriptor = new ResourceDescriptor(this.getResourceDescriptionResolver())
                .addAttributes(Attribute.class)
                .addCapabilities(Capability.class)
                .addCapabilities(EnumSet.allOf(ClusteringRequirement.class).stream().map(UnaryRequirementCapability::new).collect(Collectors.toList()))
                .addRuntimeResourceRegistration(new ChannelRuntimeResourceRegistration(executors))
                .setAddOperationTransformation(DefaultStackOperationStepHandler::new)
                ;
        ResourceServiceHandler handler = new ChannelServiceHandler(executors);
        new SimpleResourceRegistrar(descriptor, handler).register(registration);

        if (registration.isRuntimeOnlyRegistrationValid()) {
            new MetricHandler<>(new ChannelMetricExecutor(executors), ChannelMetric.class).register(registration);
        }

        new ForkResourceDefinition(executors).register(registration);

        return registration;
    }

    private static class DefaultStackOperationStepHandler implements OperationStepHandler {
        private final OperationStepHandler handler;

        DefaultStackOperationStepHandler(OperationStepHandler handler) {
            this.handler = handler;
        }

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            // Add operations emitted by legacy Infinispan subsystem may not have a stack specified
            // In this case, fix operation to use stack of default channel
            if (!operation.hasDefined(Attribute.STACK.getName())) {
                PathAddress subsystemAddress = context.getCurrentAddress().getParent();
                Resource root = context.readResourceFromRoot(subsystemAddress.getParent(), false);
                if (!root.hasChild(subsystemAddress.getLastElement())) {
                    // Subsystem not yet added - defer operation execution
                    context.addStep(operation, this, context.getCurrentStage());
                    return;
                }
                Resource subsystem = context.readResourceFromRoot(subsystemAddress, false);
                ModelNode subsystemModel = subsystem.getModel();
                if (subsystemModel.hasDefined(JGroupsSubsystemResourceDefinition.Attribute.DEFAULT_CHANNEL.getName())) {
                    String defaultChannel = subsystemModel.get(JGroupsSubsystemResourceDefinition.Attribute.DEFAULT_CHANNEL.getName()).asString();
                    if (!context.getCurrentAddressValue().equals(defaultChannel)) {
                        PathElement defaultChannelPath = pathElement(defaultChannel);
                        if (!subsystem.hasChild(defaultChannelPath)) {
                            // Default channel was not yet added, defer operation execution
                            context.addStep(operation, this, context.getCurrentStage());
                            return;
                        }
                        Resource channel = context.readResourceFromRoot(subsystemAddress.append(defaultChannelPath), false);
                        ModelNode channelModel = channel.getModel();
                        String defaultStack = channelModel.get(Attribute.STACK.getName()).asString();
                        operation.get(Attribute.STACK.getName()).set(defaultStack);
                    }
                }
            }
            this.handler.execute(context, operation);
        }
    }
}

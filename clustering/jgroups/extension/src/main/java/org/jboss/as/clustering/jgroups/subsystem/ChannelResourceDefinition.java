/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.function.UnaryOperator;

import org.jboss.as.clustering.controller.CapabilityProvider;
import org.jboss.as.clustering.controller.CapabilityReference;
import org.jboss.as.clustering.controller.ChildResourceDefinition;
import org.jboss.as.clustering.controller.ManagementResourceRegistration;
import org.jboss.as.clustering.controller.MetricHandler;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ServiceValueExecutorRegistry;
import org.jboss.as.clustering.controller.SimpleResourceRegistration;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.UnaryRequirementCapability;
import org.jboss.as.clustering.controller.validation.ModuleIdentifierValidatorBuilder;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jgroups.JChannel;
import org.wildfly.clustering.jgroups.spi.JGroupsRequirement;
import org.wildfly.clustering.service.UnaryRequirement;
import org.wildfly.clustering.spi.ClusteringRequirement;

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

    static final Map<ClusteringRequirement, org.jboss.as.clustering.controller.Capability> CLUSTERING_CAPABILITIES = new EnumMap<>(ClusteringRequirement.class);
    static {
        for (ClusteringRequirement requirement : EnumSet.allOf(ClusteringRequirement.class)) {
            CLUSTERING_CAPABILITIES.put(requirement, new UnaryRequirementCapability(requirement));
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

    public enum DeprecatedAttribute implements org.jboss.as.clustering.controller.Attribute {
        STATS_ENABLED("stats-enabled", ModelType.BOOLEAN, JGroupsModel.VERSION_4_1_0),
        ;
        private final AttributeDefinition definition;

        DeprecatedAttribute(String name, ModelType type, JGroupsModel deprecation) {
            this.definition = new SimpleAttributeDefinitionBuilder(name, type, true).setDeprecated(deprecation.getVersion()).setStorageRuntime().build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    static class AddOperationTransformation implements UnaryOperator<OperationStepHandler> {
        @Override
        public OperationStepHandler apply(OperationStepHandler handler) {
            return new OperationStepHandler() {
                @SuppressWarnings("deprecation")
                @Override
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                    // Handle recipe for version < 4.0 where stack was not required and the stack attribute would use default-stack for a default value
                    if (!operation.hasDefined(Attribute.STACK.getName())) {
                        ModelNode parentModel = context.readResourceFromRoot(context.getCurrentAddress().getParent(), false).getModel();
                        // If default-stack is not defined either, then recipe must be for version >= 4.0 and so this really is an invalid operation
                        if (parentModel.hasDefined(JGroupsSubsystemResourceDefinition.Attribute.DEFAULT_STACK.getName())) {
                            operation.get(Attribute.STACK.getName()).set(parentModel.get(JGroupsSubsystemResourceDefinition.Attribute.DEFAULT_STACK.getName()));
                        }
                    }
                    handler.execute(context, operation);
                }
            };
        }
    }

    ChannelResourceDefinition() {
        super(WILDCARD_PATH, JGroupsExtension.SUBSYSTEM_RESOLVER.createChildResolver(WILDCARD_PATH));
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent) {
        ManagementResourceRegistration registration = parent.registerSubModel(this);

        ServiceValueExecutorRegistry<JChannel> executors = new ServiceValueExecutorRegistry<>();
        ResourceDescriptor descriptor = new ResourceDescriptor(this.getResourceDescriptionResolver())
                .addAttributes(Attribute.class)
                .addCapabilities(Capability.class)
                .addCapabilities(CLUSTERING_CAPABILITIES.values())
                .addAlias(DeprecatedAttribute.STATS_ENABLED, Attribute.STATISTICS_ENABLED)
                .setAddOperationTransformation(new AddOperationTransformation())
                .addRuntimeResourceRegistration(new ChannelRuntimeResourceRegistration(executors))
                ;
        ResourceServiceHandler handler = new ChannelServiceHandler(executors);
        new SimpleResourceRegistration(descriptor, handler).register(registration);

        if (registration.isRuntimeOnlyRegistrationValid()) {
            new MetricHandler<>(new ChannelMetricExecutor(executors), ChannelMetric.class).register(registration);
        }

        new ForkResourceDefinition(executors).register(registration);

        return registration;
    }
}

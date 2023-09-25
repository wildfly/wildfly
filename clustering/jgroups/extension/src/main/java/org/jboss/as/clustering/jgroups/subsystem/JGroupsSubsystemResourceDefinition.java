/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.jgroups.subsystem;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;

import org.jboss.as.clustering.controller.Capability;
import org.jboss.as.clustering.controller.CapabilityReference;
import org.jboss.as.clustering.controller.DefaultSubsystemDescribeHandler;
import org.jboss.as.clustering.controller.ManagementResourceRegistration;
import org.jboss.as.clustering.controller.RequirementCapability;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.SimpleResourceRegistrar;
import org.jboss.as.clustering.controller.SubsystemRegistration;
import org.jboss.as.clustering.controller.SubsystemResourceDefinition;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelType;
import org.wildfly.clustering.jgroups.spi.JGroupsRequirement;
import org.wildfly.clustering.server.service.ClusteringRequirement;

/**
 * The root resource of the JGroups subsystem.
 *
 * @author Richard Achmatowicz (c) 2012 Red Hat Inc.
 */
public class JGroupsSubsystemResourceDefinition extends SubsystemResourceDefinition {

    public static final PathElement PATH = pathElement(JGroupsExtension.SUBSYSTEM_NAME);

    static final Map<JGroupsRequirement, Capability> CAPABILITIES = new EnumMap<>(JGroupsRequirement.class);
    static {
        for (JGroupsRequirement requirement : EnumSet.allOf(JGroupsRequirement.class)) {
            CAPABILITIES.put(requirement, new RequirementCapability(requirement.getDefaultRequirement()));
        }
    }

    public enum Attribute implements org.jboss.as.clustering.controller.Attribute, UnaryOperator<SimpleAttributeDefinitionBuilder> {
        DEFAULT_CHANNEL("default-channel", ModelType.STRING) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setCapabilityReference(new CapabilityReference(CAPABILITIES.get(JGroupsRequirement.CHANNEL_FACTORY), JGroupsRequirement.CHANNEL_FACTORY));
            }
        },
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type) {
            this.definition = this.apply(new SimpleAttributeDefinitionBuilder(name, type)
                    .setRequired(false)
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .setXmlName(XMLAttribute.DEFAULT.getLocalName())
                    ).build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    JGroupsSubsystemResourceDefinition() {
        super(PATH, JGroupsExtension.SUBSYSTEM_RESOLVER);
    }

    @Override
    public void register(SubsystemRegistration parentRegistration) {
        ManagementResourceRegistration registration = parentRegistration.registerSubsystemModel(this);

        new DefaultSubsystemDescribeHandler().register(registration);

        Set<ClusteringRequirement> requirements = EnumSet.allOf(ClusteringRequirement.class);
        List<Capability> capabilities = new ArrayList<>(requirements.size());
        UnaryOperator<RuntimeCapability.Builder<Void>> configurator = builder -> builder.setAllowMultipleRegistrations(true);
        for (ClusteringRequirement requirement : requirements) {
            capabilities.add(new RequirementCapability(requirement.getDefaultRequirement(), configurator));
        }

        ResourceDescriptor descriptor = new ResourceDescriptor(this.getResourceDescriptionResolver())
                .addAttributes(Attribute.class)
                .addCapabilities(model -> model.hasDefined(Attribute.DEFAULT_CHANNEL.getName()), CAPABILITIES.values())
                .addCapabilities(model -> model.hasDefined(Attribute.DEFAULT_CHANNEL.getName()), capabilities)
                ;
        ResourceServiceHandler handler = new JGroupsSubsystemServiceHandler();
        new SimpleResourceRegistrar(descriptor, handler).register(registration);

        new ChannelResourceDefinition().register(registration);
        new StackResourceDefinition().register(registration);
    }
}

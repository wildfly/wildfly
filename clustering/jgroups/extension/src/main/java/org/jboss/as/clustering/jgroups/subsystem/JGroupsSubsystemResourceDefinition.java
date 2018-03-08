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

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.function.UnaryOperator;

import org.jboss.as.clustering.controller.Capability;
import org.jboss.as.clustering.controller.CapabilityReference;
import org.jboss.as.clustering.controller.DefaultSubsystemDescribeHandler;
import org.jboss.as.clustering.controller.ManagementResourceRegistration;
import org.jboss.as.clustering.controller.RequirementCapability;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.SimpleResourceRegistration;
import org.jboss.as.clustering.controller.SubsystemRegistration;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.SubsystemResourceDefinition;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescription;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;
import org.jboss.dmr.ModelType;
import org.wildfly.clustering.jgroups.spi.JGroupsRequirement;
import org.wildfly.clustering.spi.ClusteringRequirement;

/**
 * The root resource of the JGroups subsystem.
 *
 * @author Richard Achmatowicz (c) 2012 Red Hat Inc.
 */
public class JGroupsSubsystemResourceDefinition extends SubsystemResourceDefinition<SubsystemRegistration> {

    public static final PathElement PATH = pathElement(JGroupsExtension.SUBSYSTEM_NAME);

    static final Map<JGroupsRequirement, Capability> CAPABILITIES = new EnumMap<>(JGroupsRequirement.class);
    static {
        for (JGroupsRequirement requirement : EnumSet.allOf(JGroupsRequirement.class)) {
            CAPABILITIES.put(requirement, new RequirementCapability(requirement.getDefaultRequirement()));
        }
    }

    static final Map<ClusteringRequirement, Capability> CLUSTERING_CAPABILITIES = new EnumMap<>(ClusteringRequirement.class);
    static {
        for (ClusteringRequirement requirement : EnumSet.allOf(ClusteringRequirement.class)) {
            CLUSTERING_CAPABILITIES.put(requirement, new RequirementCapability(requirement.getDefaultRequirement(), builder -> builder.setAllowMultipleRegistrations(true)));
        }
    }

    public enum Attribute implements org.jboss.as.clustering.controller.Attribute {
        DEFAULT_CHANNEL("default-channel", ModelType.STRING, builder -> builder.setCapabilityReference(new CapabilityReference(CAPABILITIES.get(JGroupsRequirement.CHANNEL_FACTORY), JGroupsRequirement.CHANNEL_FACTORY))),
        @Deprecated DEFAULT_STACK("default-stack", ModelType.STRING, builder -> builder.setDeprecated(JGroupsModel.VERSION_3_0_0.getVersion())),
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type, UnaryOperator<SimpleAttributeDefinitionBuilder> configurator) {
            this.definition = configurator.apply(new SimpleAttributeDefinitionBuilder(name, type)
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

    static TransformationDescription buildTransformers(ModelVersion version) {
        ResourceTransformationDescriptionBuilder builder = TransformationDescriptionBuilder.Factory.createSubsystemInstance();

        if (JGroupsModel.VERSION_3_0_0.requiresTransformation(version)) {
            builder.getAttributeBuilder()
                    // The attribute is always discarded, the children will drive rejection/discardation
                    .setDiscard(DiscardAttributeChecker.ALWAYS, Attribute.DEFAULT_CHANNEL.getDefinition())
                    .end();
        }

        ChannelResourceDefinition.buildTransformation(version, builder);
        StackResourceDefinition.buildTransformation(version, builder);

        return builder.build();
    }

    JGroupsSubsystemResourceDefinition() {
        super(PATH, JGroupsExtension.SUBSYSTEM_RESOLVER);
    }

    @Override
    public void register(SubsystemRegistration parentRegistration) {
        ManagementResourceRegistration registration = parentRegistration.registerSubsystemModel(this);

        new DefaultSubsystemDescribeHandler().register(registration);

        ResourceDescriptor descriptor = new ResourceDescriptor(this.getResourceDescriptionResolver())
                .addAttributes(Attribute.class)
                .addCapabilities(model -> model.hasDefined(Attribute.DEFAULT_CHANNEL.getName()), CAPABILITIES.values())
                .addCapabilities(model -> model.hasDefined(Attribute.DEFAULT_CHANNEL.getName()), CLUSTERING_CAPABILITIES.values())
                ;
        ResourceServiceHandler handler = new JGroupsSubsystemServiceHandler();
        new SimpleResourceRegistration(descriptor, handler).register(registration);

        new ChannelResourceDefinition().register(registration);
        new StackResourceDefinition().register(registration);
    }
}

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

import org.jboss.as.clustering.controller.AddStepHandler;
import org.jboss.as.clustering.controller.Capability;
import org.jboss.as.clustering.controller.CapabilityReference;
import org.jboss.as.clustering.controller.RemoveStepHandler;
import org.jboss.as.clustering.controller.RequirementCapability;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.SubsystemResourceDefinition;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.CapabilityReferenceRecorder;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescription;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;
import org.jboss.dmr.ModelType;
import org.wildfly.clustering.jgroups.spi.JGroupsRequirement;

/**
 * The root resource of the JGroups subsystem.
 *
 * @author Richard Achmatowicz (c) 2012 Red Hat Inc.
 */
public class JGroupsSubsystemResourceDefinition extends SubsystemResourceDefinition {

    public static final PathElement PATH = PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, JGroupsExtension.SUBSYSTEM_NAME);

    static final Map<JGroupsRequirement, Capability> CAPABILITIES = new EnumMap<>(JGroupsRequirement.class);
    static {
        EnumSet.allOf(JGroupsRequirement.class).forEach(requirement -> CAPABILITIES.put(requirement, new RequirementCapability(requirement.getDefaultRequirement())));
    }

    public enum Attribute implements org.jboss.as.clustering.controller.Attribute {
        DEFAULT_CHANNEL("default-channel", ModelType.STRING, new CapabilityReference(CAPABILITIES.get(JGroupsRequirement.CHANNEL_FACTORY), JGroupsRequirement.CHANNEL_FACTORY)),
        @Deprecated DEFAULT_STACK("default-stack", ModelType.STRING, JGroupsModel.VERSION_3_0_0),
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type, CapabilityReferenceRecorder reference) {
            this.definition = createBuilder(name, type).setCapabilityReference(reference).build();
        }

        Attribute(String name, ModelType type, JGroupsModel deprecation) {
            this.definition = createBuilder(name, type).setDeprecated(deprecation.getVersion()).build();
        }

        static SimpleAttributeDefinitionBuilder createBuilder(String name, ModelType type) {
            return new SimpleAttributeDefinitionBuilder(name, type)
                    .setAllowNull(true)
                    .setAllowExpression(false) // These are model references
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .setXmlName(XMLAttribute.DEFAULT.getLocalName())
            ;
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    private final boolean allowRuntimeOnlyRegistration;

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

    JGroupsSubsystemResourceDefinition(boolean allowRuntimeOnlyRegistration) {
        super(PATH, new JGroupsResourceDescriptionResolver());
        this.allowRuntimeOnlyRegistration = allowRuntimeOnlyRegistration;
    }

    @Override
    public void register(SubsystemRegistration parentRegistration) {
        ManagementResourceRegistration registration = parentRegistration.registerSubsystemModel(this);

        registration.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE);

        ResourceDescriptor descriptor = new ResourceDescriptor(this.getResourceDescriptionResolver())
                .addAttributes(Attribute.class)
                .addCapabilities(CAPABILITIES.values())
                ;
        ResourceServiceHandler handler = new JGroupsSubsystemServiceHandler();
        new AddStepHandler(descriptor, handler).register(registration);
        new RemoveStepHandler(descriptor, handler).register(registration);

        new ChannelResourceDefinition(this.allowRuntimeOnlyRegistration).register(registration);
        new StackResourceDefinition(this.allowRuntimeOnlyRegistration).register(registration);
    }
}

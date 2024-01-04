/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web;

import java.util.EnumSet;
import java.util.function.UnaryOperator;

import org.jboss.as.clustering.controller.CapabilityProvider;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.UnaryRequirementCapability;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.capability.UnaryCapabilityNameResolver;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.clustering.service.UnaryRequirement;
import org.wildfly.clustering.web.service.WebProviderRequirement;
import org.wildfly.clustering.web.service.WebRequirement;

/**
 * @author Paul Ferraro
 */
public class RankedAffinityResourceDefinition extends AffinityResourceDefinition {

    static final PathElement PATH = pathElement("ranked");

    enum Capability implements CapabilityProvider, UnaryOperator<RuntimeCapability.Builder<Void>> {
        AFFINITY(WebProviderRequirement.AFFINITY),
        ;
        private final org.jboss.as.clustering.controller.Capability capability;

        Capability(UnaryRequirement requirement) {
            this.capability = new UnaryRequirementCapability(requirement, this);
        }

        @Override
        public org.jboss.as.clustering.controller.Capability getCapability() {
            return this.capability;
        }

        @Override
        public RuntimeCapability.Builder<Void> apply(RuntimeCapability.Builder<Void> builder) {
            return builder.setAllowMultipleRegistrations(true)
                    .setDynamicNameMapper(UnaryCapabilityNameResolver.PARENT)
                    .addRequirements(WebRequirement.INFINISPAN_ROUTING_PROVIDER.getName());
        }
    }

    public enum Attribute implements org.jboss.as.clustering.controller.Attribute {
        DELIMITER("delimiter", ModelType.STRING, new ModelNode(".")),
        MAX_ROUTES("max-routes", ModelType.STRING, new ModelNode(3)),
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type, ModelNode defaultValue) {
            this.definition = new SimpleAttributeDefinitionBuilder(name, type)
                    .setAllowExpression(false)
                    .setDefaultValue(defaultValue)
                    .setRequired(false)
                    .build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    static class ResourceDescriptorConfigurator implements UnaryOperator<ResourceDescriptor> {
        @Override
        public ResourceDescriptor apply(ResourceDescriptor descriptor) {
            return descriptor.addCapabilities(Capability.class).addAttributes(Attribute.class);
        }
    }

    RankedAffinityResourceDefinition() {
        super(PATH, EnumSet.allOf(Capability.class), new ResourceDescriptorConfigurator(), RankedAffinityServiceConfigurator::new);
    }
}

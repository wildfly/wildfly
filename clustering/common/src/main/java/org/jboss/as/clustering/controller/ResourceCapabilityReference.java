/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import java.util.function.Function;

import org.jboss.as.controller.CapabilityReferenceRecorder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.capability.BinaryCapabilityNameResolver;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.capability.UnaryCapabilityNameResolver;
import org.jboss.as.controller.registry.Resource;
import org.wildfly.clustering.service.BinaryRequirement;
import org.wildfly.clustering.service.Requirement;
import org.wildfly.clustering.service.UnaryRequirement;

/**
 * {@link CapabilityReferenceRecorder} for resource-level capability references.
 * @author Paul Ferraro
 */
public class ResourceCapabilityReference extends AbstractCapabilityReference {
    private final Function<PathAddress, String[]> requirementNameResolver;

    /**
     * Creates a new reference between the specified capability and the specified requirement
     * @param capability the capability referencing the specified requirement
     * @param requirement the requirement of the specified capability
     */
    public ResourceCapabilityReference(Capability capability, Requirement requirement) {
        this(capability, requirement, SimpleCapabilityNameResolver.EMPTY);
    }

    /**
     * Creates a new reference between the specified capability and the specified requirement
     * @param capability the capability referencing the specified requirement
     * @param requirement the requirement of the specified capability
     * @param requirementNameResolver function for resolving the dynamic elements of the requirement name
     */
    public ResourceCapabilityReference(Capability capability, UnaryRequirement requirement, UnaryCapabilityNameResolver requirementNameResolver) {
        this(capability, (Requirement) requirement, requirementNameResolver);
    }

    /**
     * Creates a new reference between the specified capability and the specified requirement
     * @param capability the capability referencing the specified requirement
     * @param requirement the requirement of the specified capability
     * @param requirementNameResolver function for resolving the dynamic elements of the requirement name
     */
    public ResourceCapabilityReference(Capability capability, BinaryRequirement requirement, BinaryCapabilityNameResolver requirementNameResolver) {
        this(capability, (Requirement) requirement, requirementNameResolver);
    }

    private ResourceCapabilityReference(Capability capability, Requirement requirement, Function<PathAddress, String[]> requirementNameResolver) {
        super(capability, requirement);
        this.requirementNameResolver = requirementNameResolver;
    }

    @Override
    public void addCapabilityRequirements(OperationContext context, Resource resource, String attributeName, String... values) {
        context.registerAdditionalCapabilityRequirement(this.getRequirementName(context), this.getDependentName(context), attributeName);
    }

    @Override
    public void removeCapabilityRequirements(OperationContext context, Resource resource, String attributeName, String... values) {
        context.deregisterCapabilityRequirement(this.getRequirementName(context), this.getDependentName(context));
    }

    private String getRequirementName(OperationContext context) {
        String[] parts = this.requirementNameResolver.apply(context.getCurrentAddress());
        return (parts.length > 0) ? RuntimeCapability.buildDynamicCapabilityName(this.getBaseRequirementName(), parts) : this.getBaseRequirementName();
    }

    @Override
    public String[] getRequirementPatternSegments(String name, PathAddress address) {
        String[] segments = this.requirementNameResolver.apply(address);
        for (int i = 0; i < segments.length; ++i) {
            String segment = segments[i];
            if (segment.charAt(0) == '$') {
                segments[i] = segment.substring(1);
            }
        }
        return segments;
    }
}

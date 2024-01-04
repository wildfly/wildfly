/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import java.util.function.Function;

import org.jboss.as.controller.CapabilityReferenceRecorder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.Resource;
import org.wildfly.clustering.service.BinaryRequirement;
import org.wildfly.clustering.service.UnaryRequirement;

/**
 * {@link CapabilityReferenceRecorder} that delegates to {@link Capability#resolve(org.jboss.as.controller.PathAddress)} to generate the name of the dependent capability.
 * @author Paul Ferraro
 */
public class CapabilityReference extends AbstractCapabilityReference {

    private final Function<OperationContext, String> parentNameResolver;
    private final String parentSegment;

    /**
     * Creates a new reference between the specified capability and the specified requirement
     * @param capability the capability referencing the specified requirement
     * @param requirement the requirement of the specified capability
     */
    public CapabilityReference(Capability capability, UnaryRequirement requirement) {
        super(capability, requirement);
        this.parentNameResolver = null;
        this.parentSegment = null;
    }

    /**
     * Creates a new reference between the specified capability and the specified requirement
     * @param capability the capability referencing the specified requirement
     * @param requirement the requirement of the specified capability
     */
    public CapabilityReference(Capability capability, BinaryRequirement requirement, PathElement path) {
        this(capability, requirement, OperationContext::getCurrentAddressValue, path.getKey());
    }

    /**
     * Creates a new reference between the specified capability and the specified requirement
     * @param capability the capability referencing the specified requirement
     * @param requirement the requirement of the specified capability
     * @param parentAttribute the attribute containing the value of the parent dynamic component of the requirement
     */
    public CapabilityReference(Capability capability, BinaryRequirement requirement, Attribute parentAttribute) {
        this(capability, requirement, context -> context.readResource(PathAddress.EMPTY_ADDRESS, false).getModel().get(parentAttribute.getName()).asString(), parentAttribute.getName());
    }

    /**
     * Creates a new reference between the specified capability and the specified requirement
     * @param capability the capability referencing the specified requirement
     * @param requirement the requirement of the specified capability
     * @param parentNameResolver the resolver of the parent dynamic component of the requirement
     */
    private CapabilityReference(Capability capability, BinaryRequirement requirement, Function<OperationContext, String> parentNameResolver, String parentSegment) {
        super(capability, requirement);
        this.parentNameResolver = parentNameResolver;
        this.parentSegment = parentSegment;
    }

    @Override
    public void addCapabilityRequirements(OperationContext context, Resource resource, String attributeName, String... values) {
        String dependentName = this.getDependentName(context);
        for (String value : values) {
            if (value != null) {
                context.registerAdditionalCapabilityRequirement(this.getRequirementName(context, value), dependentName, attributeName);
            }
        }
    }

    @Override
    public void removeCapabilityRequirements(OperationContext context, Resource resource, String attributeName, String... values) {
        String dependentName = this.getDependentName(context);
        for (String value : values) {
            if (value != null) {
                context.deregisterCapabilityRequirement(this.getRequirementName(context, value), dependentName);
            }
        }
    }

    private String getRequirementName(OperationContext context, String value) {
        String[] parts = (this.parentNameResolver != null) ? new String[] { this.parentNameResolver.apply(context), value } : new String[] { value };
        return RuntimeCapability.buildDynamicCapabilityName(this.getBaseRequirementName(), parts);
    }

    @Override
    public String[] getRequirementPatternSegments(String name, PathAddress address) {
        return (this.parentSegment != null) ? new String[] { this.parentSegment, name } : new String[] { name };
    }
}

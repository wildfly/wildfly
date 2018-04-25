/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.controller;

import java.util.function.Function;

import org.jboss.as.controller.CapabilityReferenceRecorder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.capability.RuntimeCapability;
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
     * @param requirementNameResolver function for resolving the dynamic elements of the requirement name
     */
    public ResourceCapabilityReference(Capability capability, Requirement requirement) {
        this(capability, requirement, new SimpleCapabilityNameResolver());
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
    public void addCapabilityRequirements(OperationContext context, Resource resource,  String attributeName, String... values) {
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

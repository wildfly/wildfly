/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

import org.jboss.as.controller.CapabilityReferenceRecorder;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.OperationContext;

/**
 * {@link CapabilityReferenceRecorder} that delegates to {@link Capability#getRuntimeCapability(org.jboss.as.controller.PathAddress)} to generate the name of the dependent capability.
 * @author Paul Ferraro
 */
public class CapabilityReference implements CapabilityReferenceRecorder {

    private final Requirement requirement;
    private final Capability capability;

    /**
     * Creates a new reference between the specified requirement and the specified capability
     * @param requirement the requirement of the specified capability
     * @param capability the capability referencing the specified requirement
     */
    public CapabilityReference(Requirement requirement, Capability capability) {
        this.requirement = requirement;
        this.capability = capability;
    }

    @Override
    public void addCapabilityRequirements(OperationContext context, String attributeName, String... attributeValues) {
        String dependentName = this.capability.getRuntimeCapability(context.getCurrentAddress()).getName();
        for (String attributeValue : attributeValues) {
            String requirementName = RuntimeCapability.buildDynamicCapabilityName(this.requirement.getName(), attributeValue);
            context.registerAdditionalCapabilityRequirement(requirementName, dependentName, attributeName);
        }
    }

    @Override
    public void removeCapabilityRequirements(OperationContext context, String attributeName, String... attributeValues) {
        String dependentName = this.capability.getRuntimeCapability(context.getCurrentAddress()).getName();
        for (String attributeValue : attributeValues) {
            String requirementName = RuntimeCapability.buildDynamicCapabilityName(this.requirement.getName(), attributeValue);
            context.deregisterCapabilityRequirement(requirementName, dependentName);
        }
    }

    @Override
    public String getBaseDependentName() {
        return this.capability.getDefinition().getName();
    }

    @Override
    public String getBaseRequirementName() {
        return this.requirement.getName();
    }

    @Override
    public boolean isDynamicDependent() {
        return true;
    }
}

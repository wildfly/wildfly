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

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import org.jboss.as.controller.CapabilityReferenceRecorder;
import org.jboss.as.controller.OperationContext;
import org.wildfly.clustering.service.BinaryRequirement;
import org.wildfly.clustering.service.Requirement;
import org.wildfly.clustering.service.UnaryRequirement;

/**
 * {@link CapabilityReferenceRecorder} that delegates to {@link Capability#resolve(org.jboss.as.controller.PathAddress)} to generate the name of the dependent capability.
 * @author Paul Ferraro
 */
public class CapabilityReference implements CapabilityReferenceRecorder {

    private final Capability capability;
    private final Requirement requirement;
    private final BiFunction<OperationContext, String, Optional<String>> requirementResolver;

    /**
     * Creates a new reference between the specified capability and the specified requirement
     * @param capability the capability referencing the specified requirement
     * @param requirement the requirement of the specified capability
     */
    public CapabilityReference(Capability capability, UnaryRequirement requirement) {
        this(capability, requirement, (context, value) -> (value != null) ? Optional.of(requirement.resolve(value)) : Optional.empty());
    }

    /**
     * Creates a new reference between the specified capability and the specified requirement
     * @param capability the capability referencing the specified requirement
     * @param requirement the requirement of the specified capability
     */
    public CapabilityReference(Capability capability, BinaryRequirement requirement) {
        this(capability, requirement, (context, value) -> (value != null) ? Optional.of(requirement.resolve(context.getCurrentAddressValue(), value)) : Optional.empty());
    }

    CapabilityReference(Capability capability, Requirement requirement, BiFunction<OperationContext, String, Optional<String>> requirementResolver) {
        this.capability = capability;
        this.requirement = requirement;
        this.requirementResolver = requirementResolver;
    }

    @Override
    public void addCapabilityRequirements(OperationContext context, String attributeName, String... values) {
        String dependentName = this.capability.resolve(context.getCurrentAddress()).getName();
        Stream.of(values).forEach(value -> this.requirementResolver.apply(context, value).ifPresent(requirementName -> context.registerAdditionalCapabilityRequirement(requirementName, dependentName, attributeName)));
    }

    @Override
    public void removeCapabilityRequirements(OperationContext context, String attributeName, String... values) {
        String dependentName = this.capability.resolve(context.getCurrentAddress()).getName();
        Stream.of(values).forEach(value -> this.requirementResolver.apply(context, value).ifPresent(requirementName -> context.deregisterCapabilityRequirement(requirementName, dependentName)));
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
        return this.capability.getDefinition().isDynamicallyNamed();
    }
}

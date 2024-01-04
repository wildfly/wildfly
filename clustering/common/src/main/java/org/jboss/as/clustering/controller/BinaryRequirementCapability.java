/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import java.util.function.UnaryOperator;

import org.jboss.as.controller.capability.BinaryCapabilityNameResolver;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.wildfly.clustering.service.BinaryRequirement;

/**
 * Provides a capability definition provider built from a binary requirement.
 * @author Paul Ferraro
 */
public class BinaryRequirementCapability implements Capability {

    private final RuntimeCapability<Void> definition;

    /**
     * Creates a new capability based on the specified requirement
     * @param requirement the requirement basis
     */
    public BinaryRequirementCapability(BinaryRequirement requirement) {
        this(requirement, BinaryCapabilityNameResolver.PARENT_CHILD);
    }

    /**
     * Creates a new capability based on the specified requirement
     * @param requirement the requirement basis
     * @param resolver a capability name resolver
     */
    public BinaryRequirementCapability(BinaryRequirement requirement, BinaryCapabilityNameResolver resolver) {
        this(requirement, new CapabilityNameResolverConfigurator(resolver));
    }

    /**
     * Creates a new capability based on the specified requirement
     * @param requirement the requirement basis
     * @param configurator configures the capability
     */
    public BinaryRequirementCapability(BinaryRequirement requirement, UnaryOperator<RuntimeCapability.Builder<Void>> configurator) {
        this.definition = configurator.apply(RuntimeCapability.Builder.of(requirement.getName(), true).setServiceType(requirement.getType())).build();
    }

    @Override
    public RuntimeCapability<Void> getDefinition() {
        return this.definition;
    }
}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import java.util.function.UnaryOperator;

import org.jboss.as.controller.capability.RuntimeCapability;
import org.wildfly.clustering.service.UnaryRequirement;

/**
 * Provides a capability definition provider built from a unary requirement.
 * @author Paul Ferraro
 */
public class UnaryRequirementCapability implements Capability {

    private final RuntimeCapability<Void> definition;

    /**
     * Creates a new capability based on the specified unary requirement
     * @param requirement the unary requirement basis
     */
    public UnaryRequirementCapability(UnaryRequirement requirement) {
        this(requirement, UnaryCapabilityNameResolver.DEFAULT);
    }

    /**
     * Creates a new capability based on the specified unary requirement
     * @param requirement the unary requirement basis
     * @param resolver a capability name resolver
     */
    public UnaryRequirementCapability(UnaryRequirement requirement, UnaryCapabilityNameResolver resolver) {
        this(requirement, new CapabilityNameResolverConfigurator(resolver));
    }

    /**
     * Creates a new capability based on the specified unary requirement
     * @param requirement the unary requirement basis
     * @param configurator configures the runtime capability
     */
    public UnaryRequirementCapability(UnaryRequirement requirement, UnaryOperator<RuntimeCapability.Builder<Void>> configurator) {
        this.definition = configurator.apply(RuntimeCapability.Builder.of(requirement.getName(), true).setServiceType(requirement.getType())).build();
    }

    @Override
    public RuntimeCapability<Void> getDefinition() {
        return this.definition;
    }
}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import java.util.function.UnaryOperator;

import org.jboss.as.controller.capability.RuntimeCapability;
import org.wildfly.clustering.service.Requirement;

/**
 * Provides a capability definition provider built from a requirement.
 * @author Paul Ferraro
 */
public class RequirementCapability implements Capability {

    private final RuntimeCapability<Void> definition;

    /**
     * Creates a new capability based on the specified requirement
     * @param requirement the requirement basis
     */
    public RequirementCapability(Requirement requirement) {
        this(requirement, UnaryOperator.identity());
    }

    /**
     * Creates a new capability based on the specified requirement
     * @param requirement the requirement basis
     * @param configurator configures the capability
     */
    public RequirementCapability(Requirement requirement, UnaryOperator<RuntimeCapability.Builder<Void>> configurator) {
        this.definition = configurator.apply(RuntimeCapability.Builder.of(requirement.getName()).setServiceType(requirement.getType())).build();
    }

    @Override
    public RuntimeCapability<Void> getDefinition() {
        return this.definition;
    }
}

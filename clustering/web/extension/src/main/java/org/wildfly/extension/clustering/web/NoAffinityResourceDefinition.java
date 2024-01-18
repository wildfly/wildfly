/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web;

import java.util.EnumSet;
import java.util.function.UnaryOperator;

import org.jboss.as.clustering.controller.CapabilityProvider;
import org.jboss.as.clustering.controller.UnaryRequirementCapability;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.capability.UnaryCapabilityNameResolver;
import org.wildfly.clustering.service.UnaryRequirement;
import org.wildfly.clustering.web.service.WebProviderRequirement;

/**
 * @author Paul Ferraro
 */
public class NoAffinityResourceDefinition extends AffinityResourceDefinition {

    static final PathElement PATH = pathElement("none");

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
                    .setDynamicNameMapper(UnaryCapabilityNameResolver.PARENT);
        }
    }

    NoAffinityResourceDefinition() {
        super(PATH, EnumSet.allOf(Capability.class), UnaryOperator.identity(), NoAffinityServiceConfigurator::new);
    }
}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.service.UnaryRequirement;

/**
 * Factory for generating a {@link ServiceName} for a {@link UnaryRequirement}.
 * @author Paul Ferraro
 */
public class UnaryRequirementServiceNameFactory implements UnaryServiceNameFactory {

    private final UnaryRequirement requirement;

    public UnaryRequirementServiceNameFactory(UnaryRequirement requirement) {
        this.requirement = requirement;
    }

    @Override
    public ServiceName getServiceName(OperationContext context, String name) {
        return context.getCapabilityServiceName(this.requirement.getName(), this.requirement.getType(), name);
    }

    @Override
    public ServiceName getServiceName(CapabilityServiceSupport support, String name) {
        return support.getCapabilityServiceName(this.requirement.getName(), name);
    }
}

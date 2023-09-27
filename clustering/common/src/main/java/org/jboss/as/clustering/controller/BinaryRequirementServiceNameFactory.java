/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;


import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.service.BinaryRequirement;

/**
 * Factory for generating a {@link ServiceName} for a {@link BinaryRequirement}.
 * @author Paul Ferraro
 */
public class BinaryRequirementServiceNameFactory implements BinaryServiceNameFactory {

    private final BinaryRequirement requirement;

    public BinaryRequirementServiceNameFactory(BinaryRequirement requirement) {
        this.requirement = requirement;
    }

    @Override
    public ServiceName getServiceName(OperationContext context, String parent, String child) {
        return context.getCapabilityServiceName(this.requirement.getName(), this.requirement.getType(), parent, child);
    }

    @Override
    public ServiceName getServiceName(CapabilityServiceSupport support, String parent, String child) {
        return support.getCapabilityServiceName(this.requirement.getName(), parent, child);
    }
}

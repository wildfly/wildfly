/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.service.Requirement;

/**
 * @author Paul Ferraro
 */
public class RequirementServiceNameFactory implements ServiceNameFactory {

    private final Requirement requirement;

    public RequirementServiceNameFactory(Requirement requirement) {
        this.requirement = requirement;
    }

    @Override
    public ServiceName getServiceName(OperationContext context) {
        return context.getCapabilityServiceName(this.requirement.getName(), this.requirement.getType());
    }

    @Override
    public ServiceName getServiceName(CapabilityServiceSupport support) {
        return support.getCapabilityServiceName(this.requirement.getName());
    }
}

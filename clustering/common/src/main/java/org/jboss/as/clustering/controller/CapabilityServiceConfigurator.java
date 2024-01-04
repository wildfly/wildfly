/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.wildfly.clustering.service.ServiceConfigurator;

/**
 * @author Paul Ferraro
 */
public interface CapabilityServiceConfigurator extends ServiceConfigurator {

    default ServiceConfigurator configure(CapabilityServiceSupport support) {
        return this;
    }

    default ServiceConfigurator configure(OperationContext context) {
        return this.configure(context.getCapabilityServiceSupport());
    }
}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.server.service;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.wildfly.clustering.service.ServiceConfigurator;

/**
 * @author Paul Ferraro
 */
public interface GroupServiceConfiguratorProvider {
    Iterable<ServiceConfigurator> getServiceConfigurators(CapabilityServiceSupport support, String group);

    default Iterable<ServiceConfigurator> getServiceConfigurators(OperationContext context, String group) {
        return this.getServiceConfigurators(context.getCapabilityServiceSupport(), group);
    }
}

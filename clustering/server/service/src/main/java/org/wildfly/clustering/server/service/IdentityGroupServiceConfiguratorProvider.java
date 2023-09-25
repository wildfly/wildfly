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
public interface IdentityGroupServiceConfiguratorProvider {
    Iterable<ServiceConfigurator> getServiceConfigurators(CapabilityServiceSupport support, String group, String targetGroup);

    default Iterable<ServiceConfigurator> getServiceConfigurators(OperationContext context, String group, String targetGroup) {
        return this.getServiceConfigurators(context.getCapabilityServiceSupport(), group, targetGroup);
    }
}

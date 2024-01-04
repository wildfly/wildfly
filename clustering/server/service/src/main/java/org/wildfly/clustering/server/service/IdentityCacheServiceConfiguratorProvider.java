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
public interface IdentityCacheServiceConfiguratorProvider {
    Iterable<ServiceConfigurator> getServiceConfigurators(CapabilityServiceSupport support, String containerName, String cacheName, String targetCacheName);

    default Iterable<ServiceConfigurator> getServiceConfigurators(OperationContext context, String containerName, String cacheName, String targetCacheName) {
        return this.getServiceConfigurators(context.getCapabilityServiceSupport(), containerName, cacheName, targetCacheName);
    }
}

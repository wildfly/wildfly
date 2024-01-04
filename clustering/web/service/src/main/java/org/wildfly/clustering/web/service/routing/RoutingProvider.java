/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.service.routing;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.wildfly.clustering.service.SupplierDependency;

/**
 * Defines a routing provider.
 * @author Paul Ferraro
 */
public interface RoutingProvider {

    /**
     * Builds the server dependencies to be made available to every deployment.
     * @param serverName the name of the server
     * @param route the distinct route of the server
     * @return a service builder
     */
    Iterable<CapabilityServiceConfigurator> getServiceConfigurators(String serverName, SupplierDependency<String> route);
}

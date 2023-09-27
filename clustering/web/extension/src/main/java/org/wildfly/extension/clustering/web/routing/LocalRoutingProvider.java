/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web.routing;

import java.util.Collections;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.wildfly.clustering.service.SupplierDependency;
import org.wildfly.clustering.web.service.routing.RoutingProvider;

/**
 * @author Paul Ferraro
 */
public class LocalRoutingProvider implements RoutingProvider {

    @Override
    public Iterable<CapabilityServiceConfigurator> getServiceConfigurators(String serverName, SupplierDependency<String> route) {
        return Collections.singleton(new LocalRouteServiceConfigurator(serverName, route));
    }
}

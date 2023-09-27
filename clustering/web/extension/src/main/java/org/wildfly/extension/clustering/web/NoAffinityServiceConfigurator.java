/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web;

import org.jboss.as.controller.PathAddress;
import org.wildfly.clustering.web.service.routing.RouteLocatorServiceConfiguratorFactory;
import org.wildfly.extension.clustering.web.routing.NullRouteLocatorServiceConfiguratorFactory;

/**
 * @author Paul Ferraro
 */
public class NoAffinityServiceConfigurator<C> extends AffinityServiceConfigurator<C> {

    public NoAffinityServiceConfigurator(PathAddress address) {
        super(NoAffinityResourceDefinition.Capability.AFFINITY, address);
    }

    @Override
    public RouteLocatorServiceConfiguratorFactory<C> get() {
        return new NullRouteLocatorServiceConfiguratorFactory<>();
    }
}

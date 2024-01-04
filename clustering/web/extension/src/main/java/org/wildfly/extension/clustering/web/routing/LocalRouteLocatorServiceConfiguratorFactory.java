/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web.routing;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.wildfly.clustering.web.WebDeploymentConfiguration;
import org.wildfly.clustering.web.service.routing.RouteLocatorServiceConfiguratorFactory;

/**
 * Factory for creating a service configurator for a local route locator.
 * @author Paul Ferraro
 */
public class LocalRouteLocatorServiceConfiguratorFactory<C> implements RouteLocatorServiceConfiguratorFactory<C> {

    @Override
    public CapabilityServiceConfigurator createRouteLocatorServiceConfigurator(C configuration, WebDeploymentConfiguration deploymentConfiguration) {
        return new LocalRouteLocatorServiceConfigurator(deploymentConfiguration);
    }
}

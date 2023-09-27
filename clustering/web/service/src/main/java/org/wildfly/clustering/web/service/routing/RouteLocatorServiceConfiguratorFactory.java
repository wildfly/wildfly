/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.service.routing;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.wildfly.clustering.web.WebDeploymentConfiguration;

/**
 * Factory for creating a service configurator for a route locator.
 * @author Paul Ferraro
 */
public interface RouteLocatorServiceConfiguratorFactory<C> {
    CapabilityServiceConfigurator createRouteLocatorServiceConfigurator(C configuration, WebDeploymentConfiguration deploymentConfiguration);
}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web.routing.infinispan;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.wildfly.clustering.ee.infinispan.InfinispanCacheConfiguration;
import org.wildfly.clustering.web.WebDeploymentConfiguration;
import org.wildfly.clustering.web.infinispan.routing.RankedRoutingConfiguration;
import org.wildfly.clustering.web.service.routing.RouteLocatorServiceConfiguratorFactory;

/**
 * Factory for creating a service configurator for a ranked route locator.
 * @author Paul Ferraro
 */
public class RankedRouteLocatorServiceConfiguratorFactory<C extends InfinispanCacheConfiguration> implements RouteLocatorServiceConfiguratorFactory<C> {

    private final RankedRoutingConfiguration config;

    public RankedRouteLocatorServiceConfiguratorFactory(RankedRoutingConfiguration config) {
        this.config = config;
    }

    @Override
    public CapabilityServiceConfigurator createRouteLocatorServiceConfigurator(C configuration, WebDeploymentConfiguration deploymentConfiguration) {
        return new RankedRouteLocatorServiceConfigurator(configuration, deploymentConfiguration, this.config);
    }

    public RankedRoutingConfiguration getConfiguration() {
        return this.config;
    }
}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web.routing.infinispan;

import org.wildfly.clustering.ee.infinispan.InfinispanCacheConfiguration;
import org.wildfly.clustering.web.WebDeploymentConfiguration;
import org.wildfly.clustering.web.infinispan.routing.RankedRouteLocator;
import org.wildfly.clustering.web.infinispan.routing.RankedRouteLocatorConfiguration;
import org.wildfly.clustering.web.infinispan.routing.RankedRoutingConfiguration;
import org.wildfly.clustering.web.routing.RouteLocator;

/**
 * Configures a service providing a ranked route locator.
 * @author Paul Ferraro
 */
public class RankedRouteLocatorServiceConfigurator extends PrimaryOwnerRouteLocatorServiceConfigurator implements RankedRouteLocatorConfiguration {

    private final RankedRoutingConfiguration config;

    public RankedRouteLocatorServiceConfigurator(InfinispanCacheConfiguration configuration, WebDeploymentConfiguration deploymentConfiguration, RankedRoutingConfiguration routeConfiguration) {
        super(configuration, deploymentConfiguration);
        this.config = routeConfiguration;
    }

    @Override
    public RouteLocator get() {
        return new RankedRouteLocator(this);
    }

    @Override
    public String getDelimiter() {
        return this.config.getDelimiter();
    }

    @Override
    public int getMaxRoutes() {
        return this.config.getMaxRoutes();
    }
}

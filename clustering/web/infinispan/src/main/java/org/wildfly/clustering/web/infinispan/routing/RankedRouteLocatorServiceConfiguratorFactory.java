/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.clustering.web.infinispan.routing;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.wildfly.clustering.web.WebDeploymentConfiguration;
import org.wildfly.clustering.web.infinispan.session.InfinispanSessionManagementConfiguration;
import org.wildfly.clustering.web.routing.RouteLocatorServiceConfiguratorFactory;

/**
 * Factory for creating a service configurator for a ranked route locator.
 * @author Paul Ferraro
 */
public class RankedRouteLocatorServiceConfiguratorFactory implements RouteLocatorServiceConfiguratorFactory<InfinispanSessionManagementConfiguration> {

    private final RankedRoutingConfiguration config;

    public RankedRouteLocatorServiceConfiguratorFactory(RankedRoutingConfiguration config) {
        this.config = config;
    }

    @Override
    public CapabilityServiceConfigurator createRouteLocatorServiceConfigurator(InfinispanSessionManagementConfiguration managementConfiguration, WebDeploymentConfiguration deploymentConfiguration) {
        return new RankedRouteLocatorServiceConfigurator(managementConfiguration, deploymentConfiguration, this.config);
    }

    public RankedRoutingConfiguration getConfiguration() {
        return this.config;
    }
}

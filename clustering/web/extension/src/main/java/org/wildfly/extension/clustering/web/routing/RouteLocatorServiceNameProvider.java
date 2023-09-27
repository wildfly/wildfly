/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web.routing;

import org.jboss.as.controller.ServiceNameFactory;
import org.wildfly.clustering.service.SimpleServiceNameProvider;
import org.wildfly.clustering.web.WebDeploymentConfiguration;
import org.wildfly.clustering.web.service.WebDeploymentRequirement;

/**
 * Service name provider for a route locator service.
 * @author Paul Ferraro
 */
public class RouteLocatorServiceNameProvider extends SimpleServiceNameProvider {

    public RouteLocatorServiceNameProvider(WebDeploymentConfiguration configuration) {
        super(ServiceNameFactory.parseServiceName(WebDeploymentRequirement.ROUTE_LOCATOR.getName()).append(configuration.getDeploymentName()));
    }
}

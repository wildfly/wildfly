/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web.routing;

import org.wildfly.clustering.cache.function.Functions;
import org.wildfly.clustering.server.service.BinaryServiceConfiguration;
import org.wildfly.clustering.web.service.deployment.WebDeploymentConfiguration;
import org.wildfly.clustering.web.service.deployment.WebDeploymentServiceDescriptor;
import org.wildfly.clustering.web.service.routing.RouteLocatorProvider;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * @author Paul Ferraro
 */
public class LocalRouteLocatorProvider implements RouteLocatorProvider {

    @Override
    public ServiceInstaller getServiceInstaller(BinaryServiceConfiguration configuration, WebDeploymentConfiguration deployment) {
        ServiceDependency<String> localRoute = ServiceDependency.on(LocalRoutingProvider.LOCAL_ROUTE, deployment.getServerName());
        return ServiceInstaller.builder(localRoute.map(Functions::constantOperator))
                .provides(WebDeploymentServiceDescriptor.ROUTE_LOCATOR.resolve(deployment.getDeploymentUnit()))
                .build();
    }
}

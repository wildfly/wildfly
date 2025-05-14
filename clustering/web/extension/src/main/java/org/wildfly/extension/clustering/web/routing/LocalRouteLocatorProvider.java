/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web.routing;

import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.wildfly.clustering.function.UnaryOperator;
import org.wildfly.clustering.server.deployment.DeploymentConfiguration;
import org.wildfly.clustering.server.service.BinaryServiceConfiguration;
import org.wildfly.clustering.web.service.routing.RouteLocatorProvider;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * @author Paul Ferraro
 */
public class LocalRouteLocatorProvider implements RouteLocatorProvider {

    @Override
    public ServiceInstaller getServiceInstaller(DeploymentPhaseContext context, BinaryServiceConfiguration configuration, DeploymentConfiguration deployment) {
        ServiceDependency<String> route = ServiceDependency.on(LocalRoutingProvider.LOCAL_ROUTE, deployment.getServerName());
        return RouteLocatorProvider.builder(route.map(UnaryOperator::of), deployment).requires(route).build();
    }
}

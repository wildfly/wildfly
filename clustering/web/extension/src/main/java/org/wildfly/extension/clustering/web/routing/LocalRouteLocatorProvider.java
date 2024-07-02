/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web.routing;

import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.wildfly.clustering.cache.function.Functions;
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
        ServiceDependency<String> localRoute = ServiceDependency.on(LocalRoutingProvider.LOCAL_ROUTE, deployment.getServerName());
        Supplier<UnaryOperator<String>> factory = new Supplier<>() {
            @Override
            public UnaryOperator<String> get() {
                return Functions.constantOperator(localRoute.get());
            }
        };
        return RouteLocatorProvider.builder(factory, deployment).requires(localRoute).build();
    }
}

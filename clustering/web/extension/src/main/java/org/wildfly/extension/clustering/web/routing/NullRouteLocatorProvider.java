/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web.routing;

import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import org.wildfly.clustering.cache.function.Functions;
import org.wildfly.clustering.server.service.BinaryServiceConfiguration;
import org.wildfly.clustering.web.service.deployment.WebDeploymentConfiguration;
import org.wildfly.clustering.web.service.deployment.WebDeploymentServiceDescriptor;
import org.wildfly.clustering.web.service.routing.RouteLocatorProvider;
import org.wildfly.subsystem.service.DeploymentServiceInstaller;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * @author Paul Ferraro
 */
public class NullRouteLocatorProvider implements RouteLocatorProvider {

    @Override
    public DeploymentServiceInstaller getServiceInstaller(BinaryServiceConfiguration configuration, WebDeploymentConfiguration deployment) {
        Supplier<UnaryOperator<String>> factory = Functions::nullOperator;
        return ServiceInstaller.builder(factory)
                .provides(WebDeploymentServiceDescriptor.ROUTE_LOCATOR.resolve(deployment.getDeploymentUnit()))
                .build();
    }
}

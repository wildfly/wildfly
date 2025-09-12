/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web.routing;

import org.wildfly.clustering.function.UnaryOperator;
import org.wildfly.clustering.server.service.BinaryServiceConfiguration;
import org.wildfly.clustering.web.service.deployment.WebDeploymentConfiguration;
import org.wildfly.clustering.web.service.deployment.WebDeploymentServiceDescriptor;
import org.wildfly.clustering.web.service.routing.RouteLocatorProvider;
import org.wildfly.service.Installer.StartWhen;
import org.wildfly.subsystem.service.DeploymentServiceInstaller;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * @author Paul Ferraro
 */
public class NullRouteLocatorProvider implements RouteLocatorProvider {
    @Override
    public DeploymentServiceInstaller getServiceInstaller(BinaryServiceConfiguration configuration, WebDeploymentConfiguration deployment) {
        return ServiceInstaller.builder(UnaryOperator.empty())
                .provides(WebDeploymentServiceDescriptor.ROUTE_LOCATOR.resolve(deployment.getDeploymentUnit()))
                .startWhen(StartWhen.REQUIRED)
                .build();
    }
}

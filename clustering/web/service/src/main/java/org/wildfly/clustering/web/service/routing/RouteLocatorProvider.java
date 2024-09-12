/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.service.routing;

import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import org.jboss.as.controller.ServiceNameFactory;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.wildfly.clustering.server.deployment.DeploymentConfiguration;
import org.wildfly.clustering.server.service.BinaryServiceConfiguration;
import org.wildfly.clustering.web.service.WebDeploymentServiceDescriptor;
import org.wildfly.service.descriptor.UnaryServiceDescriptor;
import org.wildfly.subsystem.service.DeploymentServiceInstaller;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * Factory for creating a service configurator for a route locator.
 * @author Paul Ferraro
 */
public interface RouteLocatorProvider {
    UnaryServiceDescriptor<RouteLocatorProvider> SERVICE_DESCRIPTOR = UnaryServiceDescriptor.of("org.wildfly.clustering.web.session-affinity", RouteLocatorProvider.class);

    DeploymentServiceInstaller getServiceInstaller(DeploymentPhaseContext context, BinaryServiceConfiguration configuration, DeploymentConfiguration deployment);

    static ServiceInstaller.UnaryBuilder<UnaryOperator<String>, UnaryOperator<String>> builder(Supplier<UnaryOperator<String>> factory, DeploymentConfiguration deployment) {
        return ServiceInstaller.builder(factory).provides(ServiceNameFactory.resolveServiceName(WebDeploymentServiceDescriptor.ROUTE_LOCATOR, deployment.getDeploymentName()));
    }
}

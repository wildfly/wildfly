/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web.routing;

import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.wildfly.clustering.function.Supplier;
import org.wildfly.clustering.function.UnaryOperator;
import org.wildfly.clustering.server.deployment.DeploymentConfiguration;
import org.wildfly.clustering.server.service.BinaryServiceConfiguration;
import org.wildfly.clustering.web.service.routing.RouteLocatorProvider;
import org.wildfly.subsystem.service.DeploymentServiceInstaller;

/**
 * @author Paul Ferraro
 */
public class NullRouteLocatorProvider implements RouteLocatorProvider {
    @Override
    public DeploymentServiceInstaller getServiceInstaller(DeploymentPhaseContext context, BinaryServiceConfiguration configuration, DeploymentConfiguration deployment) {
        return RouteLocatorProvider.builder(Supplier.<String>of(null).map(UnaryOperator::of), deployment).build();
    }
}

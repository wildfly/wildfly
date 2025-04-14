/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.service.routing;

import org.wildfly.clustering.server.service.BinaryServiceConfiguration;
import org.wildfly.clustering.web.service.deployment.WebDeploymentConfiguration;
import org.wildfly.service.descriptor.UnaryServiceDescriptor;
import org.wildfly.subsystem.service.DeploymentServiceInstaller;

/**
 * Provides a service installer for the route locator of a deployment.
 * @author Paul Ferraro
 */
public interface RouteLocatorProvider {
    UnaryServiceDescriptor<RouteLocatorProvider> SERVICE_DESCRIPTOR = UnaryServiceDescriptor.of("org.wildfly.clustering.web.session-affinity", RouteLocatorProvider.class);

    /**
     * Returns an installer of a service providing a {@link RouteLocatorProvider}.
     * @param configuration a cache configuration
     * @param deployment a web deployment configuration
     * @return an installer of a service providing a {@link RouteLocatorProvider}.
     */
    DeploymentServiceInstaller getServiceInstaller(BinaryServiceConfiguration configuration, WebDeploymentConfiguration deployment);
}

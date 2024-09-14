/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.service.routing;

import org.wildfly.service.descriptor.NullaryServiceDescriptor;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * Defines a routing provider.
 * @author Paul Ferraro
 */
public interface RoutingProvider {
    NullaryServiceDescriptor<RoutingProvider> SERVICE_DESCRIPTOR = NullaryServiceDescriptor.of("org.wildfly.clustering.web.routing-provider", RoutingProvider.class);
    NullaryServiceDescriptor<RoutingProvider> INFINISPAN_SERVICE_DESCRIPTOR = NullaryServiceDescriptor.of("org.wildfly.clustering.web.infinispan-routing-provider", RoutingProvider.class);

    /**
     * Builds the server dependencies to be made available to every deployment.
     * @param serverName the name of the server
     * @param route the distinct route of the server
     * @return a service builder
     */
    Iterable<ServiceInstaller> getServiceInstallers(String serverName, ServiceDependency<String> route);
}

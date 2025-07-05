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
     * Returns a number of installers of services, one of which will provide a {@link RoutingProvider} for the server identified by the specified name.
     * @param serverName the name of the server
     * @param route a dependency on a service providing the server route.
     * @return a number of service installers
     */
    Iterable<ServiceInstaller> getServiceInstallers(String serverName, ServiceDependency<String> route);
}

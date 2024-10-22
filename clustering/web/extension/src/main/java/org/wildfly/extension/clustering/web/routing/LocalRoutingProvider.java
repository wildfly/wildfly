/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web.routing;

import java.util.List;

import org.jboss.as.controller.ServiceNameFactory;
import org.wildfly.clustering.web.service.routing.RoutingProvider;
import org.wildfly.service.descriptor.UnaryServiceDescriptor;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * @author Paul Ferraro
 */
public class LocalRoutingProvider implements RoutingProvider {
    static final UnaryServiceDescriptor<String> LOCAL_ROUTE = UnaryServiceDescriptor.of("org.wildfly.clustering.web.local-route", String.class);

    @Override
    public Iterable<ServiceInstaller> getServiceInstallers(String serverName, ServiceDependency<String> route) {
        return List.of(ServiceInstaller.builder(route).provides(ServiceNameFactory.resolveServiceName(LOCAL_ROUTE, serverName)).build());
    }
}

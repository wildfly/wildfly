/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.clustering.web;

import java.util.function.Supplier;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.web.service.routing.RouteLocatorProvider;
import org.wildfly.clustering.web.service.routing.RoutingProvider;
import org.wildfly.extension.clustering.web.routing.LocalRouteLocatorProvider;

/**
 * Registers a resource definition for a local affinity resource.
 * @author Paul Ferraro
 */
public class LocalAffinityResourceDefinitionRegistrar extends AffinityResourceDefinitionRegistrar {

    LocalAffinityResourceDefinitionRegistrar() {
        super(AffinityResourceRegistration.LOCAL, RoutingProvider.SERVICE_DESCRIPTOR);
    }

    @Override
    public Supplier<RouteLocatorProvider> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        return LocalRouteLocatorProvider::new;
    }
}

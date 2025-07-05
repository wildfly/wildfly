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
import org.wildfly.extension.clustering.web.routing.infinispan.PrimaryOwnerRouteLocatorProvider;

/**
 * Registers a resource definition for a primary-owner affinity resource.
 * @author Paul Ferraro
 */
public class PrimaryOwnerAffinityResourceDefinitionRegistrar extends AffinityResourceDefinitionRegistrar {

    PrimaryOwnerAffinityResourceDefinitionRegistrar() {
        super(AffinityResourceRegistration.PRIMARY_OWNER, RoutingProvider.INFINISPAN_SERVICE_DESCRIPTOR);
    }

    @Override
    public Supplier<RouteLocatorProvider> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        return PrimaryOwnerRouteLocatorProvider::new;
    }
}

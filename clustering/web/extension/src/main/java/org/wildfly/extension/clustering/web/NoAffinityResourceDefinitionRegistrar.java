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
import org.wildfly.extension.clustering.web.routing.NullRouteLocatorProvider;

/**
 * Registers a resource definition for a "no" affinity resource.
 * @author Paul Ferraro
 */
public class NoAffinityResourceDefinitionRegistrar extends AffinityResourceDefinitionRegistrar {

    NoAffinityResourceDefinitionRegistrar() {
        super(AffinityResourceRegistration.NONE);
    }

    @Override
    public Supplier<RouteLocatorProvider> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        return NullRouteLocatorProvider::new;
    }
}

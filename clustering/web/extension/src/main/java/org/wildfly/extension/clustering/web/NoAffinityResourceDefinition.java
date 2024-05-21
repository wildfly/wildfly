/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web;

import java.util.function.UnaryOperator;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.capability.UnaryCapabilityNameResolver;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.web.service.routing.RouteLocatorProvider;
import org.wildfly.extension.clustering.web.routing.NullRouteLocatorProvider;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.capability.CapabilityServiceInstaller;

/**
 * @author Paul Ferraro
 */
public class NoAffinityResourceDefinition extends AffinityResourceDefinition {

    static final PathElement PATH = pathElement("none");

    static final RuntimeCapability<Void> CAPABILITY = RuntimeCapability.Builder.of(RouteLocatorProvider.SERVICE_DESCRIPTOR)
            .setAllowMultipleRegistrations(true)
            .setDynamicNameMapper(UnaryCapabilityNameResolver.PARENT)
            .build();

    NoAffinityResourceDefinition() {
        super(PATH, CAPABILITY, UnaryOperator.identity());
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        return CapabilityServiceInstaller.builder(CAPABILITY, new NullRouteLocatorProvider()).build();
    }
}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web;

import java.util.List;
import java.util.function.UnaryOperator;

import org.jboss.as.clustering.controller.ChildResourceDefinition;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.SimpleResourceRegistrar;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.wildfly.clustering.web.service.routing.RoutingProvider;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;
import org.wildfly.subsystem.service.ResourceServiceConfigurator;

/**
 * Base definition for routing provider resources.
 * @author Paul Ferraro
 */
public abstract class RoutingProviderResourceDefinition extends ChildResourceDefinition<ManagementResourceRegistration> implements ResourceServiceConfigurator {

    static PathElement pathElement(String value) {
        return PathElement.pathElement("routing", value);
    }

    static final RuntimeCapability<Void> ROUTING_PROVIDER = RuntimeCapability.Builder.of(RoutingProvider.SERVICE_DESCRIPTOR).setAllowMultipleRegistrations(true).build();

    private final UnaryOperator<ResourceDescriptor> configurator;

    RoutingProviderResourceDefinition(PathElement path, UnaryOperator<ResourceDescriptor> configurator) {
        super(path, DistributableWebExtension.SUBSYSTEM_RESOLVER.createChildResolver(pathElement(PathElement.WILDCARD_VALUE), path));
        this.configurator = configurator;
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent) {
        ManagementResourceRegistration registration = parent.registerSubModel(this);
        ResourceDescriptor descriptor = this.configurator.apply(new ResourceDescriptor(this.getResourceDescriptionResolver()))
                .addCapabilities(List.of(ROUTING_PROVIDER))
                ;
        ResourceServiceHandler handler = ResourceServiceHandler.of(ResourceOperationRuntimeHandler.configureService(this));
        new SimpleResourceRegistrar(descriptor, handler).register(registration);
        return registration;
    }
}

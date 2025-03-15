/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import org.infinispan.commons.configuration.Builder;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.wildfly.subsystem.resource.ChildResourceDefinitionRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrationContext;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.resource.ResourceModelResolver;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * Registers a component resource definition.
 * @author Paul Ferraro
 */
public abstract class ConfigurationResourceDefinitionRegistrar<C, B extends Builder<C>> implements ChildResourceDefinitionRegistrar, ResourceModelResolver<ServiceDependency<B>>, Supplier<ResourceOperationRuntimeHandler> {

    public interface Configurator extends UnaryOperator<ResourceDescriptor.Builder> {
        ResourceRegistration getResourceRegistration();

        RuntimeCapability<Void> getCapability();

        @Override
        default ResourceDescriptor.Builder apply(ResourceDescriptor.Builder builder) {
            return builder.addCapability(this.getCapability());
        }

        default InfinispanSubsystemModel getDeprecation() {
            return null;
        }
    }

    private final Configurator configurator;
    private final ResourceOperationRuntimeHandler handler;

    public ConfigurationResourceDefinitionRegistrar(Configurator configurator) {
        this.configurator = configurator;
        this.handler = ResourceOperationRuntimeHandler.configureService(new ConfigurationResourceServiceConfigurator<>(this.configurator.getCapability(), this));
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent, ManagementResourceRegistrationContext context) {
        PathElement path = this.configurator.getResourceRegistration().getPathElement();
        ResourceDescriptionResolver resolver = InfinispanSubsystemResourceDefinitionRegistrar.RESOLVER.createChildResolver(path, PathElement.pathElement(path.getKey()));
        ResourceDescriptor descriptor = this.configurator.apply(ResourceDescriptor.builder(resolver))
                .withRuntimeHandler(this.get())
                .build();

        ResourceDefinition definition = ResourceDefinition.builder(this.configurator.getResourceRegistration(), resolver, this.configurator.getDeprecation()).build();
        ManagementResourceRegistration registration = parent.registerSubModel(definition);

        ManagementResourceRegistrar.of(descriptor).register(registration);

        return registration;
    }

    @Override
    public ResourceOperationRuntimeHandler get() {
        return this.handler;
    }
}

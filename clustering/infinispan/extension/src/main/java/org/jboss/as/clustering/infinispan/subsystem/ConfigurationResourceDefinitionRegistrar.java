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
 * Registers a resource definition for an Infinispan configuration component.
 * @author Paul Ferraro
 * @param <C> the configuration type
 * @param <B> the configuration builder type
 */
public abstract class ConfigurationResourceDefinitionRegistrar<C, B extends Builder<C>> implements ChildResourceDefinitionRegistrar, ResourceModelResolver<ServiceDependency<B>>, UnaryOperator<ResourceDescriptor.Builder>, Supplier<ResourceOperationRuntimeHandler> {

    public interface Configurator<C> extends ConfigurationResourceServiceConfigurator.Configurator<C> {
        ResourceRegistration getResourceRegistration();

        default InfinispanSubsystemModel getDeprecation() {
            return null;
        }
    }

    private final ResourceRegistration registration;
    private final RuntimeCapability<Void> capability;
    private final ResourceOperationRuntimeHandler runtimeHandler;
    private final InfinispanSubsystemModel deprecation;

    public ConfigurationResourceDefinitionRegistrar(Configurator<C> configurator) {
        this.registration = configurator.getResourceRegistration();
        this.capability = configurator.getCapability();
        this.runtimeHandler = ResourceOperationRuntimeHandler.configureService(new ConfigurationResourceServiceConfigurator<>(configurator, this));
        this.deprecation = configurator.getDeprecation();
    }

    @Override
    public ResourceDescriptor.Builder apply(ResourceDescriptor.Builder builder) {
        return builder.addCapability(this.capability).withRuntimeHandler(this.get());
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent, ManagementResourceRegistrationContext context) {
        PathElement path = this.registration.getPathElement();
        ResourceDescriptionResolver resolver = InfinispanSubsystemResourceDefinitionRegistrar.RESOLVER.createChildResolver(path, PathElement.pathElement(path.getKey()));
        ResourceDescriptor descriptor = this.apply(ResourceDescriptor.builder(resolver)).build();
        ResourceDefinition definition = ResourceDefinition.builder(this.registration, resolver, this.deprecation).build();

        ManagementResourceRegistration registration = parent.registerSubModel(definition);

        ManagementResourceRegistrar.of(descriptor).register(registration);

        return registration;
    }

    @Override
    public ResourceOperationRuntimeHandler get() {
        return this.runtimeHandler;
    }
}

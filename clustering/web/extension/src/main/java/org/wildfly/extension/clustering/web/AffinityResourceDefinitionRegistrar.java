/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.clustering.web;

import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.capability.UnaryCapabilityNameResolver;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.web.service.routing.RouteLocatorProvider;
import org.wildfly.clustering.web.service.routing.RoutingProvider;
import org.wildfly.service.descriptor.NullaryServiceDescriptor;
import org.wildfly.subsystem.resource.ChildResourceDefinitionRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrationContext;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.resource.ResourceModelResolver;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;
import org.wildfly.subsystem.service.ResourceServiceConfigurator;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.capability.CapabilityServiceInstaller;

/**
 * Registers a resource definition for an affinity resource.
 * @author Paul Ferraro
 */
public abstract class AffinityResourceDefinitionRegistrar implements ChildResourceDefinitionRegistrar, ResourceServiceConfigurator, ResourceModelResolver<Supplier<RouteLocatorProvider>>, UnaryOperator<ResourceDescriptor.Builder> {

    private final ResourceRegistration registration;
    private final RuntimeCapability<Void> capability;

    AffinityResourceDefinitionRegistrar(ResourceRegistration registration) {
        this(registration, UnaryOperator.identity());
    }

    AffinityResourceDefinitionRegistrar(ResourceRegistration registration, NullaryServiceDescriptor<RoutingProvider> routingProviderRequirement) {
        this(registration, builder -> builder.addRequirements(routingProviderRequirement));
    }

    private AffinityResourceDefinitionRegistrar(ResourceRegistration registration, UnaryOperator<RuntimeCapability.Builder<Void>> configurator) {
        this.registration = registration;
        this.capability = configurator.apply(RuntimeCapability.Builder.of(RouteLocatorProvider.SERVICE_DESCRIPTOR))
                .setAllowMultipleRegistrations(true)
                .setDynamicNameMapper(UnaryCapabilityNameResolver.PARENT)
                .build();
    }

    @Override
    public ResourceDescriptor.Builder apply(ResourceDescriptor.Builder builder) {
        return builder.addCapability(this.capability)
                .withRuntimeHandler(ResourceOperationRuntimeHandler.configureService(this))
                ;
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent, ManagementResourceRegistrationContext context) {
        ResourceDescriptionResolver resolver = DistributableWebSubsystemResourceDefinitionRegistrar.RESOLVER.createChildResolver(this.registration.getPathElement(), AffinityResourceRegistration.WILDCARD.getPathElement());
        ResourceDescriptor descriptor = this.apply(ResourceDescriptor.builder(resolver)).build();
        ManagementResourceRegistration registration = parent.registerSubModel(ResourceDefinition.builder(this.registration, resolver).build());
        ManagementResourceRegistrar.of(descriptor).register(registration);
        return registration;
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        return CapabilityServiceInstaller.builder(this.capability, this.resolve(context, model)).build();
    }
}

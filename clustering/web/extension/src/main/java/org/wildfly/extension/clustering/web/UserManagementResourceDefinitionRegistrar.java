/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.clustering.web;

import java.util.function.Function;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.server.service.BinaryServiceConfiguration;
import org.wildfly.clustering.server.service.CacheConfigurationAttributeGroup;
import org.wildfly.clustering.web.service.user.DistributableUserManagementProvider;
import org.wildfly.subsystem.resource.ChildResourceDefinitionRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrationContext;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;
import org.wildfly.subsystem.service.ResourceServiceConfigurator;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.capability.CapabilityServiceInstaller;

/**
 * Registers a resource definition for a user management provider.
 * @author Paul Ferraro
 */
public abstract class UserManagementResourceDefinitionRegistrar implements ChildResourceDefinitionRegistrar, ResourceServiceConfigurator, Function<BinaryServiceConfiguration, DistributableUserManagementProvider> {
    static final RuntimeCapability<Void> CAPABILITY = RuntimeCapability.Builder.of(DistributableUserManagementProvider.SERVICE_DESCRIPTOR)
            .setAllowMultipleRegistrations(true)
            .build();

    private final ResourceRegistration registration;
    private final CacheConfigurationAttributeGroup cacheAttributeGroup;

    UserManagementResourceDefinitionRegistrar(ResourceRegistration registration, CacheConfigurationAttributeGroup cacheAttributeGroup) {
        this.registration = registration;
        this.cacheAttributeGroup = cacheAttributeGroup;
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent, ManagementResourceRegistrationContext context) {
        ResourceDescriptionResolver resolver = DistributableWebSubsystemResourceDefinitionRegistrar.RESOLVER.createChildResolver(this.registration.getPathElement());
        ResourceDescriptor descriptor = ResourceDescriptor.builder(resolver)
                .addAttributes(this.cacheAttributeGroup.getAttributes())
                .addCapability(CAPABILITY)
                .withRuntimeHandler(ResourceOperationRuntimeHandler.configureService(this))
                .build();
        ManagementResourceRegistration registration = parent.registerSubModel(ResourceDefinition.builder(this.registration, descriptor.getResourceDescriptionResolver()).build());
        ManagementResourceRegistrar.of(descriptor).register(registration);
        return registration;
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        BinaryServiceConfiguration configuration = this.cacheAttributeGroup.resolve(context, model);
        return CapabilityServiceInstaller.builder(CAPABILITY, this.apply(configuration)).build();
    }
}

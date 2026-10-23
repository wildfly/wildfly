/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.clustering.ejb;

import java.util.function.UnaryOperator;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.registry.AliasEntry;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.ejb.remote.EjbClientServicesProvider;
import org.wildfly.clustering.function.Supplier;
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
 * Registers a resource definition for a client-mappings registry provider.
 * @author Paul Ferraro
 * @author Richard Achmatowicz
 */
public abstract class EjbClientServicesProviderResourceDefinitionRegistrar implements ChildResourceDefinitionRegistrar, ResourceServiceConfigurator, ResourceModelResolver<EjbClientServicesProvider>, UnaryOperator<ResourceDescriptor.Builder> {

    static final RuntimeCapability<Void> CAPABILITY = RuntimeCapability.Builder.of(EjbClientServicesProvider.SERVICE_DESCRIPTOR).setAllowMultipleRegistrations(true).build();
    // Address key used by this resource prior to DistributableEjbSubsystemModel.VERSION_3_0_0
    static final String LEGACY_PATH_KEY = "client-mappings-registry";

    private final ResourceRegistration registration;

    EjbClientServicesProviderResourceDefinitionRegistrar(EjbClientServicesProviderResourceRegistration registration) {
        this.registration = registration;
    }

    @Override
    public ResourceDescriptor.Builder apply(ResourceDescriptor.Builder builder) {
        return builder.addCapability(CAPABILITY)
                .withRuntimeHandler(ResourceOperationRuntimeHandler.configureService(this))
                ;
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent, ManagementResourceRegistrationContext context) {
        ResourceDescriptionResolver resolver = DistributableEjbSubsystemResourceDefinitionRegistrar.RESOLVER.createChildResolver(this.registration.getPathElement(), EjbClientServicesProviderResourceRegistration.WILDCARD.getPathElement());
        ResourceDescriptor descriptor = this.apply(ResourceDescriptor.builder(resolver)).build();
        ManagementResourceRegistration registration = parent.registerSubModel(ResourceDefinition.builder(this.registration, descriptor.getResourceDescriptionResolver()).build());
        // register an alias from the legacy resource name client-mappings-registry to this resource
        PathElement target = this.registration.getPathElement();
        parent.registerAlias(PathElement.pathElement(LEGACY_PATH_KEY, target.getValue()), new AliasEntry(registration) {
            @Override
            public PathAddress convertToTargetAddress(PathAddress aliasAddress, AliasContext aliasContext) {
                return aliasAddress.getParent().append(target);
            }
        });
        ManagementResourceRegistrar.of(descriptor).register(registration);
        return registration;
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        return CapabilityServiceInstaller.BlockingBuilder.of(CAPABILITY, Supplier.of(this.resolve(context, model))).build();
    }
}

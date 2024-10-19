/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import org.infinispan.commons.configuration.Builder;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.wildfly.subsystem.resource.ChildResourceDefinitionRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrationContext;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;

/**
 * Registers a component resource definition.
 * @author Paul Ferraro
 */
public class ComponentResourceDefinitionRegistrar<C, B extends Builder<C>> extends ComponentServiceConfigurator<C, B> implements ChildResourceDefinitionRegistrar {

    private final ComponentResourceDescription<C, B> description;

    public ComponentResourceDefinitionRegistrar(ComponentResourceDescription<C, B> description) {
        super(description);
        this.description = description;
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent, ManagementResourceRegistrationContext context) {
        PathElement path = this.description.getPathElement();
        ResourceDescriptionResolver resolver = InfinispanSubsystemResourceDefinitionRegistrar.RESOLVER.createChildResolver(path, PathElement.pathElement(path.getKey()));
        ResourceDescriptor descriptor = this.description.apply(ResourceDescriptor.builder(resolver))
                .withRuntimeHandler(ResourceOperationRuntimeHandler.configureService(this))
                .build();

        ResourceDefinition definition = ResourceDefinition.builder(this.description, resolver, this.description.getDeprecation()).build();
        ManagementResourceRegistration registration = parent.registerSubModel(definition);

        ManagementResourceRegistrar.of(descriptor).register(registration);

        return registration;
    }
}

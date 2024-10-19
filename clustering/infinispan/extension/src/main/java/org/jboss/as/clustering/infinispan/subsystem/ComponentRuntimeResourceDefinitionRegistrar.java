/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.wildfly.subsystem.resource.ChildResourceDefinitionRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrationContext;

/**
 * Registers a runtime resource definition for a component.
 * @author Paul Ferraro
 */
public class ComponentRuntimeResourceDefinitionRegistrar implements ChildResourceDefinitionRegistrar {

    private final ResourceRegistration registration;
    private final ManagementResourceRegistrar registrar;

    protected ComponentRuntimeResourceDefinitionRegistrar(ResourceRegistration registration, ManagementResourceRegistrar registrar) {
        this.registration = registration;
        this.registrar = registrar;
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent, ManagementResourceRegistrationContext context) {
        ResourceDescriptionResolver resolver = InfinispanSubsystemResourceDefinitionRegistrar.RESOLVER.createChildResolver(this.registration.getPathElement());
        ManagementResourceRegistration registration = parent.registerSubModel(ResourceDefinition.builder(this.registration, resolver).asRuntime().build());
        this.registrar.register(registration);
        return registration;
    }
}

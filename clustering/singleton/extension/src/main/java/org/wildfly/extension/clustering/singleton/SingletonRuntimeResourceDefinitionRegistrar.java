/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.clustering.singleton;

import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.wildfly.subsystem.resource.ChildResourceDefinitionRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrationContext;

/**
 * Registers a runtime resource definition for a singleton policy.
 * @author Paul Ferraro
 */
public class SingletonRuntimeResourceDefinitionRegistrar implements ChildResourceDefinitionRegistrar {

    private final ResourceRegistration registration;

    public SingletonRuntimeResourceDefinitionRegistrar(ResourceRegistration registration) {
        this.registration = registration;
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent, ManagementResourceRegistrationContext context) {
        ResourceDescriptionResolver resolver = SingletonSubsystemResourceDefinitionRegistrar.RESOLVER.createChildResolver(SingletonRuntimeResourceRegistration.DEPLOYMENT.getPathElement());
        return parent.registerSubModel(ResourceDefinition.builder(this.registration, resolver).asRuntime().build());
    }
}

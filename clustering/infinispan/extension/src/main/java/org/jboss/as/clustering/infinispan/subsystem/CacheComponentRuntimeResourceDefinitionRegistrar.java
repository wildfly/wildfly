/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import org.infinispan.Cache;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.wildfly.subsystem.resource.ChildResourceDefinitionRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrationContext;
import org.wildfly.subsystem.service.capture.FunctionExecutorRegistry;

/**
 * @author Paul Ferraro
 */
public class CacheComponentRuntimeResourceDefinitionRegistrar implements ChildResourceDefinitionRegistrar {

    private final CacheComponentRuntimeResourceDescription description;
    private final ManagementResourceRegistrar registrar;

    CacheComponentRuntimeResourceDefinitionRegistrar(CacheComponentRuntimeResourceDescription description, FunctionExecutorRegistry<Cache<?, ?>> registry) {
        this.description = description;
        this.registrar = description.apply(registry);
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent, ManagementResourceRegistrationContext context) {
        ResourceDescriptionResolver resolver = InfinispanSubsystemResourceDefinitionRegistrar.RESOLVER.createChildResolver(this.description.getPathElement());
        ManagementResourceRegistration registration = parent.registerSubModel(ResourceDefinition.builder(this.description, resolver).asRuntime().build());
        this.registrar.register(registration);
        return registration;
    }
}

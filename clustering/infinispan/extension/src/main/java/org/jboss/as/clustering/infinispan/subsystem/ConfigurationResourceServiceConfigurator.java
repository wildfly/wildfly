/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import org.infinispan.commons.configuration.Builder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.dmr.ModelNode;
import org.wildfly.subsystem.resource.ResourceModelResolver;
import org.wildfly.subsystem.service.ResourceServiceConfigurator;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.capability.CapabilityServiceInstaller;

/**
 * Configures a service that provides some Infinispan configuration.
 * @author Paul Ferraro
 * @param <C> the configuration type
 * @param <B> the configuration builder type
 */
public class ConfigurationResourceServiceConfigurator<C, B extends Builder<C>> implements ResourceServiceConfigurator {

    private final RuntimeCapability<Void> capability;
    private final ResourceModelResolver<ServiceDependency<B>> resolver;

    public ConfigurationResourceServiceConfigurator(RuntimeCapability<Void> capability, ResourceModelResolver<ServiceDependency<B>> resolver) {
        this.capability = capability;
        this.resolver = resolver;
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        return CapabilityServiceInstaller.builder(this.capability, this.resolver.resolve(context, model).map(Builder::create)).build();
    }
}

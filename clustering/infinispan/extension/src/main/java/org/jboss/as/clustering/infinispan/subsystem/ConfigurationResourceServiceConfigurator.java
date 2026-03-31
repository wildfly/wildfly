/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import java.util.function.UnaryOperator;

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

    public interface Configurator<C> extends UnaryOperator<CapabilityServiceInstaller.BlockingBuilder<C, C>> {
        RuntimeCapability<Void> getCapability();

        @Override
        default CapabilityServiceInstaller.BlockingBuilder<C, C> apply(CapabilityServiceInstaller.BlockingBuilder<C, C> builder) {
            return builder;
        }
    }

    private final ResourceModelResolver<ServiceDependency<B>> resolver;
    private final Configurator<C> configurator;

    public ConfigurationResourceServiceConfigurator(Configurator<C> configurator, ResourceModelResolver<ServiceDependency<B>> resolver) {
        this.configurator = configurator;
        this.resolver = resolver;
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        return this.configurator.apply(CapabilityServiceInstaller.BlockingBuilder.of(this.configurator.getCapability(), this.resolver.resolve(context, model).map(Builder::create))).build();
    }
}

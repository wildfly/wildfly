/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import java.util.List;

import org.infinispan.configuration.cache.PersistenceConfiguration;
import org.infinispan.configuration.cache.PersistenceConfigurationBuilder;
import org.jboss.as.controller.capability.BinaryCapabilityNameResolver;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.wildfly.service.descriptor.BinaryServiceDescriptor;

/**
 * Registers a resource definition for the persistence component of a cache configuration.
 * @author Paul Ferraro
 */
public abstract class PersistenceResourceDefinitionRegistrar extends ConfigurationResourceDefinitionRegistrar<PersistenceConfiguration, PersistenceConfigurationBuilder> {

    static final BinaryServiceDescriptor<PersistenceConfiguration> SERVICE_DESCRIPTOR = BinaryServiceDescriptorFactory.createServiceDescriptor(List.of(StoreResourceRegistration.WILDCARD), PersistenceConfiguration.class);
    static final RuntimeCapability<Void> CAPABILITY = RuntimeCapability.Builder.of(SERVICE_DESCRIPTOR).setDynamicNameMapper(BinaryCapabilityNameResolver.GRANDPARENT_PARENT).setAllowMultipleRegistrations(true).build();

    interface Configurator extends ConfigurationResourceDefinitionRegistrar.Configurator<PersistenceConfiguration> {

        @Override
        default RuntimeCapability<Void> getCapability() {
            return CAPABILITY;
        }
    }

    PersistenceResourceDefinitionRegistrar(Configurator configurator) {
        super(configurator);
    }
}

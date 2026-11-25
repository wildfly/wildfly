/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import java.util.List;

import org.infinispan.configuration.cache.AsyncStoreConfiguration;
import org.infinispan.configuration.cache.AsyncStoreConfigurationBuilder;
import org.infinispan.persistence.sifs.configuration.SoftIndexFileStoreConfigurationBuilder;
import org.jboss.as.controller.capability.BinaryCapabilityNameResolver;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.wildfly.service.descriptor.BinaryServiceDescriptor;

/**
 * Registers a resource definition for the write component of a cache store.
 * @author Paul Ferraro
 */
public abstract class StoreWriteResourceDefinitionRegistrar extends ConfigurationResourceDefinitionRegistrar<AsyncStoreConfiguration, AsyncStoreConfigurationBuilder<SoftIndexFileStoreConfigurationBuilder>> {

    static final BinaryServiceDescriptor<AsyncStoreConfiguration> SERVICE_DESCRIPTOR = BinaryServiceDescriptorFactory.createServiceDescriptor(List.of(StoreResourceRegistration.WILDCARD, StoreWriteResourceRegistration.WILDCARD), AsyncStoreConfiguration.class);
    private static final RuntimeCapability<Void> CAPABILITY = RuntimeCapability.Builder.of(SERVICE_DESCRIPTOR).setDynamicNameMapper(BinaryCapabilityNameResolver.GREATGRANDPARENT_GRANDPARENT).setAllowMultipleRegistrations(true).build();

    interface Configurator extends ConfigurationResourceDefinitionRegistrar.Configurator<AsyncStoreConfiguration> {
        @Override
        default RuntimeCapability<Void> getCapability() {
            return CAPABILITY;
        }
    }

    StoreWriteResourceDefinitionRegistrar(Configurator configurator) {
        super(configurator);
    }
}

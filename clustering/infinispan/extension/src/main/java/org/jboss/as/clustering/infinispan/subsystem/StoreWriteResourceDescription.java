/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.List;

import org.infinispan.configuration.cache.AsyncStoreConfiguration;
import org.infinispan.configuration.cache.AsyncStoreConfigurationBuilder;
import org.infinispan.persistence.sifs.configuration.SoftIndexFileStoreConfigurationBuilder;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.capability.BinaryCapabilityNameResolver;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.wildfly.service.descriptor.BinaryServiceDescriptor;

/**
 * @author Paul Ferraro
 */
public interface StoreWriteResourceDescription extends CacheComponentResourceDescription<AsyncStoreConfiguration, AsyncStoreConfigurationBuilder<SoftIndexFileStoreConfigurationBuilder>> {

    static PathElement pathElement(String value) {
        return PathElement.pathElement("write", value);
    }

    static final BinaryServiceDescriptor<AsyncStoreConfiguration> SERVICE_DESCRIPTOR = CacheComponentResourceDescription.createServiceDescriptor(List.of(PersistenceResourceDescription.pathElement(PathElement.WILDCARD_VALUE), pathElement(PathElement.WILDCARD_VALUE)), AsyncStoreConfiguration.class);
    static final RuntimeCapability<Void> CAPABILITY = RuntimeCapability.Builder.of(SERVICE_DESCRIPTOR).setDynamicNameMapper(BinaryCapabilityNameResolver.GREATGRANDPARENT_GRANDPARENT).setAllowMultipleRegistrations(true).build();

    @Override
    default PathElement getPathKey() {
        return pathElement(PathElement.WILDCARD_VALUE);
    }

    @Override
    default BinaryServiceDescriptor<AsyncStoreConfiguration> getServiceDescriptor() {
        return SERVICE_DESCRIPTOR;
    }

    @Override
    default RuntimeCapability<Void> getCapability() {
        return CAPABILITY;
    }
}

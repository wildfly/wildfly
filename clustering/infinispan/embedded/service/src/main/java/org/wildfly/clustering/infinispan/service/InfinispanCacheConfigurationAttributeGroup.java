/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.infinispan.service;

import org.infinispan.configuration.cache.Configuration;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.wildfly.clustering.server.service.CacheConfigurationAttributeGroup;
import org.wildfly.subsystem.resource.capability.CapabilityReference;
import org.wildfly.subsystem.resource.capability.CapabilityReferenceAttributeDefinition;

/**
 * Encapsulates the attributes that describe an Infinispan cache configuration.
 * @author Paul Ferraro
 */
public class InfinispanCacheConfigurationAttributeGroup implements CacheConfigurationAttributeGroup {

    private final CapabilityReferenceAttributeDefinition<Configuration> containerAttribute;
    private final CapabilityReferenceAttributeDefinition<Configuration> cacheAttribute;

    public InfinispanCacheConfigurationAttributeGroup(RuntimeCapability<Void> capability) {
        this.containerAttribute = new CapabilityReferenceAttributeDefinition.Builder<>("cache-container", CapabilityReference.builder(capability, InfinispanServiceDescriptor.DEFAULT_CACHE_CONFIGURATION).build()).build();
        this.cacheAttribute = new CapabilityReferenceAttributeDefinition.Builder<>("cache", CapabilityReference.builder(capability, InfinispanServiceDescriptor.CACHE_CONFIGURATION).withParentAttribute(this.containerAttribute).build()).setRequired(false).build();
    }

    @Override
    public CapabilityReferenceAttributeDefinition<Configuration> getContainerAttribute() {
        return this.containerAttribute;
    }

    @Override
    public CapabilityReferenceAttributeDefinition<Configuration> getCacheAttribute() {
        return this.cacheAttribute;
    }
}

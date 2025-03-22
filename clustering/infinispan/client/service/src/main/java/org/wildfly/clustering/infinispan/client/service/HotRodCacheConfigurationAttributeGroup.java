/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.infinispan.client.service;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelType;
import org.wildfly.clustering.infinispan.client.RemoteCacheContainer;
import org.wildfly.clustering.server.service.CacheConfigurationAttributeGroup;
import org.wildfly.subsystem.resource.capability.CapabilityReference;
import org.wildfly.subsystem.resource.capability.CapabilityReferenceAttributeDefinition;

/**
 * Encapsulates the attributes that describe a HotRod remote cache configuration.
 * @author Paul Ferraro
 */
public class HotRodCacheConfigurationAttributeGroup implements CacheConfigurationAttributeGroup {

    private final CapabilityReferenceAttributeDefinition<RemoteCacheContainer> containerAttribute;
    private final AttributeDefinition cacheAttribute;

    public HotRodCacheConfigurationAttributeGroup(RuntimeCapability<Void> capability) {
        this.containerAttribute = new CapabilityReferenceAttributeDefinition.Builder<>("remote-cache-container", CapabilityReference.builder(capability, HotRodServiceDescriptor.REMOTE_CACHE_CONTAINER).build()).build();
        this.cacheAttribute = new SimpleAttributeDefinitionBuilder("cache-configuration", ModelType.STRING)
                .setAllowExpression(true)
                .setRequired(false)
                .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                .build();
    }

    @Override
    public CapabilityReferenceAttributeDefinition<RemoteCacheContainer> getContainerAttribute() {
        return this.containerAttribute;
    }

    @Override
    public AttributeDefinition getCacheAttribute() {
        return this.cacheAttribute;
    }
}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.clustering.ejb;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.ejb.infinispan.remote.InfinispanClientMappingsRegistryProvider;
import org.wildfly.clustering.ejb.remote.ClientMappingsRegistryProvider;
import org.wildfly.clustering.infinispan.service.InfinispanCacheConfigurationAttributeGroup;
import org.wildfly.clustering.server.service.CacheConfigurationAttributeGroup;
import org.wildfly.subsystem.resource.ResourceDescriptor;

/**
 * Registers a resource definition for an embedded Infinispan provider of a client-mappings registry.
 * @author Paul Ferraro
 * @author Richard Achmatowicz
 */
public class InfinispanClientMappingsRegistryProviderResourceDefinitionRegistrar extends ClientMappingsRegistryProviderResourceDefinitionRegistrar {

    static final CacheConfigurationAttributeGroup CACHE_ATTRIBUTE_GROUP = new InfinispanCacheConfigurationAttributeGroup(CAPABILITY);

    InfinispanClientMappingsRegistryProviderResourceDefinitionRegistrar() {
        super(ClientMappingsRegistryProviderResourceRegistration.INFINISPAN);
    }

    @Override
    public ResourceDescriptor.Builder apply(ResourceDescriptor.Builder builder) {
        return super.apply(builder).addAttributes(CACHE_ATTRIBUTE_GROUP.getAttributes());
    }

    @Override
    public ClientMappingsRegistryProvider resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        return new InfinispanClientMappingsRegistryProvider(CACHE_ATTRIBUTE_GROUP.resolve(context, model));
    }
}

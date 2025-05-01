/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.clustering.ejb;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.ejb.infinispan.remote.InfinispanClientMappingsRegistryProvider;
import org.wildfly.clustering.infinispan.service.InfinispanCacheConfigurationAttributeGroup;
import org.wildfly.clustering.server.service.CacheConfigurationAttributeGroup;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.capability.CapabilityServiceInstaller;

/**
 * Definition of the /subsystem=distributable-ejb/client-mappings-registry=infinispan resource.
 *
 * @author Paul Ferraro
 * @author Richard Achmatowicz
 */
public class InfinispanClientMappingsRegistryProviderResourceDefinition extends ClientMappingsRegistryProviderResourceDefinition {

    static final PathElement PATH = pathElement("infinispan");
    static final CacheConfigurationAttributeGroup CACHE_ATTRIBUTE_GROUP = new InfinispanCacheConfigurationAttributeGroup(CAPABILITY);

    InfinispanClientMappingsRegistryProviderResourceDefinition() {
        super(PATH, builder -> builder.addAttributes(CACHE_ATTRIBUTE_GROUP.getAttributes()));
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        return CapabilityServiceInstaller.builder(CAPABILITY, new InfinispanClientMappingsRegistryProvider(CACHE_ATTRIBUTE_GROUP.resolve(context, model))).build();
    }
}

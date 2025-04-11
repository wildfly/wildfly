/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import java.util.function.Function;

import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.jboss.as.clustering.infinispan.persistence.hotrod.HotRodStoreConfiguration;
import org.jboss.as.clustering.infinispan.persistence.hotrod.HotRodStoreConfigurationBuilder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.infinispan.client.RemoteCacheContainer;
import org.wildfly.clustering.infinispan.client.service.HotRodCacheConfigurationAttributeGroup;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * Registers a resource definition of a HotRod store.
 * @author Paul Ferraro
 */
public class HotRodStoreResourceDefinitionRegistrar extends StoreResourceDefinitionRegistrar<HotRodStoreConfiguration, HotRodStoreConfigurationBuilder> {

    static final HotRodCacheConfigurationAttributeGroup CACHE_ATTRIBUTE_GROUP = new HotRodCacheConfigurationAttributeGroup(CAPABILITY);

    HotRodStoreResourceDefinitionRegistrar() {
        super(new Configurator<>() {
            @Override
            public ResourceRegistration getResourceRegistration() {
                return StoreResourceRegistration.HOTROD;
            }

            @Override
            public ServiceDependency<HotRodStoreConfigurationBuilder> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
                String cacheConfiguration = CACHE_ATTRIBUTE_GROUP.getCacheAttribute().resolveModelAttribute(context, model).asStringOrNull();
                return CACHE_ATTRIBUTE_GROUP.getContainerAttribute().resolve(context, model).map(new Function<>() {
                    @Override
                    public HotRodStoreConfigurationBuilder apply(RemoteCacheContainer container) {
                        return new ConfigurationBuilder().persistence().addStore(HotRodStoreConfigurationBuilder.class)
                                .remoteCacheContainer(container)
                                .cacheConfiguration(cacheConfiguration);
                    }
                });
            }
        });
    }

    @Override
    public ResourceDescriptor.Builder apply(ResourceDescriptor.Builder builder) {
        return super.apply(builder).addAttributes(CACHE_ATTRIBUTE_GROUP.getAttributes());
    }
}

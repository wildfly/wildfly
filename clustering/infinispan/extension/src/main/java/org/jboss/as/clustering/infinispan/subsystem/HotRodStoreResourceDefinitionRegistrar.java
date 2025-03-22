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
import org.wildfly.clustering.infinispan.client.service.HotRodCacheConfigurationDescriptor;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * @author Paul Ferraro
 *
 */
public class HotRodStoreResourceDefinitionRegistrar extends StoreResourceDefinitionRegistrar<HotRodStoreConfiguration, HotRodStoreConfigurationBuilder> {

    static final HotRodCacheConfigurationDescriptor CACHE_CONFIGURATION = new HotRodCacheConfigurationDescriptor(CAPABILITY);

    HotRodStoreResourceDefinitionRegistrar() {
        super(new Configurator<>() {
            @Override
            public ResourceRegistration getResourceRegistration() {
                return StoreResourceRegistration.HOTROD;
            }

            @Override
            public ServiceDependency<HotRodStoreConfigurationBuilder> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
                String cacheConfiguration = CACHE_CONFIGURATION.getCacheAttribute().resolveModelAttribute(context, model).asStringOrNull();
                return CACHE_CONFIGURATION.getContainerAttribute().resolve(context, model).map(new Function<>() {
                    @Override
                    public HotRodStoreConfigurationBuilder apply(RemoteCacheContainer container) {
                        return new ConfigurationBuilder().persistence().addStore(HotRodStoreConfigurationBuilder.class)
                                .segmented(false)
                                .remoteCacheContainer(container)
                                .cacheConfiguration(cacheConfiguration);
                    }
                });
            }
        });
    }

    @Override
    public ResourceDescriptor.Builder apply(ResourceDescriptor.Builder builder) {
        return CACHE_CONFIGURATION.apply(super.apply(builder));
    }
}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;


import static org.jboss.as.clustering.infinispan.subsystem.HotRodStoreResourceDefinition.Attribute.CACHE_CONFIGURATION;
import static org.jboss.as.clustering.infinispan.subsystem.HotRodStoreResourceDefinition.Attribute.REMOTE_CACHE_CONTAINER;

import org.jboss.as.clustering.infinispan.persistence.hotrod.HotRodStoreConfiguration;
import org.jboss.as.clustering.infinispan.persistence.hotrod.HotRodStoreConfigurationBuilder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.wildfly.clustering.infinispan.client.RemoteCacheContainer;
import org.wildfly.clustering.infinispan.client.service.InfinispanClientRequirement;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SupplierDependency;

/**
 * @author Radoslav Husar
 */
public class HotRodStoreServiceConfigurator extends StoreServiceConfigurator<HotRodStoreConfiguration, HotRodStoreConfigurationBuilder> {

    private volatile SupplierDependency<RemoteCacheContainer> remoteCacheContainer;
    private volatile String cacheConfiguration;

    HotRodStoreServiceConfigurator(PathAddress address) {
        super(address, HotRodStoreConfigurationBuilder.class);
    }

    @Override
    public ServiceConfigurator configure(OperationContext context, ModelNode model) throws OperationFailedException {
        this.cacheConfiguration = CACHE_CONFIGURATION.resolveModelAttribute(context, model).asStringOrNull();
        String remoteCacheContainerName = REMOTE_CACHE_CONTAINER.resolveModelAttribute(context, model).asString();
        this.remoteCacheContainer = new ServiceSupplierDependency<>(InfinispanClientRequirement.REMOTE_CONTAINER.getServiceName(context, remoteCacheContainerName));
        return super.configure(context, model);
    }

    @Override
    public <T> ServiceBuilder<T> register(ServiceBuilder<T> builder) {
        return super.register(this.remoteCacheContainer.register(builder));
    }

    @Override
    public void accept(HotRodStoreConfigurationBuilder builder) {
        builder.segmented(false)
                .cacheConfiguration(this.cacheConfiguration)
                .remoteCacheContainer(this.remoteCacheContainer.get())
        ;
    }
}

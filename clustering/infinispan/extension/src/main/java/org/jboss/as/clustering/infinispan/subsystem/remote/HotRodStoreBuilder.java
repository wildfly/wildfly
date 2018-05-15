/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.clustering.infinispan.subsystem.remote;


import static org.jboss.as.clustering.infinispan.subsystem.remote.HotRodStoreResourceDefinition.Attribute.CACHE_CONFIGURATION;
import static org.jboss.as.clustering.infinispan.subsystem.remote.HotRodStoreResourceDefinition.Attribute.REMOTE_CACHE_CONTAINER;

import org.infinispan.configuration.cache.PersistenceConfiguration;
import org.jboss.as.clustering.infinispan.subsystem.StoreBuilder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.infinispan.spi.InfinispanRequirement;
import org.wildfly.clustering.infinispan.spi.RemoteCacheContainer;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.service.InjectedValueDependency;

/**
 * @author Radoslav Husar
 */
public class HotRodStoreBuilder extends StoreBuilder<HotRodStoreConfiguration, HotRodStoreConfigurationBuilder> {

    private volatile InjectedValueDependency<RemoteCacheContainer> remoteCacheContainer;
    private volatile String cacheConfiguration;

    HotRodStoreBuilder(PathAddress address) {
        super(address, HotRodStoreConfigurationBuilder.class);
    }

    @Override
    public Builder<PersistenceConfiguration> configure(OperationContext context, ModelNode model) throws OperationFailedException {
        cacheConfiguration = CACHE_CONFIGURATION.resolveModelAttribute(context, model).asStringOrNull();
        String remoteCacheContainerName = REMOTE_CACHE_CONTAINER.resolveModelAttribute(context, model).asString();
        this.remoteCacheContainer = new InjectedValueDependency<>(InfinispanRequirement.REMOTE_CONTAINER.getServiceName(context, remoteCacheContainerName), RemoteCacheContainer.class);
        return super.configure(context, model);
    }

    @Override
    public ServiceBuilder<PersistenceConfiguration> build(ServiceTarget target) {
        return this.remoteCacheContainer.register(super.build(target));
    }

    @Override
    public void accept(HotRodStoreConfigurationBuilder builder) {
        builder.cacheConfiguration(cacheConfiguration)
                .remoteCacheContainer(this.remoteCacheContainer.getValue())
        ;
    }
}

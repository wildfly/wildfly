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

import org.jboss.as.clustering.infinispan.subsystem.StoreServiceConfigurator;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.wildfly.clustering.infinispan.spi.InfinispanRequirement;
import org.wildfly.clustering.infinispan.spi.RemoteCacheContainer;
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
        this.remoteCacheContainer = new ServiceSupplierDependency<>(InfinispanRequirement.REMOTE_CONTAINER.getServiceName(context, remoteCacheContainerName));
        return super.configure(context, model);
    }

    @Override
    public <T> ServiceBuilder<T> register(ServiceBuilder<T> builder) {
        return super.register(this.remoteCacheContainer.register(builder));
    }

    @Override
    public void accept(HotRodStoreConfigurationBuilder builder) {
        builder.cacheConfiguration(this.cacheConfiguration)
                .remoteCacheContainer(this.remoteCacheContainer.get())
        ;
    }
}

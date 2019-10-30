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

package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.clustering.infinispan.subsystem.SegmentedCacheResourceDefinition.Attribute.CONSISTENT_HASH_STRATEGY;
import static org.jboss.as.clustering.infinispan.subsystem.SegmentedCacheResourceDefinition.Attribute.SEGMENTS;

import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.HashConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.distribution.ch.impl.DefaultConsistentHashFactory;
import org.infinispan.distribution.ch.impl.TopologyAwareConsistentHashFactory;
import org.jboss.as.clustering.dmr.ModelNodes;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SupplierDependency;

/**
 * @author Paul Ferraro
 */
public class SegmentedCacheServiceConfigurator extends SharedStateCacheServiceConfigurator {

    private final SupplierDependency<GlobalConfiguration> global;

    private volatile ConsistentHashStrategy consistentHashStrategy;
    private volatile int segments;

    SegmentedCacheServiceConfigurator(PathAddress address, CacheMode mode) {
        super(address, mode);
        this.global = new ServiceSupplierDependency<>(CacheContainerResourceDefinition.Capability.CONFIGURATION.getServiceName(address.getParent()));
    }

    @Override
    public <T> ServiceBuilder<T> register(ServiceBuilder<T> builder) {
        return super.register(this.global.register(builder));
    }

    @Override
    public ServiceConfigurator configure(OperationContext context, ModelNode model) throws OperationFailedException {
        this.consistentHashStrategy = ModelNodes.asEnum(CONSISTENT_HASH_STRATEGY.resolveModelAttribute(context, model), ConsistentHashStrategy.class);
        this.segments = SEGMENTS.resolveModelAttribute(context, model).asInt();

        return super.configure(context, model);
    }

    @Override
    public void accept(ConfigurationBuilder builder) {
        super.accept(builder);

        HashConfigurationBuilder hash = builder.clustering().hash().numSegments(this.segments);

        // ConsistentHashStrategy.INTER_CACHE is Infinispan's default behavior
        if (this.consistentHashStrategy == ConsistentHashStrategy.INTRA_CACHE) {
            hash.consistentHashFactory(this.global.get().transport().hasTopologyInfo() ? new TopologyAwareConsistentHashFactory() : new DefaultConsistentHashFactory());
        }
    }
}

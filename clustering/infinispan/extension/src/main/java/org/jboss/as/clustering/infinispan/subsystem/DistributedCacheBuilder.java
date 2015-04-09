/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

import static org.jboss.as.clustering.infinispan.subsystem.DistributedCacheResourceDefinition.Attribute.*;

import java.util.ServiceLoader;

import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.GroupsConfigurationBuilder;
import org.infinispan.configuration.cache.HashConfiguration;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.distribution.group.Grouper;
import org.jboss.as.clustering.dmr.ModelNodes;
import org.jboss.as.controller.ExpressionResolver;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.clustering.infinispan.spi.service.CacheContainerServiceName;
import org.wildfly.clustering.service.Builder;

/**
 * Builds the configuration for a distributed cache.
 * @author Paul Ferraro
 */
public class DistributedCacheBuilder extends SharedStateCacheBuilder {

    private final InjectedValue<GlobalConfiguration> container = new InjectedValue<>();
    private final String containerName;

    private volatile HashConfiguration hash;
    private volatile ConsistentHashStrategy consistentHashStrategy;

    DistributedCacheBuilder(String containerName, String cacheName) {
        super(containerName, cacheName, CacheMode.DIST_SYNC);
        this.containerName = containerName;
    }

    @Override
    public ServiceBuilder<Configuration> build(ServiceTarget target) {
        return super.build(target).addDependency(CacheContainerServiceName.CONFIGURATION.getServiceName(this.containerName), GlobalConfiguration.class, this.container);
    }

    @Override
    public Builder<Configuration> configure(ExpressionResolver resolver, ModelNode model) throws OperationFailedException {
        this.consistentHashStrategy = ModelNodes.asEnum(CONSISTENT_HASH_STRATEGY.getDefinition().resolveModelAttribute(resolver, model), ConsistentHashStrategy.class);
        long l1Lifespan = L1_LIFESPAN.getDefinition().resolveModelAttribute(resolver, model).asLong();
        this.hash = new ConfigurationBuilder().clustering().hash()
                .capacityFactor(CAPACITY_FACTOR.getDefinition().resolveModelAttribute(resolver, model).asInt())
                .numOwners(OWNERS.getDefinition().resolveModelAttribute(resolver, model).asInt())
                .numSegments(SEGMENTS.getDefinition().resolveModelAttribute(resolver, model).asInt())
                .l1().enabled(l1Lifespan > 0).lifespan(l1Lifespan)
                .hash().create();
        return super.configure(resolver, model);
    }

    @Override
    public ConfigurationBuilder createConfigurationBuilder() {
        ConfigurationBuilder builder = super.createConfigurationBuilder();
        GroupsConfigurationBuilder groupsBuilder = builder.clustering().hash().read(this.hash)
                .consistentHashFactory(this.consistentHashStrategy.createConsistentHashFactory(this.container.getValue().transport().hasTopologyInfo()))
                .groups().enabled();
        for (Grouper<?> grouper: ServiceLoader.load(Grouper.class, this.getClassLoader())) {
            groupsBuilder.addGrouper(grouper);
        }
        return builder;
    }
}

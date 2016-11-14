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

import static org.jboss.as.clustering.infinispan.subsystem.EvictionResourceDefinition.Attribute.MAX_ENTRIES;
import static org.jboss.as.clustering.infinispan.subsystem.EvictionResourceDefinition.Attribute.STRATEGY;

import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.EvictionConfiguration;
import org.infinispan.configuration.cache.EvictionConfigurationBuilder;
import org.infinispan.configuration.cache.PersistenceConfiguration;
import org.infinispan.eviction.EvictionStrategy;
import org.infinispan.eviction.EvictionType;
import org.jboss.as.clustering.controller.ResourceServiceBuilder;
import org.jboss.as.clustering.dmr.ModelNodes;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.service.InjectedValueDependency;
import org.wildfly.clustering.service.ValueDependency;

/**
 * @author Paul Ferraro
 */
public class EvictionBuilder extends ComponentBuilder<EvictionConfiguration> implements ResourceServiceBuilder<EvictionConfiguration> {

    private final EvictionConfigurationBuilder builder = new ConfigurationBuilder().eviction();
    private final ValueDependency<PersistenceConfiguration> persistence;

    private volatile EvictionStrategy strategy;

    EvictionBuilder(PathAddress cacheAddress) {
        super(CacheComponent.EVICTION, cacheAddress);
        this.persistence = new InjectedValueDependency<>(CacheComponent.PERSISTENCE.getServiceName(cacheAddress), PersistenceConfiguration.class);
    }

    @Override
    public ServiceBuilder<EvictionConfiguration> build(ServiceTarget target) {
        return this.persistence.register(super.build(target));
    }

    @Override
    public Builder<EvictionConfiguration> configure(OperationContext context, ModelNode model) throws OperationFailedException {
        this.strategy = ModelNodes.asEnum(STRATEGY.resolveModelAttribute(context, model), EvictionStrategy.class);
        if (this.strategy.isEnabled()) {
            this.builder.type(EvictionType.COUNT).size(MAX_ENTRIES.resolveModelAttribute(context, model).asLong());
        }
        return this;
    }

    @Override
    public EvictionConfiguration getValue() {
        if ((this.strategy == EvictionStrategy.NONE) && this.persistence.getValue().passivation()) {
            // When passivation is enabled, convert NONE to MANUAL, which silences log WARNs on cache configuration validation
            this.strategy = EvictionStrategy.MANUAL;
        }
        return this.builder.strategy(this.strategy).create();
    }
}

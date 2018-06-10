/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.MemoryConfiguration;
import org.infinispan.configuration.cache.StorageType;
import org.infinispan.eviction.EvictionStrategy;
import org.infinispan.eviction.EvictionType;
import org.jboss.as.clustering.dmr.ModelNodes;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.service.ServiceConfigurator;

/**
 * @author Paul Ferraro
 */
public class MemoryServiceConfigurator extends ComponentServiceConfigurator<MemoryConfiguration> {

    private final StorageType storageType;
    private volatile long size;
    private volatile int capacity;
    private volatile EvictionType evictionType;

    MemoryServiceConfigurator(StorageType storageType, PathAddress address) {
        super(CacheComponent.MEMORY, address);
        this.storageType = storageType;
    }

    @Override
    public ServiceConfigurator configure(OperationContext context, ModelNode model) throws OperationFailedException {
        this.size = MemoryResourceDefinition.Attribute.SIZE.resolveModelAttribute(context, model).asLong();
        this.evictionType = ModelNodes.asEnum(BinaryMemoryResourceDefinition.Attribute.EVICTION_TYPE.resolveModelAttribute(context, model), EvictionType.class);
        this.capacity = OffHeapMemoryResourceDefinition.Attribute.CAPACITY.resolveModelAttribute(context, model).asInt();
        return this;
    }

    @Override
    public MemoryConfiguration get() {
        return new ConfigurationBuilder().memory()
                .size(this.size)
                .storageType(this.storageType)
                .evictionStrategy(this.size > 0 ? EvictionStrategy.REMOVE : EvictionStrategy.MANUAL)
                .evictionType(this.evictionType)
                .addressCount(this.capacity)
                .create();
    }
}

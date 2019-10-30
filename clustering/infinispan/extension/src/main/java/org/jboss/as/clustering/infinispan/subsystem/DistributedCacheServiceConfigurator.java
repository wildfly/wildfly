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

import static org.jboss.as.clustering.infinispan.subsystem.DistributedCacheResourceDefinition.Attribute.CAPACITY_FACTOR;
import static org.jboss.as.clustering.infinispan.subsystem.DistributedCacheResourceDefinition.Attribute.L1_LIFESPAN;
import static org.jboss.as.clustering.infinispan.subsystem.DistributedCacheResourceDefinition.Attribute.OWNERS;

import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.service.ServiceConfigurator;

/**
 * Builds the configuration for a distributed cache.
 * @author Paul Ferraro
 */
public class DistributedCacheServiceConfigurator extends SegmentedCacheServiceConfigurator {

    private volatile int capacityFactor;
    private volatile int owners;
    private volatile long l1Lifespan;

    DistributedCacheServiceConfigurator(PathAddress address) {
        super(address, CacheMode.DIST_SYNC);
    }

    @Override
    public ServiceConfigurator configure(OperationContext context, ModelNode model) throws OperationFailedException {
        this.capacityFactor = CAPACITY_FACTOR.resolveModelAttribute(context, model).asInt();
        this.l1Lifespan = L1_LIFESPAN.resolveModelAttribute(context, model).asLong();
        this.owners = OWNERS.resolveModelAttribute(context, model).asInt();

        return super.configure(context, model);
    }

    @Override
    public void accept(ConfigurationBuilder builder) {
        super.accept(builder);

        builder.clustering()
                .hash().capacityFactor(this.capacityFactor).numOwners(this.owners)
                .l1().enabled(this.l1Lifespan > 0).lifespan(this.l1Lifespan)
                ;
    }
}

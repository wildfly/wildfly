/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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

    private volatile float capacityFactor;
    private volatile int owners;
    private volatile long l1Lifespan;

    DistributedCacheServiceConfigurator(PathAddress address) {
        super(address, CacheMode.DIST_SYNC);
    }

    @Override
    public ServiceConfigurator configure(OperationContext context, ModelNode model) throws OperationFailedException {
        this.capacityFactor = (float) CAPACITY_FACTOR.resolveModelAttribute(context, model).asDouble();
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

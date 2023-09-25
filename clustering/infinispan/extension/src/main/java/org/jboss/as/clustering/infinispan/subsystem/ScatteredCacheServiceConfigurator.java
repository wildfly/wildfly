/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.clustering.infinispan.subsystem.ScatteredCacheResourceDefinition.Attribute.BIAS_LIFESPAN;
import static org.jboss.as.clustering.infinispan.subsystem.ScatteredCacheResourceDefinition.Attribute.INVALIDATION_BATCH_SIZE;

import java.util.concurrent.TimeUnit;

import org.infinispan.configuration.cache.BiasAcquisition;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.service.ServiceConfigurator;

/**
 * @author Paul Ferraro
 */
@Deprecated
public class ScatteredCacheServiceConfigurator extends SegmentedCacheServiceConfigurator {

    private volatile int invalidationBatchSize;
    private volatile long biasLifespan;

    ScatteredCacheServiceConfigurator(PathAddress address) {
        super(address, CacheMode.SCATTERED_SYNC);
    }

    @Override
    public ServiceConfigurator configure(OperationContext context, ModelNode model) throws OperationFailedException {
        this.invalidationBatchSize = INVALIDATION_BATCH_SIZE.resolveModelAttribute(context, model).asInt();
        this.biasLifespan = BIAS_LIFESPAN.resolveModelAttribute(context, model).asLong();
        return super.configure(context, model);
    }

    @Override
    public void accept(ConfigurationBuilder builder) {
        super.accept(builder);

        builder.clustering()
                .biasAcquisition((this.biasLifespan > 0) ? BiasAcquisition.ON_WRITE : BiasAcquisition.NEVER)
                .biasLifespan(this.biasLifespan, TimeUnit.MILLISECONDS)
                .invalidationBatchSize(this.invalidationBatchSize)
                ;
    }

}

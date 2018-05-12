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

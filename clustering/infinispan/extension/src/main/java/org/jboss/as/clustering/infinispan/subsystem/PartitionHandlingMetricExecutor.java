/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.jboss.as.controller.capability.BinaryCapabilityNameResolver;
import org.wildfly.subsystem.service.capture.FunctionExecutorRegistry;

/**
 * Executor for partition handling metrics.
 * @author Paul Ferraro
 */
public class PartitionHandlingMetricExecutor extends CacheMetricExecutor<AdvancedCache<?, ?>> {

    public PartitionHandlingMetricExecutor(FunctionExecutorRegistry<Cache<?, ?>> executors) {
        super(executors, BinaryCapabilityNameResolver.GRANDPARENT_PARENT);
    }

    @Override
    public AdvancedCache<?, ?> apply(Cache<?, ?> cache) {
        return cache.getAdvancedCache();
    }
}

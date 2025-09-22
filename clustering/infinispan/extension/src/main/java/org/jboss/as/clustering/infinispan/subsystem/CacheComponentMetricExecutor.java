/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import org.infinispan.Cache;
import org.infinispan.factories.ComponentRegistry;
import org.wildfly.subsystem.service.capture.FunctionExecutorRegistry;

/**
 * Executor for metrics based on cache components.
 * @author Paul Ferraro
 */
public class CacheComponentMetricExecutor<C> extends CacheMetricExecutor<C> {

    private final Class<C> componentClass;

    public CacheComponentMetricExecutor(FunctionExecutorRegistry<Cache<?, ?>> executors, Class<C> componentClass) {
        super(executors);
        this.componentClass = componentClass;
    }

    @Override
    public C apply(Cache<?, ?> cache) {
        return ComponentRegistry.componentOf(cache.getAdvancedCache(), this.componentClass);
    }
}

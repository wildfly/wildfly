/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import java.util.function.Function;

import org.infinispan.manager.EmbeddedCacheManager;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.infinispan.service.InfinispanServiceDescriptor;
import org.wildfly.service.capture.FunctionExecutor;
import org.wildfly.subsystem.resource.executor.Metric;
import org.wildfly.subsystem.resource.executor.MetricExecutor;
import org.wildfly.subsystem.resource.executor.MetricFunction;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.capture.FunctionExecutorRegistry;

/**
 * Executor for cache-container metrics.
 *
 * @author Paul Ferraro
 */
public class CacheContainerMetricExecutor implements MetricExecutor<EmbeddedCacheManager> {

    private final FunctionExecutorRegistry<EmbeddedCacheManager> executors;

    public CacheContainerMetricExecutor(FunctionExecutorRegistry<EmbeddedCacheManager> executors) {
        this.executors = executors;
    }

    @Override
    public ModelNode execute(OperationContext context, Metric<EmbeddedCacheManager> metric) throws OperationFailedException {
        FunctionExecutor<EmbeddedCacheManager> executor = this.executors.getExecutor(ServiceDependency.on(InfinispanServiceDescriptor.CACHE_CONTAINER, context.getCurrentAddressValue()));
        return (executor != null) ? executor.execute(new MetricFunction<>(Function.identity(), metric)) : null;
    }
}

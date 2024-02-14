/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.function.Function;

import org.infinispan.Cache;
import org.jboss.as.clustering.controller.Metric;
import org.jboss.as.clustering.controller.MetricExecutor;
import org.jboss.as.clustering.controller.MetricFunction;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.capability.BinaryCapabilityNameResolver;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.infinispan.service.InfinispanCacheRequirement;
import org.wildfly.service.capture.FunctionExecutor;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.capture.FunctionExecutorRegistry;

/**
 * @author Paul Ferraro
 */
public abstract class CacheMetricExecutor<C> implements MetricExecutor<C>, Function<Cache<?, ?>, C> {

    private final FunctionExecutorRegistry<Cache<?, ?>> executors;
    private final BinaryCapabilityNameResolver resolver;

    protected CacheMetricExecutor(FunctionExecutorRegistry<Cache<?, ?>> executors) {
        this(executors, BinaryCapabilityNameResolver.PARENT_CHILD);
    }

    protected CacheMetricExecutor(FunctionExecutorRegistry<Cache<?, ?>> executors, BinaryCapabilityNameResolver resolver) {
        this.executors = executors;
        this.resolver = resolver;
    }

    @Override
    public ModelNode execute(OperationContext context, Metric<C> metric) throws OperationFailedException {
        ServiceName name = InfinispanCacheRequirement.CACHE.getServiceName(context, this.resolver);
        FunctionExecutor<Cache<?, ?>> executor = this.executors.getExecutor(ServiceDependency.on(name));
        return (executor != null) ? executor.execute(new MetricFunction<>(this, metric)) : null;
    }
}

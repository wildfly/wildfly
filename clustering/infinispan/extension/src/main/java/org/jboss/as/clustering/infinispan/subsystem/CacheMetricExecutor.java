/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.StatisticsConfiguration;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.capability.BinaryCapabilityNameResolver;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.function.Function;
import org.wildfly.clustering.function.Predicate;
import org.wildfly.clustering.infinispan.service.InfinispanServiceDescriptor;
import org.wildfly.service.capture.FunctionExecutor;
import org.wildfly.subsystem.resource.executor.Metric;
import org.wildfly.subsystem.resource.executor.MetricExecutor;
import org.wildfly.subsystem.resource.executor.MetricFunction;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.capture.FunctionExecutorRegistry;

/**
 * An executor of cache metrics.
 * @author Paul Ferraro
 */
public abstract class CacheMetricExecutor<C> implements MetricExecutor<C>, Function<Cache<?, ?>, C> {
    private static final Predicate<Cache<?, ?>> STATISTICS_ENABLED = Predicate.of(Function.of(Cache::getCacheConfiguration, Configuration::statistics), StatisticsConfiguration::enabled);

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
        String[] resolved = this.resolver.apply(context.getCurrentAddress());
        FunctionExecutor<Cache<?, ?>> executor = this.executors.getExecutor(ServiceDependency.on(InfinispanServiceDescriptor.CACHE, resolved[0], resolved[1]));
        return (executor != null) ? executor.execute(new MetricFunction<>(Function.when(STATISTICS_ENABLED, this, Function.of(null)), metric)) : null;
    }
}

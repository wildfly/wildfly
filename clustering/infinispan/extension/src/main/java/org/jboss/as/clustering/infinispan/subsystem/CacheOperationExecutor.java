/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.function.Function;

import org.infinispan.Cache;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceNameFactory;
import org.jboss.as.controller.capability.BinaryCapabilityNameResolver;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.infinispan.service.InfinispanServiceDescriptor;
import org.wildfly.service.capture.FunctionExecutor;
import org.wildfly.subsystem.resource.executor.RuntimeOperation;
import org.wildfly.subsystem.resource.executor.RuntimeOperationExecutor;
import org.wildfly.subsystem.resource.executor.RuntimeOperationFunction;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.capture.FunctionExecutorRegistry;

/**
 * An executor for cache runtime operations.
 * @author Paul Ferraro
 */
public abstract class CacheOperationExecutor<C> implements RuntimeOperationExecutor<C>, Function<Cache<?, ?>, C> {

    private final FunctionExecutorRegistry<Cache<?, ?>> executors;
    private final BinaryCapabilityNameResolver resolver;

    CacheOperationExecutor(FunctionExecutorRegistry<Cache<?, ?>> executors) {
        this(executors, BinaryCapabilityNameResolver.PARENT_CHILD);
    }

    CacheOperationExecutor(FunctionExecutorRegistry<Cache<?, ?>> executors, BinaryCapabilityNameResolver resolver) {
        this.executors = executors;
        this.resolver = resolver;
    }

    @Override
    public ModelNode execute(OperationContext context, ModelNode op, RuntimeOperation<C> operation) throws OperationFailedException {
        ServiceName name = ServiceNameFactory.parseServiceName(InfinispanServiceDescriptor.CACHE.getName()).append(this.resolver.apply(context.getCurrentAddress()));
        FunctionExecutor<Cache<?, ?>> executor = this.executors.getExecutor(ServiceDependency.on(name));
        return (executor != null) ? executor.execute(new RuntimeOperationFunction<>(context, op, this, operation)) : null;
    }
}

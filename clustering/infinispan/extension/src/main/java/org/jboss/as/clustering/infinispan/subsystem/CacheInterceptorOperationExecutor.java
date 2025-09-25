/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import org.infinispan.Cache;
import org.infinispan.factories.ComponentRegistry;
import org.infinispan.interceptors.AsyncInterceptor;
import org.infinispan.interceptors.AsyncInterceptorChain;
import org.wildfly.subsystem.service.capture.FunctionExecutorRegistry;

/**
 * Executor for metrics based on a cache interceptor.
 * @author Paul Ferraro
 */
public class CacheInterceptorOperationExecutor<I extends AsyncInterceptor> extends CacheOperationExecutor<I> {

    private final Class<I> interceptorClass;

    public CacheInterceptorOperationExecutor(FunctionExecutorRegistry<Cache<?, ?>> executors, Class<I> interceptorClass) {
        super(executors);
        this.interceptorClass = interceptorClass;
    }

    @Override
    public I apply(Cache<?, ?> cache) {
        return ComponentRegistry.componentOf(cache.getAdvancedCache(), AsyncInterceptorChain.class).findInterceptorExtending(this.interceptorClass);
    }
}

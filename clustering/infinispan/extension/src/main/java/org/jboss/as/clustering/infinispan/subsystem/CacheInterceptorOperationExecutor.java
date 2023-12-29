/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import org.infinispan.Cache;
import org.infinispan.interceptors.AsyncInterceptor;
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

    @SuppressWarnings("deprecation")
    @Override
    public I apply(Cache<?, ?> cache) {
        return cache.getAdvancedCache().getAsyncInterceptorChain().findInterceptorExtending(this.interceptorClass);
    }
}

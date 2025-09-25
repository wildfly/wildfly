/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import org.infinispan.Cache;
import org.infinispan.factories.ComponentRegistry;
import org.infinispan.interceptors.AsyncInterceptor;
import org.infinispan.interceptors.AsyncInterceptorChain;
import org.jboss.as.controller.capability.BinaryCapabilityNameResolver;
import org.wildfly.subsystem.service.capture.FunctionExecutorRegistry;

/**
 * Executor for metrics based on a cache interceptor.
 * @author Paul Ferraro
 */
public class CacheInterceptorMetricExecutor<I extends AsyncInterceptor> extends CacheMetricExecutor<I> {

    private final Class<I> interceptorClass;

    public CacheInterceptorMetricExecutor(FunctionExecutorRegistry<Cache<?, ?>> executors, Class<I> interceptorClass) {
        super(executors);
        this.interceptorClass = interceptorClass;
    }

    public CacheInterceptorMetricExecutor(FunctionExecutorRegistry<Cache<?, ?>> executors, Class<I> interceptorClass, BinaryCapabilityNameResolver resolver) {
        super(executors, resolver);
        this.interceptorClass = interceptorClass;
    }

    @Override
    public I apply(Cache<?, ?> cache) {
        return ComponentRegistry.componentOf(cache.getAdvancedCache(), AsyncInterceptorChain.class).findInterceptorExtending(this.interceptorClass);
    }
}

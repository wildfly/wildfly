/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import org.infinispan.Cache;
import org.infinispan.interceptors.impl.CacheLoaderInterceptor;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.capability.BinaryCapabilityNameResolver;
import org.wildfly.subsystem.resource.ManagementResourceRegistrar;
import org.wildfly.subsystem.resource.executor.MetricOperationStepHandler;
import org.wildfly.subsystem.service.capture.FunctionExecutorRegistry;

/**
 * @author Paul Ferraro
 */
public enum PersistenceRuntimeResourceDescription implements CacheComponentRuntimeResourceDescription {
    INSTANCE;

    private final PathElement path = ComponentResourceDescription.pathElement("persistence");

    @Override
    public PathElement getPathElement() {
        return this.path;
    }

    @Override
    public ManagementResourceRegistrar apply(FunctionExecutorRegistry<Cache<?, ?>> executors) {
        return new MetricOperationStepHandler<>(new CacheInterceptorMetricExecutor<>(executors, CacheLoaderInterceptor.class, BinaryCapabilityNameResolver.GRANDPARENT_PARENT), StoreMetric.class);
    }
}

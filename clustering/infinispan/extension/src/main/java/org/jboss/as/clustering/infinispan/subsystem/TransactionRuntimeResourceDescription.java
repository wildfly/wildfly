/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import org.infinispan.Cache;
import org.infinispan.interceptors.impl.TxInterceptor;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.capability.BinaryCapabilityNameResolver;
import org.wildfly.subsystem.resource.ManagementResourceRegistrar;
import org.wildfly.subsystem.resource.executor.MetricOperationStepHandler;
import org.wildfly.subsystem.service.capture.FunctionExecutorRegistry;

/**
 * @author Paul Ferraro
 */
public enum TransactionRuntimeResourceDescription implements CacheComponentRuntimeResourceDescription {
    INSTANCE;

    @Override
    public PathElement getPathElement() {
        return TransactionResourceDescription.INSTANCE.getPathElement();
    }

    @Override
    public ManagementResourceRegistrar apply(FunctionExecutorRegistry<Cache<?, ?>> executors) {
        return new MetricOperationStepHandler<>(new CacheInterceptorMetricExecutor<>(executors, TxInterceptor.class, BinaryCapabilityNameResolver.GRANDPARENT_PARENT), TransactionMetric.class);
    }
}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import org.infinispan.Cache;
import org.infinispan.interceptors.impl.TxInterceptor;
import org.jboss.as.clustering.controller.MetricHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.capability.BinaryCapabilityNameResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.wildfly.subsystem.service.capture.FunctionExecutorRegistry;

/**
 * @author Paul Ferraro
 */
public class TransactionRuntimeResourceDefinition extends CacheComponentRuntimeResourceDefinition {

    static final PathElement PATH = pathElement("transaction");

    private final FunctionExecutorRegistry<Cache<?, ?>> executors;

    TransactionRuntimeResourceDefinition(FunctionExecutorRegistry<Cache<?, ?>> executors) {
        super(PATH);
        this.executors = executors;
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent) {
        ManagementResourceRegistration registration = super.register(parent);
        new MetricHandler<>(new CacheInterceptorMetricExecutor<>(this.executors, TxInterceptor.class, BinaryCapabilityNameResolver.GRANDPARENT_PARENT), TransactionMetric.class).register(registration);
        return registration;
    }
}

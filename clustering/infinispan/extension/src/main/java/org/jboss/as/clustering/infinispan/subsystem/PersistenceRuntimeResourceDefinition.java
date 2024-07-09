/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import org.infinispan.Cache;
import org.infinispan.interceptors.impl.CacheLoaderInterceptor;
import org.jboss.as.clustering.controller.MetricHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.capability.BinaryCapabilityNameResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.wildfly.subsystem.service.capture.FunctionExecutorRegistry;

/**
 * @author Paul Ferraro
 */
public class PersistenceRuntimeResourceDefinition extends CacheComponentRuntimeResourceDefinition {

    static final PathElement PATH = pathElement("persistence");

    private final FunctionExecutorRegistry<Cache<?, ?>> executors;

    PersistenceRuntimeResourceDefinition(FunctionExecutorRegistry<Cache<?, ?>> executors) {
        super(PATH, StoreResourceDefinition.WILDCARD_PATH);
        this.executors = executors;
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent) {
        ManagementResourceRegistration registration = super.register(parent);
        new MetricHandler<>(new CacheInterceptorMetricExecutor<>(this.executors, CacheLoaderInterceptor.class, BinaryCapabilityNameResolver.GRANDPARENT_PARENT), StoreMetric.class).register(registration);
        return registration;
    }
}

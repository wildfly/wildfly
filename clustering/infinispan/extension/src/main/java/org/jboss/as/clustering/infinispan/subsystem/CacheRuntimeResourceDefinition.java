/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import org.infinispan.Cache;
import org.infinispan.eviction.impl.ActivationManager;
import org.infinispan.eviction.impl.PassivationManager;
import org.infinispan.interceptors.impl.CacheMgmtInterceptor;
import org.infinispan.interceptors.impl.InvalidationInterceptor;
import org.jboss.as.clustering.controller.ChildResourceDefinition;
import org.jboss.as.clustering.controller.MetricHandler;
import org.jboss.as.clustering.controller.OperationHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.wildfly.subsystem.service.capture.FunctionExecutorRegistry;

/**
 * @author Paul Ferraro
 */
public class CacheRuntimeResourceDefinition extends ChildResourceDefinition<ManagementResourceRegistration> {

    static final PathElement WILDCARD_PATH = pathElement(PathElement.WILDCARD_VALUE);
    static PathElement pathElement(String name) {
        return PathElement.pathElement("cache", name);
    }

    private final FunctionExecutorRegistry<Cache<?, ?>> executors;

    CacheRuntimeResourceDefinition(FunctionExecutorRegistry<Cache<?, ?>> executors) {
        super(new Parameters(WILDCARD_PATH, InfinispanExtension.SUBSYSTEM_RESOLVER.createChildResolver(WILDCARD_PATH)).setRuntime());
        this.executors = executors;
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent) {
        ManagementResourceRegistration registration = parent.registerSubModel(this);

        new MetricHandler<>(new CacheInterceptorMetricExecutor<>(this.executors, CacheMgmtInterceptor.class), CacheMetric.class).register(registration);
        new MetricHandler<>(new CacheInterceptorMetricExecutor<>(this.executors, InvalidationInterceptor.class), CacheInvalidationInterceptorMetric.class).register(registration);
        new MetricHandler<>(new CacheComponentMetricExecutor<>(this.executors, ActivationManager.class), CacheActivationMetric.class).register(registration);
        new MetricHandler<>(new CacheComponentMetricExecutor<>(this.executors, PassivationManager.class), CachePassivationMetric.class).register(registration);
        new MetricHandler<>(new ClusteredCacheMetricExecutor(this.executors), ClusteredCacheMetric.class).register(registration);
        new OperationHandler<>(new CacheInterceptorOperationExecutor<>(this.executors, CacheMgmtInterceptor.class), CacheOperation.class).register(registration);

        new LockingRuntimeResourceDefinition(this.executors).register(registration);
        new PartitionHandlingRuntimeResourceDefinition(this.executors).register(registration);
        new PersistenceRuntimeResourceDefinition(this.executors).register(registration);
        new TransactionRuntimeResourceDefinition(this.executors).register(registration);

        return registration;
    }
}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.List;

import org.infinispan.Cache;
import org.infinispan.eviction.impl.ActivationManager;
import org.infinispan.eviction.impl.PassivationManager;
import org.infinispan.interceptors.impl.CacheMgmtInterceptor;
import org.infinispan.interceptors.impl.InvalidationInterceptor;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.wildfly.subsystem.resource.ChildResourceDefinitionRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrationContext;
import org.wildfly.subsystem.resource.executor.MetricOperationStepHandler;
import org.wildfly.subsystem.resource.executor.RuntimeOperationStepHandler;
import org.wildfly.subsystem.service.capture.FunctionExecutorRegistry;

/**
 * @author Paul Ferraro
 */
public class CacheRuntimeResourceDefinitionRegistrar implements ChildResourceDefinitionRegistrar {

    static final PathElement WILDCARD_PATH = pathElement(PathElement.WILDCARD_VALUE);
    static PathElement pathElement(String name) {
        return PathElement.pathElement("cache", name);
    }

    private final FunctionExecutorRegistry<Cache<?, ?>> executors;

    CacheRuntimeResourceDefinitionRegistrar(FunctionExecutorRegistry<Cache<?, ?>> executors) {
        this.executors = executors;
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent, ManagementResourceRegistrationContext context) {
        ResourceDescriptionResolver resolver = InfinispanSubsystemResourceDefinitionRegistrar.RESOLVER.createChildResolver(WILDCARD_PATH);
        ManagementResourceRegistration registration = parent.registerSubModel(ResourceDefinition.builder(ResourceRegistration.of(WILDCARD_PATH), resolver).asRuntime().build());

        new MetricOperationStepHandler<>(new CacheInterceptorMetricExecutor<>(this.executors, CacheMgmtInterceptor.class), CacheMetric.class).register(registration);
        new MetricOperationStepHandler<>(new CacheInterceptorMetricExecutor<>(this.executors, InvalidationInterceptor.class), CacheInvalidationInterceptorMetric.class).register(registration);
        new MetricOperationStepHandler<>(new CacheComponentMetricExecutor<>(this.executors, ActivationManager.class), CacheActivationMetric.class).register(registration);
        new MetricOperationStepHandler<>(new CacheComponentMetricExecutor<>(this.executors, PassivationManager.class), CachePassivationMetric.class).register(registration);
        new MetricOperationStepHandler<>(new ClusteredCacheMetricExecutor(this.executors), ClusteredCacheMetric.class).register(registration);
        new RuntimeOperationStepHandler<>(new CacheInterceptorOperationExecutor<>(this.executors, CacheMgmtInterceptor.class), CacheOperation.class).register(registration);

        for (CacheComponentRuntimeResourceDescription component : List.of(LockingRuntimeResourceDescription.INSTANCE, PartitionHandlingRuntimeResourceDescription.INSTANCE, PersistenceRuntimeResourceDescription.INSTANCE, TransactionRuntimeResourceDescription.INSTANCE)) {
            new CacheComponentRuntimeResourceDefinitionRegistrar(component, this.executors).register(registration, context);
        }

        return registration;
    }
}

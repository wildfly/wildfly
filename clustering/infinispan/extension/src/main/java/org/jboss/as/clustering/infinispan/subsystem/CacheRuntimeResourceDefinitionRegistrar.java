/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import org.infinispan.Cache;
import org.infinispan.eviction.impl.ActivationManager;
import org.infinispan.eviction.impl.PassivationManager;
import org.infinispan.interceptors.impl.CacheLoaderInterceptor;
import org.infinispan.interceptors.impl.CacheMgmtInterceptor;
import org.infinispan.interceptors.impl.InvalidationInterceptor;
import org.infinispan.interceptors.impl.TxInterceptor;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.capability.BinaryCapabilityNameResolver;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.wildfly.subsystem.resource.ChildResourceDefinitionRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrationContext;
import org.wildfly.subsystem.resource.executor.MetricOperationStepHandler;
import org.wildfly.subsystem.resource.executor.RuntimeOperationStepHandler;
import org.wildfly.subsystem.service.capture.FunctionExecutorRegistry;

/**
 * Registers a runtime resource definition for a cache.
 * @author Paul Ferraro
 */
public class CacheRuntimeResourceDefinitionRegistrar implements ChildResourceDefinitionRegistrar {

    static final ResourceRegistration REGISTRATION = ResourceRegistration.of(PathElement.pathElement("cache"));

    private final FunctionExecutorRegistry<Cache<?, ?>> executors;

    CacheRuntimeResourceDefinitionRegistrar(FunctionExecutorRegistry<Cache<?, ?>> executors) {
        this.executors = executors;
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent, ManagementResourceRegistrationContext context) {
        ResourceDescriptionResolver resolver = InfinispanSubsystemResourceDefinitionRegistrar.RESOLVER.createChildResolver(REGISTRATION.getPathElement());
        ManagementResourceRegistration registration = parent.registerSubModel(ResourceDefinition.builder(REGISTRATION, resolver).asRuntime().build());

        new MetricOperationStepHandler<>(new CacheInterceptorMetricExecutor<>(this.executors, CacheMgmtInterceptor.class), CacheMetric.class).register(registration);
        new MetricOperationStepHandler<>(new CacheInterceptorMetricExecutor<>(this.executors, InvalidationInterceptor.class), CacheInvalidationInterceptorMetric.class).register(registration);
        new MetricOperationStepHandler<>(new CacheComponentMetricExecutor<>(this.executors, ActivationManager.class), CacheActivationMetric.class).register(registration);
        new MetricOperationStepHandler<>(new CacheComponentMetricExecutor<>(this.executors, PassivationManager.class), CachePassivationMetric.class).register(registration);
        new MetricOperationStepHandler<>(new ClusteredCacheMetricExecutor(this.executors), ClusteredCacheMetric.class).register(registration);
        new RuntimeOperationStepHandler<>(new CacheInterceptorOperationExecutor<>(this.executors, CacheMgmtInterceptor.class), CacheOperation.class).register(registration);

        new ComponentRuntimeResourceDefinitionRegistrar(ComponentResourceRegistration.LOCKING, new MetricOperationStepHandler<>(new LockingMetricExecutor(this.executors), LockingMetric.class)).register(registration, context);
        new ComponentRuntimeResourceDefinitionRegistrar(ComponentResourceRegistration.PARTITION_HANDLING, new ManagementResourceRegistrar() {
            @Override
            public void register(ManagementResourceRegistration registration) {
                new MetricOperationStepHandler<>(new PartitionHandlingMetricExecutor(CacheRuntimeResourceDefinitionRegistrar.this.executors), PartitionHandlingMetric.class).register(registration);
                new RuntimeOperationStepHandler<>(new PartitionHandlingOperationExecutor(CacheRuntimeResourceDefinitionRegistrar.this.executors), PartitionHandlingOperation.class).register(registration);
            }
        }).register(registration, context);
        new ComponentRuntimeResourceDefinitionRegistrar(ComponentResourceRegistration.PERSISTENCE, new MetricOperationStepHandler<>(new CacheInterceptorMetricExecutor<>(this.executors, CacheLoaderInterceptor.class, BinaryCapabilityNameResolver.GRANDPARENT_PARENT), StoreMetric.class)).register(registration, context);
        new ComponentRuntimeResourceDefinitionRegistrar(ComponentResourceRegistration.TRANSACTION, new MetricOperationStepHandler<>(new CacheInterceptorMetricExecutor<>(this.executors, TxInterceptor.class, BinaryCapabilityNameResolver.GRANDPARENT_PARENT), TransactionMetric.class)).register(registration, context);

        return registration;
    }
}

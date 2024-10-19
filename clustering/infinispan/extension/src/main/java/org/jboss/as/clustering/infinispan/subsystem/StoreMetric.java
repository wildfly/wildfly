/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import org.infinispan.interceptors.impl.CacheLoaderInterceptor;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.subsystem.resource.executor.Metric;

/**
 * Enumeration of management metrics for a cache store.
 *
 * @author Paul Ferraro
 */
@SuppressWarnings("rawtypes")
public enum StoreMetric implements Metric<CacheLoaderInterceptor> {

    CACHE_LOADER_LOADS("cache-loader-loads", ModelType.LONG, AttributeAccess.Flag.COUNTER_METRIC) {
        @Override
        public ModelNode execute(CacheLoaderInterceptor interceptor) {
            return new ModelNode(interceptor.getCacheLoaderLoads());
        }
    },
    CACHE_LOADER_MISSES("cache-loader-misses", ModelType.LONG, AttributeAccess.Flag.COUNTER_METRIC) {
        @Override
        public ModelNode execute(CacheLoaderInterceptor interceptor) {
            return new ModelNode(interceptor.getCacheLoaderMisses());
        }
    },
    ;
    private final AttributeDefinition definition;

    StoreMetric(String name, ModelType type, AttributeAccess.Flag metricType) {
        this.definition = new SimpleAttributeDefinitionBuilder(name, type)
                .setFlags(metricType)
                .setStorageRuntime()
                .build();
    }

    @Override
    public AttributeDefinition get() {
        return this.definition;
    }
}
/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import org.infinispan.interceptors.impl.InvalidationInterceptor;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.subsystem.resource.executor.Metric;

/**
 * Cache invalidation metrics.
 * @author Paul Ferraro
 */
public enum CacheInvalidationInterceptorMetric implements Metric<InvalidationInterceptor> {

    INVALIDATIONS("invalidations", ModelType.LONG, AttributeAccess.Flag.COUNTER_METRIC) {
        @Override
        public ModelNode execute(InvalidationInterceptor interceptor) {
            return new ModelNode(interceptor.getInvalidations());
        }
    },
    ;
    private final AttributeDefinition definition;

    CacheInvalidationInterceptorMetric(String name, ModelType type, AttributeAccess.Flag metricType) {
        this.definition = new SimpleAttributeDefinitionBuilder(name, type)
                .setStorageRuntime()
                .build();
    }

    @Override
    public AttributeDefinition get() {
        return this.definition;
    }
}

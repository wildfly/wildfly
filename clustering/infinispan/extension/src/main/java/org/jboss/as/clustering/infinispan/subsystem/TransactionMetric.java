/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import org.infinispan.interceptors.impl.TxInterceptor;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.subsystem.resource.executor.Metric;

/**
 * Enumeration of transaction management metrics for a cache.
 *
 * @author Paul Ferraro
 */
public enum TransactionMetric implements Metric<TxInterceptor> {

    COMMITS("commits", ModelType.LONG, AttributeAccess.Flag.COUNTER_METRIC) {
        @Override
        public ModelNode execute(TxInterceptor interceptor) {
            return new ModelNode(interceptor.getCommits());
        }
    },
    PREPARES("prepares", ModelType.LONG, AttributeAccess.Flag.COUNTER_METRIC) {
        @Override
        public ModelNode execute(TxInterceptor interceptor) {
            return new ModelNode(interceptor.getPrepares());
        }
    },
    ROLLBACKS("rollbacks", ModelType.LONG, AttributeAccess.Flag.COUNTER_METRIC) {
        @Override
        public ModelNode execute(TxInterceptor interceptor) {
            return new ModelNode(interceptor.getRollbacks());
        }
    },
    ;
    private final AttributeDefinition definition;

    TransactionMetric(String name, ModelType type, AttributeAccess.Flag metricType) {
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
/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import java.util.function.UnaryOperator;

import org.infinispan.util.concurrent.locks.impl.DefaultLockManager;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.subsystem.resource.executor.Metric;

/**
 * Enumeration of locking management metrics for a cache.
 *
 * @author Paul Ferraro
 */
public enum LockingMetric implements Metric<DefaultLockManager>, UnaryOperator<SimpleAttributeDefinitionBuilder> {

    CURRENT_CONCURRENCY_LEVEL("current-concurrency-level", ModelType.INT, AttributeAccess.Flag.GAUGE_METRIC) {
        @Override
        public ModelNode execute(DefaultLockManager manager) {
            return new ModelNode(manager.getConcurrencyLevel());
        }

        @Override
        public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
            return builder.setDeprecated(InfinispanSubsystemModel.VERSION_17_0_0.getVersion());
        }
    },
    NUMBER_OF_LOCKS_AVAILABLE("number-of-locks-available", ModelType.INT, AttributeAccess.Flag.GAUGE_METRIC) {
        @Override
        public ModelNode execute(DefaultLockManager manager) {
            return new ModelNode(manager.getNumberOfLocksAvailable());
        }
    },
    NUMBER_OF_LOCKS_HELD("number-of-locks-held", ModelType.INT, AttributeAccess.Flag.GAUGE_METRIC) {
        @Override
        public ModelNode execute(DefaultLockManager manager) {
            return new ModelNode(manager.getNumberOfLocksHeld());
        }
    },
    ;
    private final AttributeDefinition definition;

    LockingMetric(String name, ModelType type, AttributeAccess.Flag metricType) {
        this.definition = this.apply(new SimpleAttributeDefinitionBuilder(name, type)
                .setFlags(metricType)
                .setStorageRuntime()
                ).build();
    }

    @Override
    public AttributeDefinition get() {
        return this.definition;
    }

    @Override
    public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
        return builder;
    }
}

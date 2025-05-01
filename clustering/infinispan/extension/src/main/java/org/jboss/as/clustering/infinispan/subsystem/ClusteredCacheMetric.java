/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import org.infinispan.remoting.rpc.RpcManagerImpl;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.subsystem.resource.executor.Metric;

/**
 * Enumeration of management metrics for a clustered cache.
 * @author Paul Ferraro
 */
public enum ClusteredCacheMetric implements Metric<RpcManagerImpl> {

    AVERAGE_REPLICATION_TIME("average-replication-time", ModelType.LONG, MeasurementUnit.MILLISECONDS) {
        @Override
        public ModelNode execute(RpcManagerImpl manager) {
            return new ModelNode(manager.getAverageReplicationTime());
        }
    },
    REPLICATION_COUNT("replication-count", ModelType.LONG, AttributeAccess.Flag.COUNTER_METRIC) {
        @Override
        public ModelNode execute(RpcManagerImpl manager) {
            return new ModelNode(manager.getReplicationCount());
        }
    },
    REPLICATION_FAILURES("replication-failures", ModelType.LONG, AttributeAccess.Flag.COUNTER_METRIC) {
        @Override
        public ModelNode execute(RpcManagerImpl manager) {
            return new ModelNode(manager.getReplicationFailures());
        }
    },
    SUCCESS_RATIO("success-ratio", ModelType.DOUBLE, AttributeAccess.Flag.GAUGE_METRIC) {
        @Override
        public ModelNode execute(RpcManagerImpl manager) {
            return new ModelNode(manager.getSuccessRatioFloatingPoint());
        }
    },
    ;
    private final AttributeDefinition definition;

    ClusteredCacheMetric(String name, ModelType type, AttributeAccess.Flag metricType) {
        this(name, type, metricType, null);
    }

    ClusteredCacheMetric(String name, ModelType type, MeasurementUnit unit) {
        this(name, type, AttributeAccess.Flag.COUNTER_METRIC, unit);
    }

    ClusteredCacheMetric(String name, ModelType type, AttributeAccess.Flag metricType, MeasurementUnit unit) {
        this.definition = new SimpleAttributeDefinitionBuilder(name, type)
                .setFlags(metricType)
                .setMeasurementUnit(unit)
                .setStorageRuntime()
                .build();
    }

    @Override
    public AttributeDefinition get() {
        return this.definition;
    }
}
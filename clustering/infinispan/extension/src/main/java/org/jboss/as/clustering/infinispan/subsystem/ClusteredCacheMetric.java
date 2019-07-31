/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.clustering.infinispan.subsystem;

import org.infinispan.remoting.rpc.RpcManagerImpl;
import org.jboss.as.clustering.controller.Metric;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

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
    public AttributeDefinition getDefinition() {
        return this.definition;
    }
}
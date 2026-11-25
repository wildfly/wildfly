/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import java.util.function.UnaryOperator;

import org.infinispan.interceptors.impl.CacheMgmtInterceptor;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.subsystem.resource.executor.Metric;

/**
 * Enumeration of management metrics for a cache.
 * @author Paul Ferraro
 */
public enum CacheMetric implements Metric<CacheMgmtInterceptor>, UnaryOperator<SimpleAttributeDefinitionBuilder> {

    AVERAGE_READ_TIME("average-read-time", ModelType.LONG, MeasurementUnit.MILLISECONDS) {
        @Override
        public ModelNode execute(CacheMgmtInterceptor interceptor) {
            return new ModelNode(interceptor.getAverageReadTime());
        }
    },
    AVERAGE_REMOVE_TIME("average-remove-time", ModelType.LONG, MeasurementUnit.MILLISECONDS) {
        @Override
        public ModelNode execute(CacheMgmtInterceptor interceptor) {
            return new ModelNode(interceptor.getAverageRemoveTime());
        }
    },
    AVERAGE_WRITE_TIME("average-write-time", ModelType.LONG, MeasurementUnit.MILLISECONDS) {
        @Override
        public ModelNode execute(CacheMgmtInterceptor interceptor) {
            return new ModelNode(interceptor.getAverageWriteTime());
        }
    },
    EVICTIONS("evictions", ModelType.LONG, AttributeAccess.Flag.COUNTER_METRIC) {
        @Override
        public ModelNode execute(CacheMgmtInterceptor interceptor) {
            return new ModelNode(interceptor.getEvictions());
        }
    },
    HIT_RATIO("hit-ratio", ModelType.DOUBLE, AttributeAccess.Flag.GAUGE_METRIC) {
        @Override
        public ModelNode execute(CacheMgmtInterceptor interceptor) {
            return new ModelNode(interceptor.getHitRatio());
        }
    },
    HITS("hits", ModelType.LONG, AttributeAccess.Flag.COUNTER_METRIC) {
        @Override
        public ModelNode execute(CacheMgmtInterceptor interceptor) {
            return new ModelNode(interceptor.getHits());
        }
    },
    MISSES("misses", ModelType.LONG, AttributeAccess.Flag.COUNTER_METRIC) {
        @Override
        public ModelNode execute(CacheMgmtInterceptor interceptor) {
            return new ModelNode(interceptor.getMisses());
        }
    },
    NUMBER_OF_ENTRIES("number-of-entries", ModelType.LONG, AttributeAccess.Flag.GAUGE_METRIC) {
        @Override
        public ModelNode execute(CacheMgmtInterceptor interceptor) {
            return new ModelNode(interceptor.getApproximateEntries());
        }
    },
    NUMBER_OF_ENTRIES_IN_MEMORY("number-of-entries-in-memory", ModelType.LONG, AttributeAccess.Flag.GAUGE_METRIC) {
        @Override
        public ModelNode execute(CacheMgmtInterceptor interceptor) {
            return new ModelNode(interceptor.getApproximateEntriesInMemory());
        }
    },
    READ_WRITE_RATIO("read-write-ratio", ModelType.DOUBLE, AttributeAccess.Flag.GAUGE_METRIC) {
        @Override
        public ModelNode execute(CacheMgmtInterceptor interceptor) {
            return new ModelNode(interceptor.getReadWriteRatio());
        }
    },
    REMOVE_HITS("remove-hits", ModelType.LONG, AttributeAccess.Flag.COUNTER_METRIC) {
        @Override
        public ModelNode execute(CacheMgmtInterceptor interceptor) {
            return new ModelNode(interceptor.getRemoveHits());
        }
    },
    REMOVE_MISSES("remove-misses", ModelType.LONG, AttributeAccess.Flag.COUNTER_METRIC) {
        @Override
        public ModelNode execute(CacheMgmtInterceptor interceptor) {
            return new ModelNode(interceptor.getRemoveMisses());
        }
    },
    TIME_SINCE_RESET("time-since-reset", ModelType.LONG, MeasurementUnit.SECONDS) {
        @Override
        public ModelNode execute(CacheMgmtInterceptor interceptor) {
            return new ModelNode(interceptor.getTimeSinceReset());
        }
    },
    TIME_SINCE_START("time-since-start", ModelType.LONG, MeasurementUnit.SECONDS) {
        @Override
        public ModelNode execute(CacheMgmtInterceptor interceptor) {
            return new ModelNode(interceptor.getTimeSinceStart());
        }
    },
    WRITES("writes", ModelType.LONG, AttributeAccess.Flag.COUNTER_METRIC) {
        @Override
        public ModelNode execute(CacheMgmtInterceptor interceptor) {
            return new ModelNode(interceptor.getStores());
        }
    },
    ;
    private final AttributeDefinition definition;

    CacheMetric(String name, ModelType type, AttributeAccess.Flag metricType) {
        this(name, type, metricType, null);
    }

    CacheMetric(String name, ModelType type, MeasurementUnit unit) {
        this(name, type, AttributeAccess.Flag.GAUGE_METRIC, unit);
    }

    CacheMetric(String name, ModelType type, AttributeAccess.Flag metricType, MeasurementUnit unit) {
        this.definition = new SimpleAttributeDefinitionBuilder(name, type)
                .setFlags(metricType)
                .setMeasurementUnit(unit)
                .setStorageRuntime()
                .build();
    }

    @Override
    public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
        return builder;
    }

    @Override
    public AttributeDefinition get() {
        return this.definition;
    }
}
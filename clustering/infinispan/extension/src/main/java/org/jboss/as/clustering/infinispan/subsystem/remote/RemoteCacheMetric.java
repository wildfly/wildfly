/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem.remote;

import java.util.function.ToLongFunction;

import org.infinispan.client.hotrod.jmx.RemoteCacheClientStatisticsMXBean;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.registry.AttributeAccess.Flag;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.subsystem.resource.executor.Metric;

/**
 * Enumerates the metrics of a remote cache.
 * @author Paul Ferraro
 */
public enum RemoteCacheMetric implements Metric<RemoteCacheClientStatisticsMXBean>, ToLongFunction<RemoteCacheClientStatisticsMXBean> {

    AVERAGE_READ_TIME("average-read-time", MeasurementUnit.MILLISECONDS) {
        @Override
        public long applyAsLong(RemoteCacheClientStatisticsMXBean statistics) {
            return statistics.getAverageRemoteReadTime();
        }
    },
    AVERAGE_REMOVE_TIME("average-remove-time", MeasurementUnit.MILLISECONDS) {
        @Override
        public long applyAsLong(RemoteCacheClientStatisticsMXBean statistics) {
            return statistics.getAverageRemoteRemovesTime();
        }
    },
    AVERAGE_WRITE_TIME("average-write-time", MeasurementUnit.MILLISECONDS) {
        @Override
        public long applyAsLong(RemoteCacheClientStatisticsMXBean statistics) {
            return statistics.getAverageRemoteStoreTime();
        }
    },
    NEAR_CACHE_HITS("near-cache-hits", Flag.COUNTER_METRIC) {
        @Override
        public long applyAsLong(RemoteCacheClientStatisticsMXBean statistics) {
            return statistics.getNearCacheHits();
        }
    },
    NEAR_CACHE_INVALIDATIONS("near-cache-invalidations", Flag.COUNTER_METRIC) {
        @Override
        public long applyAsLong(RemoteCacheClientStatisticsMXBean statistics) {
            return statistics.getNearCacheInvalidations();
        }
    },
    NEAR_CACHE_MISSES("near-cache-misses", Flag.COUNTER_METRIC) {
        @Override
        public long applyAsLong(RemoteCacheClientStatisticsMXBean statistics) {
            return statistics.getNearCacheMisses();
        }
    },
    NEAR_CACHE_SIZE("near-cache-size", Flag.GAUGE_METRIC) {
        @Override
        public long applyAsLong(RemoteCacheClientStatisticsMXBean statistics) {
            return statistics.getNearCacheSize();
        }
    },
    HITS("hits", Flag.COUNTER_METRIC) {
        @Override
        public long applyAsLong(RemoteCacheClientStatisticsMXBean statistics) {
            return statistics.getRemoteHits();
        }
    },
    MISSES("misses", Flag.COUNTER_METRIC) {
        @Override
        public long applyAsLong(RemoteCacheClientStatisticsMXBean statistics) {
            return statistics.getRemoteMisses();
        }
    },
    REMOVES("removes", Flag.COUNTER_METRIC) {
        @Override
        public long applyAsLong(RemoteCacheClientStatisticsMXBean statistics) {
            return statistics.getRemoteRemoves();
        }
    },
    WRITES("writes", Flag.COUNTER_METRIC) {
        @Override
        public long applyAsLong(RemoteCacheClientStatisticsMXBean statistics) {
            return statistics.getRemoteStores();
        }
    },
    TIME_SINCE_RESET("time-since-reset", MeasurementUnit.SECONDS) {
        @Override
        public long applyAsLong(RemoteCacheClientStatisticsMXBean statistics) {
            return statistics.getTimeSinceReset();
        }
    },
    ;
    private final AttributeDefinition definition;

    RemoteCacheMetric(String name, Flag metricType) {
        this(name, metricType, null);
    }

    RemoteCacheMetric(String name, MeasurementUnit unit) {
        this(name, Flag.GAUGE_METRIC, unit);
    }

    RemoteCacheMetric(String name, Flag metricType, MeasurementUnit unit) {
        this.definition = new SimpleAttributeDefinitionBuilder(name, ModelType.LONG)
                .setFlags(metricType)
                .setMeasurementUnit(unit)
                .setStorageRuntime()
                .build();
    }

    @Override
    public AttributeDefinition get() {
        return this.definition;
    }

    @Override
    public ModelNode execute(RemoteCacheClientStatisticsMXBean statistics) {
        return new ModelNode(this.applyAsLong(statistics));
    }
}

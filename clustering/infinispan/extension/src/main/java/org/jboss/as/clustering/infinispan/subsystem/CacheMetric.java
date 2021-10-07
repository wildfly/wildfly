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

import org.infinispan.interceptors.impl.CacheMgmtInterceptor;
import org.jboss.as.clustering.controller.Metric;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Enumeration of management metrics for a cache.
 * @author Paul Ferraro
 */
public enum CacheMetric implements Metric<CacheMgmtInterceptor> {

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
    NUMBER_OF_ENTRIES("number-of-entries", ModelType.INT, AttributeAccess.Flag.GAUGE_METRIC) {
        @Override
        public ModelNode execute(CacheMgmtInterceptor interceptor) {
            return new ModelNode(interceptor.getNumberOfEntries());
        }
    },
    NUMBER_OF_ENTRIES_IN_MEMORY("number-of-entries-in-memory", ModelType.INT, AttributeAccess.Flag.GAUGE_METRIC) {
        @Override
        public ModelNode execute(CacheMgmtInterceptor interceptor) {
            return new ModelNode(interceptor.getNumberOfEntriesInMemory());
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
    public AttributeDefinition getDefinition() {
        return this.definition;
    }
}
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

import org.infinispan.Cache;
import org.infinispan.eviction.ActivationManager;
import org.infinispan.eviction.PassivationManager;
import org.infinispan.interceptors.AsyncInterceptor;
import org.infinispan.interceptors.impl.CacheMgmtInterceptor;
import org.infinispan.interceptors.impl.InvalidationInterceptor;
import org.jboss.as.clustering.controller.Metric;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Enumeration of management metrics for a cache.
 * @author Paul Ferraro
 */
public enum CacheMetric implements Metric<Cache<?, ?>> {

    ACTIVATIONS(MetricKeys.ACTIVATIONS, ModelType.LONG) {
        @Override
        public ModelNode execute(Cache<?, ?> cache) {
            ActivationManager manager = cache.getAdvancedCache().getComponentRegistry().getComponent(ActivationManager.class);
            return new ModelNode((manager != null) ? manager.getActivationCount() : 0);
        }
    },
    AVERAGE_READ_TIME(MetricKeys.AVERAGE_READ_TIME, ModelType.LONG) {
        @Override
        public ModelNode execute(Cache<?, ?> cache) {
            CacheMgmtInterceptor interceptor = findInterceptor(cache, CacheMgmtInterceptor.class);
            return new ModelNode((interceptor != null) ? interceptor.getAverageReadTime() : 0);
        }
    },
    AVERAGE_WRITE_TIME(MetricKeys.AVERAGE_WRITE_TIME, ModelType.LONG) {
        @Override
        public ModelNode execute(Cache<?, ?> cache) {
            CacheMgmtInterceptor interceptor = findInterceptor(cache, CacheMgmtInterceptor.class);
            return new ModelNode((interceptor != null) ? interceptor.getAverageWriteTime() : 0);
        }
    },
    CACHE_STATUS(MetricKeys.CACHE_STATUS, ModelType.STRING) {
        @Override
        public ModelNode execute(Cache<?, ?> cache) {
            return new ModelNode(cache.getStatus().toString());
        }
    },
    ELAPSED_TIME(MetricKeys.ELAPSED_TIME, ModelType.LONG) {
        @Override
        public ModelNode execute(Cache<?, ?> cache) {
            CacheMgmtInterceptor interceptor = findInterceptor(cache, CacheMgmtInterceptor.class);
            return new ModelNode((interceptor != null) ? interceptor.getTimeSinceStart() : 0);
        }
    },
    HIT_RATIO(MetricKeys.HIT_RATIO, ModelType.DOUBLE) {
        @Override
        public ModelNode execute(Cache<?, ?> cache) {
            CacheMgmtInterceptor interceptor = findInterceptor(cache, CacheMgmtInterceptor.class);
            return new ModelNode((interceptor != null) ? interceptor.getHitRatio() : 0);
        }
    },
    HITS(MetricKeys.HITS, ModelType.LONG) {
        @Override
        public ModelNode execute(Cache<?, ?> cache) {
            CacheMgmtInterceptor interceptor = findInterceptor(cache, CacheMgmtInterceptor.class);
            return new ModelNode((interceptor != null) ? interceptor.getHits() : 0);
        }
    },
    INVALIDATIONS(MetricKeys.INVALIDATIONS, ModelType.LONG) {
        @Override
        public ModelNode execute(Cache<?, ?> cache) {
            InvalidationInterceptor interceptor = findInterceptor(cache, InvalidationInterceptor.class);
            return new ModelNode((interceptor != null) ? interceptor.getInvalidations() : 0);
        }
    },
    MISSES(MetricKeys.MISSES, ModelType.LONG) {
        @Override
        public ModelNode execute(Cache<?, ?> cache) {
            CacheMgmtInterceptor interceptor = findInterceptor(cache, CacheMgmtInterceptor.class);
            return new ModelNode((interceptor != null) ? interceptor.getMisses() : 0);
        }
    },
    NUMBER_OF_ENTRIES(MetricKeys.NUMBER_OF_ENTRIES, ModelType.INT) {
        @Override
        public ModelNode execute(Cache<?, ?> cache) {
            CacheMgmtInterceptor interceptor = findInterceptor(cache, CacheMgmtInterceptor.class);
            return new ModelNode((interceptor != null) ? interceptor.getNumberOfEntries() : 0);
        }
    },
    PASSIVATIONS(MetricKeys.PASSIVATIONS, ModelType.LONG) {
        @Override
        public ModelNode execute(Cache<?, ?> cache) {
            PassivationManager manager = cache.getAdvancedCache().getComponentRegistry().getComponent(PassivationManager.class);
            return new ModelNode((manager != null) ? manager.getPassivations() : 0);
        }
    },
    READ_WRITE_RATIO(MetricKeys.READ_WRITE_RATIO, ModelType.DOUBLE) {
        @Override
        public ModelNode execute(Cache<?, ?> cache) {
            CacheMgmtInterceptor interceptor = findInterceptor(cache, CacheMgmtInterceptor.class);
            return new ModelNode((interceptor != null) ? interceptor.getReadWriteRatio() : 0);
        }
    },
    REMOVE_HITS(MetricKeys.REMOVE_HITS, ModelType.LONG) {
        @Override
        public ModelNode execute(Cache<?, ?> cache) {
            CacheMgmtInterceptor interceptor = findInterceptor(cache, CacheMgmtInterceptor.class);
            return new ModelNode((interceptor != null) ? interceptor.getRemoveHits() : 0);
        }
    },
    REMOVE_MISSES(MetricKeys.REMOVE_MISSES, ModelType.LONG) {
        @Override
        public ModelNode execute(Cache<?, ?> cache) {
            CacheMgmtInterceptor interceptor = findInterceptor(cache, CacheMgmtInterceptor.class);
            return new ModelNode((interceptor != null) ? interceptor.getRemoveMisses() : 0);
        }
    },
    STORES(MetricKeys.STORES, ModelType.LONG) {
        @Override
        public ModelNode execute(Cache<?, ?> cache) {
            CacheMgmtInterceptor interceptor = findInterceptor(cache, CacheMgmtInterceptor.class);
            return new ModelNode((interceptor != null) ? interceptor.getStores() : 0);
        }
    },
    TIME_SINCE_RESET(MetricKeys.TIME_SINCE_RESET, ModelType.LONG) {
        @Override
        public ModelNode execute(Cache<?, ?> cache) {
            CacheMgmtInterceptor interceptor = findInterceptor(cache, CacheMgmtInterceptor.class);
            return new ModelNode((interceptor != null) ? interceptor.getTimeSinceReset() : 0);
        }
    },
    ;
    private final AttributeDefinition definition;

    CacheMetric(String name, ModelType type) {
        this.definition = new SimpleAttributeDefinitionBuilder(name, type, true).setStorageRuntime().build();
    }

    @Override
    public AttributeDefinition getDefinition() {
        return this.definition;
    }

    static <T extends AsyncInterceptor> T findInterceptor(Cache<?, ?> cache, Class<T> interceptorClass) {
        return cache.getAdvancedCache().getAsyncInterceptorChain().findInterceptorExtending(interceptorClass);
    }
}
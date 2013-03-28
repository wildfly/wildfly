/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.infinispan.Cache;
import org.infinispan.interceptors.ActivationInterceptor;
import org.infinispan.interceptors.CacheMgmtInterceptor;
import org.infinispan.interceptors.InvalidationInterceptor;
import org.infinispan.interceptors.PassivationInterceptor;
import org.infinispan.interceptors.TxInterceptor;
import org.infinispan.interceptors.base.CommandInterceptor;
import org.infinispan.remoting.rpc.RpcManagerImpl;
import org.infinispan.util.concurrent.locks.LockManagerImpl;
import org.jboss.as.controller.AbstractRuntimeOnlyHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;

/**
 * A handler for all Infinispan cache container and cache metrics.
 *
 * @author Tristan Tarrant
 * @author Richard Achmatowicz
 */

public class CacheMetricsHandler extends AbstractRuntimeOnlyHandler {

    public static final CacheMetricsHandler INSTANCE = new CacheMetricsHandler();

    public enum CacheMetrics {
        CACHE_STATUS(CacheResource.CACHE_STATUS),
        // LockManager
        NUMBER_OF_LOCKS_AVAILABLE(LockingResource.NUMBER_OF_LOCKS_AVAILABLE),
        NUMBER_OF_LOCKS_HELD(LockingResource.NUMBER_OF_LOCKS_HELD),
        CONCURRENCY_LEVEL(LockingResource.CURRENT_CONCURRENCY_LEVEL),
        // CacheMgmtInterceptor
        AVERAGE_READ_TIME(CacheResource.AVERAGE_READ_TIME),
        AVERAGE_WRITE_TIME(CacheResource.AVERAGE_WRITE_TIME),
        ELAPSED_TIME(CacheResource.ELAPSED_TIME),
        EVICTIONS(EvictionResource.EVICTIONS),
        HIT_RATIO(CacheResource.HIT_RATIO),
        HITS(CacheResource.HITS),
        MISSES(CacheResource.MISSES),
        NUMBER_OF_ENTRIES(CacheResource.NUMBER_OF_ENTRIES),
        READ_WRITE_RATIO(CacheResource.READ_WRITE_RATIO),
        REMOVE_HITS(CacheResource.REMOVE_HITS),
        REMOVE_MISSES(CacheResource.REMOVE_MISSES),
        STORES(CacheResource.STORES),
        TIME_SINCE_RESET(CacheResource.TIME_SINCE_RESET),
        // RpcManager
        AVERAGE_REPLICATION_TIME(ClusteredCacheResource.AVERAGE_REPLICATION_TIME),
        REPLICATION_COUNT(ClusteredCacheResource.REPLICATION_COUNT),
        REPLICATION_FAILURES(ClusteredCacheResource.REPLICATION_FAILURES),
        SUCCESS_RATIO(ClusteredCacheResource.SUCCESS_RATIO),
        // TxInterceptor
        COMMITS(TransactionResource.COMMITS),
        PREPARES(TransactionResource.PREPARES),
        ROLLBACKS(TransactionResource.ROLLBACKS),
        // InvalidationInterceptor
        INVALIDATIONS(CacheResource.INVALIDATIONS),
        // PassivationInterceptor
        PASSIVATIONS(CacheResource.PASSIVATIONS),
        // ActivationInterceptor
        ACTIVATIONS(CacheResource.ACTIVATIONS),
        CACHE_LOADER_LOADS(BaseStoreResource.CACHE_LOADER_LOADS),
        CACHE_LOADER_MISSES(BaseStoreResource.CACHE_LOADER_MISSES);

        private static final Map<String, CacheMetrics> MAP = new HashMap<String, CacheMetrics>();

        static {
            for (CacheMetrics metric : CacheMetrics.values()) {
                MAP.put(metric.toString(), metric);
            }
        }

        final AttributeDefinition definition;

        private CacheMetrics(final AttributeDefinition definition) {
            this.definition = definition;
        }

        @Override
        public final String toString() {
            return definition.getName();
        }

        public static CacheMetrics getStat(final String stringForm) {
            return MAP.get(stringForm);
        }
    }

    @Override
    protected void executeRuntimeStep(OperationContext context, ModelNode operation) throws OperationFailedException {
        final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
        final String cacheContainerName = address.getElement(address.size() - 2).getValue();
        final String cacheName = address.getLastElement().getValue();
        final String attrName = operation.require(ModelDescriptionConstants.NAME).asString();
        CacheMetrics metric = CacheMetrics.getStat(attrName);
        final ServiceController<?> controller = context.getServiceRegistry(false).getService(CacheService.getServiceName(cacheContainerName, cacheName));

        // check that the service has been installed and started
        boolean started = controller != null && controller.getValue() != null;
        ModelNode result = new ModelNode();

        if (metric == null) {
            context.getFailureDescription().set(String.format("Unknown metric %s", attrName));
        } else if (!started) {
            // when the cache service is not available, return a null result
        } else {
            Cache<?, ?> cache = (Cache<?, ?>) controller.getValue();
            switch (metric) {
                case CACHE_STATUS:
                    result.set(cache.getAdvancedCache().getStatus().toString());
                    break;
                case CONCURRENCY_LEVEL:
                    result.set(((LockManagerImpl) cache.getAdvancedCache().getLockManager()).getConcurrencyLevel());
                    break;
                case NUMBER_OF_LOCKS_AVAILABLE:
                    result.set(((LockManagerImpl) cache.getAdvancedCache().getLockManager()).getNumberOfLocksAvailable());
                    break;
                case NUMBER_OF_LOCKS_HELD:
                    result.set(((LockManagerImpl) cache.getAdvancedCache().getLockManager()).getNumberOfLocksHeld());
                    break;
                case AVERAGE_READ_TIME: {
                    CacheMgmtInterceptor cacheMgmtInterceptor = getFirstInterceptorWhichExtends(cache.getAdvancedCache().getInterceptorChain(), CacheMgmtInterceptor.class);
                    result.set(cacheMgmtInterceptor != null ? cacheMgmtInterceptor.getAverageReadTime() : 0);
                    break;
                }
                case AVERAGE_WRITE_TIME: {
                    CacheMgmtInterceptor cacheMgmtInterceptor = getFirstInterceptorWhichExtends(cache.getAdvancedCache().getInterceptorChain(), CacheMgmtInterceptor.class);
                    result.set(cacheMgmtInterceptor != null ? cacheMgmtInterceptor.getAverageWriteTime() : 0);
                    break;
                }
                case ELAPSED_TIME: {
                    CacheMgmtInterceptor cacheMgmtInterceptor = getFirstInterceptorWhichExtends(cache.getAdvancedCache().getInterceptorChain(), CacheMgmtInterceptor.class);
                    result.set(cacheMgmtInterceptor != null ? cacheMgmtInterceptor.getElapsedTime() : 0);
                    break;
                }
                case EVICTIONS: {
                    CacheMgmtInterceptor cacheMgmtInterceptor = getFirstInterceptorWhichExtends(cache.getAdvancedCache().getInterceptorChain(), CacheMgmtInterceptor.class);
                    result.set(cacheMgmtInterceptor != null ? cacheMgmtInterceptor.getEvictions() : 0);
                    break;
                }
                case HIT_RATIO: {
                    CacheMgmtInterceptor cacheMgmtInterceptor = getFirstInterceptorWhichExtends(cache.getAdvancedCache().getInterceptorChain(), CacheMgmtInterceptor.class);
                    result.set(cacheMgmtInterceptor != null ? cacheMgmtInterceptor.getHitRatio() : 0);
                    break;
                }
                case HITS: {
                    CacheMgmtInterceptor cacheMgmtInterceptor = getFirstInterceptorWhichExtends(cache.getAdvancedCache().getInterceptorChain(), CacheMgmtInterceptor.class);
                    result.set(cacheMgmtInterceptor != null ? cacheMgmtInterceptor.getHits() : 0);
                    break;
                }
                case MISSES: {
                    CacheMgmtInterceptor cacheMgmtInterceptor = getFirstInterceptorWhichExtends(cache.getAdvancedCache().getInterceptorChain(), CacheMgmtInterceptor.class);
                    result.set(cacheMgmtInterceptor != null ? cacheMgmtInterceptor.getMisses() : 0);
                    break;
                }
                case NUMBER_OF_ENTRIES: {
                    CacheMgmtInterceptor cacheMgmtInterceptor = getFirstInterceptorWhichExtends(cache.getAdvancedCache().getInterceptorChain(), CacheMgmtInterceptor.class);
                    result.set(cacheMgmtInterceptor != null ? cacheMgmtInterceptor.getNumberOfEntries() : 0);
                    break;
                }
                case READ_WRITE_RATIO: {
                    CacheMgmtInterceptor cacheMgmtInterceptor = getFirstInterceptorWhichExtends(cache.getAdvancedCache().getInterceptorChain(), CacheMgmtInterceptor.class);
                    result.set(cacheMgmtInterceptor != null ? cacheMgmtInterceptor.getReadWriteRatio() : 0);
                    break;
                }
                case REMOVE_HITS: {
                    CacheMgmtInterceptor cacheMgmtInterceptor = getFirstInterceptorWhichExtends(cache.getAdvancedCache().getInterceptorChain(), CacheMgmtInterceptor.class);
                    result.set(cacheMgmtInterceptor != null ? cacheMgmtInterceptor.getRemoveHits() : 0);
                    break;
                }
                case REMOVE_MISSES: {
                    CacheMgmtInterceptor cacheMgmtInterceptor = getFirstInterceptorWhichExtends(cache.getAdvancedCache().getInterceptorChain(), CacheMgmtInterceptor.class);
                    result.set(cacheMgmtInterceptor != null ? cacheMgmtInterceptor.getRemoveMisses() : 0);
                    break;
                }
                case STORES: {
                    CacheMgmtInterceptor cacheMgmtInterceptor = getFirstInterceptorWhichExtends(cache.getAdvancedCache().getInterceptorChain(), CacheMgmtInterceptor.class);
                    result.set(cacheMgmtInterceptor != null ? cacheMgmtInterceptor.getStores() : 0);
                    break;
                }
                case TIME_SINCE_RESET: {
                    CacheMgmtInterceptor cacheMgmtInterceptor = getFirstInterceptorWhichExtends(cache.getAdvancedCache().getInterceptorChain(), CacheMgmtInterceptor.class);
                    result.set(cacheMgmtInterceptor != null ? cacheMgmtInterceptor.getTimeSinceReset() : 0);
                    break;
                }
                case AVERAGE_REPLICATION_TIME: {
                    result.set(((RpcManagerImpl) cache.getAdvancedCache().getRpcManager()).getAverageReplicationTime());
                    break;
                }
                case REPLICATION_COUNT:
                    result.set(((RpcManagerImpl) cache.getAdvancedCache().getRpcManager()).getReplicationCount());
                    break;
                case REPLICATION_FAILURES:
                    result.set(((RpcManagerImpl) cache.getAdvancedCache().getRpcManager()).getReplicationFailures());
                    break;
                case SUCCESS_RATIO:
                    result.set(((RpcManagerImpl) cache.getAdvancedCache().getRpcManager()).getSuccessRatioFloatingPoint());
                    break;
                case COMMITS: {
                    TxInterceptor txInterceptor = getFirstInterceptorWhichExtends(cache.getAdvancedCache()
                            .getInterceptorChain(), TxInterceptor.class);
                    result.set(txInterceptor != null ? txInterceptor.getCommits() : 0);
                    break;
                }
                case PREPARES: {
                    TxInterceptor txInterceptor = getFirstInterceptorWhichExtends(cache.getAdvancedCache()
                            .getInterceptorChain(), TxInterceptor.class);
                    result.set(txInterceptor != null ? txInterceptor.getPrepares() : 0);
                    break;
                }
                case ROLLBACKS: {
                    TxInterceptor txInterceptor = getFirstInterceptorWhichExtends(cache.getAdvancedCache()
                            .getInterceptorChain(), TxInterceptor.class);
                    result.set(txInterceptor != null ? txInterceptor.getRollbacks() : 0);
                    break;
                }
                case INVALIDATIONS: {
                    InvalidationInterceptor invInterceptor = getFirstInterceptorWhichExtends(cache.getAdvancedCache()
                            .getInterceptorChain(), InvalidationInterceptor.class);
                    result.set(invInterceptor != null ? invInterceptor.getInvalidations() : 0);
                    break;
                }
                case PASSIVATIONS: {
                    PassivationInterceptor interceptor = getFirstInterceptorWhichExtends(cache.getAdvancedCache()
                            .getInterceptorChain(), PassivationInterceptor.class);
                    result.set(interceptor != null ? interceptor.getPassivations() : "");
                    break;
                }
                /*
                case ACTIVATIONS: {
                    ActivationInterceptor interceptor = getFirstInterceptorWhichExtends(cache.getAdvancedCache()
                            .getInterceptorChain(), ActivationInterceptor.class);
                    result.set(interceptor != null ? interceptor.getActivations() : "");
                    break;
                }
                */
                case CACHE_LOADER_LOADS: {
                    ActivationInterceptor interceptor = getFirstInterceptorWhichExtends(cache.getAdvancedCache()
                            .getInterceptorChain(), ActivationInterceptor.class);
                    result.set(interceptor != null ? interceptor.getCacheLoaderLoads() : 0);
                    break;
                }
                case CACHE_LOADER_MISSES: {
                    ActivationInterceptor interceptor = getFirstInterceptorWhichExtends(cache.getAdvancedCache()
                            .getInterceptorChain(), ActivationInterceptor.class);
                    result.set(interceptor != null ? interceptor.getCacheLoaderMisses() : 0);
                    break;
                }
            }
            context.getResult().set(result);
        }
        context.completeStep(OperationContext.ResultHandler.NOOP_RESULT_HANDLER);
    }

    private static <T extends CommandInterceptor> T getFirstInterceptorWhichExtends(List<CommandInterceptor> interceptors,
                                                                                    Class<T> interceptorClass) {
        for (CommandInterceptor interceptor : interceptors) {
            boolean isSubclass = interceptorClass.isAssignableFrom(interceptor.getClass());
            if (isSubclass) {
                Collections.emptyList();
                return (T) interceptor;
            }
        }
        return null;
    }
}

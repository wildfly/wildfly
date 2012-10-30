package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
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
import org.jboss.as.clustering.infinispan.InfinispanLogger;
import org.jboss.as.controller.AbstractRuntimeOnlyHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceController;

/**
 * Handler which manages read-only access to cache runtime information (metrics)
 *
 * @author Tristan Tarrant (c) 2011 Red Hat Inc.
 */

public class CacheMetricsHandler extends AbstractRuntimeOnlyHandler {
    public static final CacheMetricsHandler INSTANCE = new CacheMetricsHandler();

    public enum CacheMetrics {
        CACHE_STATUS(MetricKeys.CACHE_STATUS, ModelType.STRING, true),
        // LockManager
        NUMBER_OF_LOCKS_AVAILABLE(MetricKeys.NUMBER_OF_LOCKS_AVAILABLE, ModelType.INT, true),
        NUMBER_OF_LOCKS_HELD(MetricKeys.NUMBER_OF_LOCKS_HELD, ModelType.INT, true),
        CONCURRENCY_LEVEL(MetricKeys.CONCURRENCY_LEVEL, ModelType.INT, true),
        // CacheMgmtInterceptor
        AVERAGE_READ_TIME(MetricKeys.AVERAGE_READ_TIME, ModelType.LONG, true),
        AVERAGE_WRITE_TIME(MetricKeys.AVERAGE_WRITE_TIME, ModelType.LONG, true),
        ELAPSED_TIME(MetricKeys.ELAPSED_TIME, ModelType.LONG, true),
        EVICTIONS(MetricKeys.EVICTIONS, ModelType.LONG, true),
        HIT_RATIO(MetricKeys.HIT_RATIO, ModelType.DOUBLE, true),
        HITS(MetricKeys.HITS, ModelType.LONG, true),
        MISSES(MetricKeys.MISSES, ModelType.LONG, true),
        NUMBER_OF_ENTRIES(MetricKeys.NUMBER_OF_ENTRIES, ModelType.INT, true),
        READ_WRITE_RATIO(MetricKeys.READ_WRITE_RATIO, ModelType.DOUBLE, true),
        REMOVE_HITS(MetricKeys.REMOVE_HITS, ModelType.LONG, true),
        REMOVE_MISSES(MetricKeys.REMOVE_MISSES, ModelType.LONG, true),
        STORES(MetricKeys.STORES, ModelType.LONG, true),
        TIME_SINCE_RESET(MetricKeys.TIME_SINCE_RESET, ModelType.LONG, true),
        // TxInterceptor
        COMMITS(MetricKeys.COMMITS, ModelType.LONG, true),
        PREPARES(MetricKeys.PREPARES, ModelType.LONG, true),
        ROLLBACKS(MetricKeys.ROLLBACKS, ModelType.LONG, true),
        // InvalidationInterceptor
        INVALIDATIONS(MetricKeys.INVALIDATIONS, ModelType.LONG, true),
        // PassivationInterceptor
        PASSIVATIONS(MetricKeys.PASSIVATIONS, ModelType.STRING, true),
        // ActivationInterceptor
        ACTIVATIONS(MetricKeys.ACTIVATIONS, ModelType.STRING, true),
        CACHE_LOADER_LOADS(MetricKeys.CACHE_LOADER_LOADS, ModelType.LONG, true),
        CACHE_LOADER_MISSES(MetricKeys.CACHE_LOADER_MISSES, ModelType.LONG, true),
        // RpcManager
        AVERAGE_REPLICATION_TIME(MetricKeys.AVERAGE_REPLICATION_TIME, ModelType.LONG, true, true),
        REPLICATION_COUNT(MetricKeys.REPLICATION_COUNT, ModelType.LONG, true, true),
        REPLICATION_FAILURES(MetricKeys.REPLICATION_FAILURES, ModelType.LONG, true, true),
        SUCCESS_RATIO(MetricKeys.SUCCESS_RATIO, ModelType.DOUBLE, true, true);

        private static final Map<String, CacheMetrics> MAP = new HashMap<String, CacheMetrics>();

        static {
            for (CacheMetrics metric : CacheMetrics.values()) {
                MAP.put(metric.toString(), metric);
            }
        }

        final AttributeDefinition definition;
        final boolean clustered;

        private CacheMetrics(final AttributeDefinition definition, final boolean clustered) {
            this.definition = definition;
            this.clustered = clustered;
        }

        private CacheMetrics(String attributeName, ModelType type, boolean allowNull) {
            this(new SimpleAttributeDefinitionBuilder(attributeName, type, allowNull).setStorageRuntime().build(), false);
        }

        private CacheMetrics(String attributeName, ModelType type, boolean allowNull, final boolean clustered) {
            this(new SimpleAttributeDefinitionBuilder(attributeName, type, allowNull).setStorageRuntime().build(), clustered);
        }

        @Override
        public final String toString() {
            return definition.getName();
        }

        public static CacheMetrics getStat(final String stringForm) {
            return MAP.get(stringForm);
        }
    }

    /*
     * Two constraints need to be dealt with here:
     * 1. There may be no started cache instance available to interrogate. Because of lazy deployment,
     * a cache instance is only started upon deployment of an application which uses that cache instance.
     * 2. The attribute name passed in may not correspond to a defined metric
     *
     * Read-only attributes have no easy way to throw an exception without negatively impacting other parts
     * of the system. Therefore in such cases, as message will be logged and a ModelNode of undefined will be returned.
     */
    @Override
    protected void executeRuntimeStep(OperationContext context, ModelNode operation) throws OperationFailedException {
        final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
        final String cacheContainerName = address.getElement(address.size() - 2).getValue();
        final String cacheName = address.getLastElement().getValue();
        final String attrName = operation.require(NAME).asString();
        final ServiceController<?> controller = context.getServiceRegistry(false).getService(CacheService.getServiceName(cacheContainerName, cacheName));
        Cache<?, ?> cache = (Cache<?, ?>) controller.getValue();

        CacheMetrics metric = CacheMetrics.getStat(attrName);
        ModelNode result = new ModelNode();

        if (metric == null) {
            InfinispanLogger.ROOT_LOGGER.cacheMetricNotDefined(metric.toString(), cacheName);
        } else if (cache == null) {
            InfinispanLogger.ROOT_LOGGER.cacheMetricNotDefined(metric.toString(), cacheName);
        } else {
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
                case ACTIVATIONS: {
                    ActivationInterceptor interceptor = getFirstInterceptorWhichExtends(cache.getAdvancedCache()
                            .getInterceptorChain(), ActivationInterceptor.class);
                    result.set(interceptor != null ? interceptor.getActivations() : "");
                    break;
                }
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
        context.completeStep();
    }

    public void registerCommonMetrics(ManagementResourceRegistration container) {
        for (CacheMetrics metric : CacheMetrics.values()) {
            if (!metric.clustered) {
                container.registerMetric(metric.definition, this);
            }
        }
    }

    public void registerClusteredMetrics(ManagementResourceRegistration container) {
        for (CacheMetrics metric : CacheMetrics.values()) {
            if (metric.clustered) {
                container.registerMetric(metric.definition, this);
            }
        }
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
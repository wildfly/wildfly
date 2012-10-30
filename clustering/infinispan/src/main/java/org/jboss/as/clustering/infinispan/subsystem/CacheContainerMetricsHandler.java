package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.HashMap;
import java.util.Map;

import org.jboss.as.clustering.infinispan.DefaultEmbeddedCacheManager;
import org.jboss.as.clustering.infinispan.InfinispanLogger;
import org.jboss.as.controller.AbstractRuntimeOnlyHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceController;

public class CacheContainerMetricsHandler extends AbstractRuntimeOnlyHandler {
    public static final CacheContainerMetricsHandler INSTANCE = new CacheContainerMetricsHandler();

    public enum CacheManagerMetrics {
        CACHE_MANAGER_STATUS(MetricKeys.CACHE_MANAGER_STATUS, ModelType.STRING, true),
        CLUSTER_NAME(MetricKeys.CLUSTER_NAME, ModelType.STRING, true, true),
        IS_COORDINATOR(MetricKeys.IS_COORDINATOR, ModelType.BOOLEAN, true, true),
        COORDINATOR_ADDRESS(MetricKeys.COORDINATOR_ADDRESS, ModelType.STRING, true, true),
        LOCAL_ADDRESS(MetricKeys.LOCAL_ADDRESS, ModelType.STRING, true, true);

        private static final Map<String, CacheManagerMetrics> MAP = new HashMap<String, CacheManagerMetrics>();

        static {
            for (CacheManagerMetrics metric : CacheManagerMetrics.values()) {
                MAP.put(metric.toString(), metric);
            }
        }

        final AttributeDefinition definition;
        final boolean clustered;

        private CacheManagerMetrics(final AttributeDefinition definition, final boolean clustered) {
            this.definition = definition;
            this.clustered = clustered;
        }

        private CacheManagerMetrics(String attributeName, ModelType type, boolean allowNull) {
            this(attributeName, type, allowNull, false);
        }

        private CacheManagerMetrics(String attributeName, ModelType type, boolean allowNull, boolean clustered) {
            this(new SimpleAttributeDefinitionBuilder(attributeName, type, allowNull).setStorageRuntime().build(), clustered);
        }

        @Override
        public final String toString() {
            return definition.getName();
        }

        public static CacheManagerMetrics getStat(final String stringForm) {
            return MAP.get(stringForm);
        }
    }

    /*
     * Two constraints need to be dealt with here:
     * 1. There may be no started cache container instance available to interrogate. Because of lazy deployment,
     * a cache container instance is only started upon deployment of an application which uses that cache instance.
     * 2. The attribute name passed in may not correspond to a defined metric
     *
     * Read-only attributes have no easy way to throw an exception without negatively impacting other parts
     * of the system. Therefore in such cases, as message will be logged and a ModelNode of undefined will be returned.
     */
    @Override
    protected void executeRuntimeStep(OperationContext context, ModelNode operation) throws OperationFailedException {
        final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
        final String cacheContainerName = address.getLastElement().getValue();
        final String attrName = operation.require(ModelDescriptionConstants.NAME).asString();
        final ServiceController<?> controller = context.getServiceRegistry(false).getService(EmbeddedCacheManagerService.getServiceName(cacheContainerName));
        DefaultEmbeddedCacheManager cacheManager = (DefaultEmbeddedCacheManager) controller.getValue();

        CacheManagerMetrics metric = CacheManagerMetrics.getStat(attrName);
        ModelNode result = new ModelNode();

        if (metric == null) {
            InfinispanLogger.ROOT_LOGGER.cacheContainerMetricNotDefined(metric.toString(), cacheContainerName);
        } else if (cacheManager == null) {
            InfinispanLogger.ROOT_LOGGER.cacheContainerMetricNotDefined(metric.toString(), cacheContainerName);
        } else {
            switch (metric) {
                case CACHE_MANAGER_STATUS:
                    result.set(cacheManager.getStatus().toString());
                    break;
                case IS_COORDINATOR:
                    result.set(cacheManager.isCoordinator());
                    break;
                case LOCAL_ADDRESS:
                    result.set(cacheManager.getAddress() != null ? cacheManager.getAddress().toString() : "N/A");
                    break;
                case COORDINATOR_ADDRESS:
                    result.set(cacheManager.getCoordinator() != null ? cacheManager.getCoordinator().toString() : "N/A");
                    break;
                case CLUSTER_NAME:
                    result.set(cacheManager.getClusterName() != null ? cacheManager.getClusterName() : "N/A");
            }
            context.getResult().set(result);
        }
        context.completeStep();
    }

    public void registerMetrics(ManagementResourceRegistration container) {
        for (CacheManagerMetrics metric : CacheManagerMetrics.values()) {
            container.registerMetric(metric.definition, this);
        }
    }
}

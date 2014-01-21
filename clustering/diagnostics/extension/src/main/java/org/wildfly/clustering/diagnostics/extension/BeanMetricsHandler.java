package org.wildfly.clustering.diagnostics.extension;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.HashMap;
import java.util.Map;

import org.jboss.as.clustering.msc.ServiceContainerHelper;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.diagnostics.services.cache.CacheManagement;
import org.wildfly.clustering.diagnostics.services.cache.CacheManagementService;
import org.wildfly.clustering.diagnostics.services.cache.CacheState;

/**
 * Handler for reading run-time only attributes from an underlying clustered SFSB session cache service.
 *
 * @author Richard Achmatowicz (c) 2013 Red Hat Inc.
 */
public class BeanMetricsHandler extends CacheBasedMetricsHandler {

    public static final BeanMetricsHandler INSTANCE = new BeanMetricsHandler();

    public enum BeanMetrics {
        CACHE_VIEW(BeanInstanceResourceDefinition.CACHE_VIEW),
        DISTRIBUTION(BeanInstanceResourceDefinition.DISTRIBUTION),
        OPERATION_STATS(BeanInstanceResourceDefinition.OPERATION_STATS),
        RPC_STATS(BeanInstanceResourceDefinition.RPC_STATS),
        TXN_STATS(BeanInstanceResourceDefinition.TXN_STATS);

        private static final Map<String, BeanMetrics> MAP = new HashMap<String, BeanMetrics>();

        static {
            for (BeanMetrics metric : BeanMetrics.values()) {
                MAP.put(metric.toString(), metric);
            }
        }

        final AttributeDefinition definition ;

        private BeanMetrics(AttributeDefinition definition) {
            this.definition = definition;
        }

        @Override
        public String toString() {
            return definition.getName();
        }

        public static BeanMetrics getStat(final String stringForm) {
            return MAP.get(stringForm);
        }
    }

    /*
     * THis handler displays cache statistics for a given deployment's bean session cache.
     *
     * An operation here has an address cluster=X/deployment=Y/bean=Z
     * We need to:
     * - get the cluster name
     * - get the deployment name
     * - get the bean name
     * - lookup the cache info for that clustered deployment from the Clustered DeploymentRepository
     * - lookup the cache using the container name and the cache name
     */
    @Override
    protected void executeRuntimeStep(OperationContext context, ModelNode operation) throws OperationFailedException {

        // get the channel name, cache name and cache attribute
        PathAddress pathAddress = PathAddress.pathAddress(operation.require(OP_ADDR));
        String channelName = pathAddress.getElement(pathAddress.size()-3).getValue();
        String deploymentName = pathAddress.getElement(pathAddress.size()-2).getValue();
        String beanName = pathAddress.getElement(pathAddress.size()-1).getValue();
        String attrName = operation.require(NAME).asString();
        BeanMetrics metric = BeanMetrics.getStat(attrName);

        // trace logging
        ClusteringDiagnosticsSubsystemLogger.ROOT_LOGGER.processingBeanMetricsRequest(channelName, deploymentName, beanName, attrName);

        // lookup the cache info for this web deployment
        String cacheName = null;
        DeploymentCacheInfo info = ClusteringDiagnosticsSubsystemHelper.getBeanDeploymentCacheInfo(context.getServiceRegistry(false), channelName, deploymentName, beanName);
        if (info == null) {
            context.getFailureDescription().set(ClusteringDiagnosticsSubsystemMessages.MESSAGES.clusteredDeploymentRepositoryNotAvailable());
            context.completeStep(OperationContext.ResultHandler.NOOP_RESULT_HANDLER);
            return;
        }
        cacheName = info.getCache();

        // lookup the channel RPC support for this channel
        ServiceName serviceName = CacheManagementService.getServiceName(channelName);
        ServiceController<CacheManagement> controller = ServiceContainerHelper.findService(context.getServiceRegistry(false), serviceName);

        // check that the service has been installed and started
        boolean started = controller != null && controller.getState().in(ServiceController.State.UP);
        ModelNode result = new ModelNode();

        if (metric == null) {
            context.getFailureDescription().set(ClusteringDiagnosticsSubsystemMessages.MESSAGES.unknownMetric(attrName));
        } else if (!started) {
            context.getFailureDescription().set(ClusteringDiagnosticsSubsystemMessages.MESSAGES.rpcServiceNotStarted(channelName));
        } else {
            CacheManagement management = controller.getValue();

            try {
                // if the cache is not available on the other node, we can get service not found!
                Map<Node, CacheState> states = management.getCacheState(cacheName);

                switch (metric) {
                    case CACHE_VIEW:
                        result.set(createCacheView(states));
                        break;
                    case DISTRIBUTION:
                        result.set(createDistribution(states));
                        break;
                    case OPERATION_STATS:
                        result.set(createOperationStats(states));
                        break;
                    case RPC_STATS:
                        result.set(createRpcStats(states));
                        break;
                    case TXN_STATS:
                        result.set(createTxnStats(states));
                        break;
                }

                // trace logging
                ClusteringDiagnosticsSubsystemLogger.ROOT_LOGGER.processedRequestResult(result.toString());

                context.getResult().set(result);

            } catch(InterruptedException ie) {
                context.getFailureDescription().set(ClusteringDiagnosticsSubsystemMessages.MESSAGES.interrupted(channelName));
                // re-interrupt to reset the interrupted flag so that outer code can handle shutdown
                Thread.currentThread().interrupt();
            }
        }
        context.completeStep(OperationContext.ResultHandler.NOOP_RESULT_HANDLER);
    }

}

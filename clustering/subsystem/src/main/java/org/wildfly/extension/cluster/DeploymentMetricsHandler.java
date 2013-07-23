package org.wildfly.extension.cluster;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.wildfly.extension.cluster.ClusterSubsystemMessages.MESSAGES;

import java.util.HashMap;
import java.util.Map;

import org.jboss.as.clustering.msc.ServiceContainerHelper;
import org.jboss.as.controller.AbstractRuntimeOnlyHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.Node;
import org.wildfly.extension.cluster.cache.CacheManagement;
import org.wildfly.extension.cluster.cache.CacheManagementService;
import org.wildfly.extension.cluster.cache.CacheState;

/**
 * Handler for reading run-time only attributes from an underlying cache service.
 *
 * @author Richard Achmatowicz (c) 2013 Red Hat Inc.
 */
public class DeploymentMetricsHandler extends AbstractRuntimeOnlyHandler {

    public static final DeploymentMetricsHandler INSTANCE = new DeploymentMetricsHandler();

    public enum DeploymentMetrics {
        CACHE_VIEW(DeploymentInstanceResourceDefinition.CACHE_VIEW),
        DISTRIBUTION(DeploymentInstanceResourceDefinition.DISTRIBUTION),
        OPERATION_STATS(DeploymentInstanceResourceDefinition.OPERATION_STATS),
        RPC_STATS(DeploymentInstanceResourceDefinition.RPC_STATS),
        TXN_STATS(DeploymentInstanceResourceDefinition.TXN_STATS);

        private static final Map<String, DeploymentMetrics> MAP = new HashMap<String, DeploymentMetrics>();

        static {
            for (DeploymentMetrics metric : DeploymentMetrics.values()) {
                MAP.put(metric.toString(), metric);
            }
        }

        final AttributeDefinition definition ;

        private DeploymentMetrics(AttributeDefinition definition) {
            this.definition = definition;
        }

        @Override
        public String toString() {
            return definition.getName();
        }

        public static DeploymentMetrics getStat(final String stringForm) {
            return MAP.get(stringForm);
        }
    }

    @Override
    protected void executeRuntimeStep(OperationContext context, ModelNode operation) throws OperationFailedException {

        // get the channel name, cache name and cache attribute
        PathAddress pathAddress = PathAddress.pathAddress(operation.require(OP_ADDR));
        String channelName = pathAddress.getElement(pathAddress.size()-2).getValue();
        String cacheName = pathAddress.getElement(pathAddress.size()-1).getValue();
        String attrName = operation.require(NAME).asString();
        DeploymentMetrics metric = DeploymentMetrics.getStat(attrName);

        // lookup the channel RPC support for this channel
        ServiceName serviceName = CacheManagementService.getServiceName(channelName);
        ServiceController<CacheManagement> controller = ServiceContainerHelper.findService(context.getServiceRegistry(false), serviceName);

        // check that the service has been installed and started
        boolean started = controller != null && controller.getState().in(ServiceController.State.UP);
        ModelNode result = new ModelNode();

        if (metric == null) {
            context.getFailureDescription().set(ClusterSubsystemMessages.MESSAGES.unknownMetric(attrName));
        } else if (!started) {
            context.getFailureDescription().set(ClusterSubsystemMessages.MESSAGES.rpcServiceNotStarted(channelName));
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

                context.getResult().set(result);

            } catch(InterruptedException ie) {
                context.getFailureDescription().set(ClusterSubsystemMessages.MESSAGES.interrupted(channelName));
            }
        }
        context.completeStep(OperationContext.ResultHandler.NOOP_RESULT_HANDLER);
    }

    /*
     * NOTE: for {A,B,C} if a cache is undeployed on B, we do not want the replies displayed
     * on A and C to refer to B. We use rsp.isInView() for this.
     */

    private ModelNode createCacheView(Map<Node, CacheState> states) {
        ModelNode result = new ModelNode();
        for (Map.Entry<Node, CacheState> entry: states.entrySet()) {
            Node node = entry.getKey();
            CacheState state = entry.getValue();
            result.add(node.getName(), state.getView());
        }
        return result;
    }

    private ModelNode createDistribution(Map<Node, CacheState> states) {
        ModelNode result = new ModelNode();
        for (Map.Entry<Node, CacheState> entry: states.entrySet()) {
            Node node = entry.getKey();
            CacheState state = entry.getValue();
            result.add(node.getName(), MESSAGES.cacheDistributionStats(state.getEntries()));
        }
        return result;
    }

    private ModelNode createOperationStats(Map<Node, CacheState> states) {
        ModelNode result = new ModelNode();
        for (Map.Entry<Node, CacheState> entry: states.entrySet()) {
            Node node = entry.getKey();
            CacheState state = entry.getValue();
            String stats = MESSAGES.cacheOperationStats(state.getHits(), state.getMisses(), state.getStores(), state.getRemoveHits(), state.getRemoveMisses());
            result.add(node.getName(), stats);
        }
        return result;
    }

    private ModelNode createRpcStats(Map<Node, CacheState> states) {
        ModelNode result = new ModelNode();
        for (Map.Entry<Node, CacheState> entry: states.entrySet()) {
            Node node = entry.getKey();
            CacheState state = entry.getValue();
            String stats = MESSAGES.cacheRPCStats(state.getRpcCount(), state.getRpcFailures());
            result.add(node.getName(), stats);
        }
        return result;
    }

    private ModelNode createTxnStats(Map<Node, CacheState> states) {
        ModelNode result = new ModelNode();
        for (Map.Entry<Node, CacheState> entry: states.entrySet()) {
            Node node = entry.getKey();
            CacheState state = entry.getValue();
            String stats = MESSAGES.cacheTxnStats(state.getPrepares(), state.getCommits(), state.getRollbacks());
            result.add(node.getName(), stats);
        }
        return result;
    }
}

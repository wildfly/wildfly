package org.wildfly.extension.cluster;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.as.clustering.management.support.impl.ManagementAPIClusterSupport;
import org.jboss.as.clustering.management.support.impl.ManagementAPIClusterSupportService;
import org.jboss.as.clustering.management.support.impl.RemoteCacheResponse;
import org.jboss.as.clustering.msc.ServiceContainerHelper;
import org.jboss.as.controller.AbstractRuntimeOnlyHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

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
        ServiceName rpcServiceName = ManagementAPIClusterSupportService.getServiceName(channelName);
        ServiceController<ManagementAPIClusterSupport> controller = ServiceContainerHelper.getService(context.getServiceRegistry(false), rpcServiceName);

        // check that the service has been installed and started
        boolean started = controller != null && controller.getValue() != null;
        ModelNode result = new ModelNode();

        if (metric == null) {
            context.getFailureDescription().set(ClusterSubsystemMessages.MESSAGES.unknownMetric(attrName));
        } else if (!started) {
            context.getFailureDescription().set(ClusterSubsystemMessages.MESSAGES.rpcServiceNotStarted(channelName));
        } else {
            ManagementAPIClusterSupport support = (ManagementAPIClusterSupport) controller.getValue();

            try {
                // if the cache is not available on the other node, we can get service not found!
                List<RemoteCacheResponse> rsps = support.getCacheState(channelName, cacheName);

                switch (metric) {
                    case CACHE_VIEW:
                        result.set(createCacheView(rsps));
                        break;
                    case DISTRIBUTION:
                        result.set(createDistribution(rsps));
                        break;
                    case OPERATION_STATS:
                        result.set(createOperationStats(rsps));
                        break;
                    case RPC_STATS:
                        result.set(createRpcStats(rsps));
                        break;
                    case TXN_STATS:
                        result.set(createTxnStats(rsps));
                        break;
                }

                context.getResult().set(result);

            } catch(InterruptedException ie) {
                context.getFailureDescription().set(ClusterSubsystemMessages.MESSAGES.interrupted(channelName));
            }
        }
        context.completeStep(OperationContext.ResultHandler.NOOP_RESULT_HANDLER);
    }

    private ModelNode createCacheView(List<RemoteCacheResponse> rsps) {
        ModelNode result = new ModelNode();
        for (RemoteCacheResponse rsp : rsps) {
            // create a LIST of PROPERTY
            result.add(rsp.getResponder().getName(), rsp.getView());
        }
        return result;
    }

    private ModelNode createDistribution(List<RemoteCacheResponse> rsps) {
        ModelNode result = new ModelNode();
        for (RemoteCacheResponse rsp : rsps) {
            // create a LIST of PROPERTY
            String stats = String.format("entries: %s", rsp.getEntries());
            result.add(rsp.getResponder().getName(), stats);
        }
        return result;
    }

    private ModelNode createOperationStats(List<RemoteCacheResponse> rsps) {
        ModelNode result = new ModelNode();
        for (RemoteCacheResponse rsp : rsps) {
            // create a LIST of PROPERTY
            String stats = String.format("get hits: %s get misses: %s, puts %s, remove hits: %s remove misses: %s",
                    rsp.getHits(), rsp.getMisses(), rsp.getStores(), rsp.getRemoveHits(), rsp.getRemoveMisses());
            result.add(rsp.getResponder().getName(), stats);
        }
        return result;
    }

    private ModelNode createRpcStats(List<RemoteCacheResponse> rsps) {
        ModelNode result = new ModelNode();
        for (RemoteCacheResponse rsp : rsps) {
            // create a LIST of PROPERTY
            String stats = String.format("RPC count: %s, RPC misses: %s", rsp.getRPCCount(), rsp.getRPCMisses());
            result.add(rsp.getResponder().getName(), stats);
        }
        return result;
    }

    private ModelNode createTxnStats(List<RemoteCacheResponse> rsps) {
        ModelNode result = new ModelNode();
        for (RemoteCacheResponse rsp : rsps) {
            // create a LIST of PROPERTY
            String stats = String.format("prepares: %s, commits: %s, rollbacks: %s", rsp.getPrepares(), rsp.getCommits(), rsp.getRollbacks());
            result.add(rsp.getResponder().getName(), stats);
        }
        return result;
    }

}

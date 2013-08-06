package org.wildfly.clustering.monitor.extension;

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
import org.wildfly.clustering.monitor.services.cache.CacheManagement;
import org.wildfly.clustering.monitor.services.cache.CacheManagementService;
import org.wildfly.clustering.monitor.services.cache.CacheState;

/**
 * Handler for reading run-time only attributes from an underlying clustered web session cache service.
 *
 * @author Richard Achmatowicz (c) 2013 Red Hat Inc.
 */
public class WebMetricsHandler extends CacheBasedMetricsHandler {

    public static final WebMetricsHandler INSTANCE = new WebMetricsHandler();

    public enum WebMetrics {
        CACHE_VIEW(WebInstanceResourceDefinition.CACHE_VIEW),
        DISTRIBUTION(WebInstanceResourceDefinition.DISTRIBUTION),
        OPERATION_STATS(WebInstanceResourceDefinition.OPERATION_STATS),
        RPC_STATS(WebInstanceResourceDefinition.RPC_STATS),
        TXN_STATS(WebInstanceResourceDefinition.TXN_STATS);

        private static final Map<String, WebMetrics> MAP = new HashMap<String, WebMetrics>();

        static {
            for (WebMetrics metric : WebMetrics.values()) {
                MAP.put(metric.toString(), metric);
            }
        }

        final AttributeDefinition definition ;

        private WebMetrics(AttributeDefinition definition) {
            this.definition = definition;
        }

        @Override
        public String toString() {
            return definition.getName();
        }

        public static WebMetrics getStat(final String stringForm) {
            return MAP.get(stringForm);
        }
    }

    /*
     * THis handler displays cache statistics for a given deplooyment's web session cache.
     *
     * An operation here has an address cluster=X/deployment=Y/web=WEB
     * We need to:
     * - get the cluster name
     * - get the deployment name
     * - lookup the cache info for that clustered deployment from the Clustered DeploymentRepository
     * - lookup the cache using the container name and the cache name
     */
    @Override
    protected void executeRuntimeStep(OperationContext context, ModelNode operation) throws OperationFailedException {

        // get the channel name, cache name and cache attribute
        PathAddress pathAddress = PathAddress.pathAddress(operation.require(OP_ADDR));
        String channelName = pathAddress.getElement(pathAddress.size()-3).getValue();
        String deploymentName = pathAddress.getElement(pathAddress.size()-2).getValue();
        String attrName = operation.require(NAME).asString();
        WebMetrics metric = WebMetrics.getStat(attrName);

        // lookup the cache info for this web deployment
        String cacheName = null;
        DeploymentCacheInfo info = ClusterSubsystemHelper.getWebDeploymentCacheInfo(context.getServiceRegistry(false), channelName, deploymentName);
        if (info == null) {
            context.getFailureDescription().set(ClusterSubsystemMessages.MESSAGES.clusteredDeploymentRepositoryNotAvailable());
            context.completeStep(OperationContext.ResultHandler.NOOP_RESULT_HANDLER);
            return;
        }
        cacheName = info.getCache();

        // lookup the channel RPC support for this channel
        ServiceName serviceName = CacheManagementService.getServiceName(channelName);
        ServiceController<CacheManagement> controller = ServiceContainerHelper.findService(context.getServiceRegistry(false), serviceName);

        // check that the service has been installed and started
        boolean started = controller != null && controller.getState().in(ServiceController.State.UP);
        if (!started && controller != null) {
            try {
              // start the RPC service
              ClusterSubsystemHelper.startService(controller);
              started = true;
            } catch (Exception e) {
                // this will be handled below
           }
        }
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
}

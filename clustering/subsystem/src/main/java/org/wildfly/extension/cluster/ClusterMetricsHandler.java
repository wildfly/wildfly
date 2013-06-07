package org.wildfly.extension.cluster;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.wildfly.extension.cluster.ClusterSubsystemMessages.MESSAGES;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.as.clustering.management.support.impl.ManagementAPIClusterSupport;
import org.jboss.as.clustering.management.support.impl.ManagementAPIClusterSupportService;
import org.jboss.as.clustering.management.support.impl.RemoteClusterResponse;
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
 * Handler for reading run-time only attributes from an underlying channel service.
 *
 * @author Richard Achmatowicz (c) 2013 Red Hat Inc.
 */
public class ClusterMetricsHandler extends AbstractRuntimeOnlyHandler {

    public static final ClusterMetricsHandler INSTANCE = new ClusterMetricsHandler();

    public enum ClusterMetrics {
        RPC_STATS(ClusterInstanceResourceDefinition.RPC_STATS),
        VIEW(ClusterInstanceResourceDefinition.VIEW),
        VIEW_HISTORY(ClusterInstanceResourceDefinition.VIEW_HISTORY);

        private static final Map<String, ClusterMetrics> MAP = new HashMap<String, ClusterMetrics>();

        static {
            for (ClusterMetrics metric : ClusterMetrics.values()) {
                MAP.put(metric.toString(), metric);
            }
        }

        final AttributeDefinition definition ;

        private ClusterMetrics(AttributeDefinition definition) {
            this.definition = definition;
        }

        @Override
        public String toString() {
            return definition.getName();
        }

        public static ClusterMetrics getStat(final String stringForm) {
            return MAP.get(stringForm);
        }
    }

    @Override
    protected void executeRuntimeStep(OperationContext context, ModelNode operation) throws OperationFailedException {

        // get the channel name and channel attribute
        PathAddress pathAddress = PathAddress.pathAddress(operation.require(OP_ADDR));
        String channelName = pathAddress.getElement(pathAddress.size()-1).getValue();
        String attrName = operation.require(NAME).asString();
        ClusterMetrics metric = ClusterMetrics.getStat(attrName);

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
                List<RemoteClusterResponse> rsps = support.getClusterState(channelName);

                switch (metric) {
                    case RPC_STATS:
                        result.set(createClusterRPCStats(rsps));
                        break;
                    case VIEW:
                        result.set(createClusterView(rsps));
                        break;
                    case VIEW_HISTORY:
                        result.set(createClusterViewHistory(rsps));
                        break;
                }

                context.getResult().set(result);

            } catch (InterruptedException ie) {
                context.getFailureDescription().set(ClusterSubsystemMessages.MESSAGES.interrupted(channelName));
                // don't swallow the interrupt
                Thread.currentThread().interrupt();
            }
        }
        context.completeStep(OperationContext.ResultHandler.NOOP_RESULT_HANDLER);
    }

    private ModelNode createClusterRPCStats(List<RemoteClusterResponse> rsps) {
        ModelNode result = new ModelNode();
        for (RemoteClusterResponse rsp : rsps) {
            // create a LIST of PROPERTY
            int unicasts = rsp.getAsyncUnicasts() + rsp.getSyncUnicasts();
            int multicasts = rsp.getAsyncMulticasts() + rsp.getSyncMulticasts();
            int anycasts = rsp.getAsyncAnycasts() + rsp.getSyncAnycasts();
            result.add(rsp.getResponder().getName(), MESSAGES.clusterRPCStats(unicasts, multicasts, anycasts));
        }
        return result;
    }

    private ModelNode createClusterView(List<RemoteClusterResponse> rsps) {
        ModelNode result = new ModelNode();
        for (RemoteClusterResponse rsp : rsps) {
            // create a LIST of PROPERTY
            result.add(rsp.getResponder().getName(), rsp.getView());
        }
        return result;
    }

    private ModelNode createClusterViewHistory(List<RemoteClusterResponse> rsps) {
        ModelNode result = new ModelNode();
        for (RemoteClusterResponse rsp : rsps) {
            // create a LIST of PROPERTY
            String viewHistory = rsp.getViewHistory();
            if (viewHistory != null) {
                String[] viewHistoryElements = viewHistory.split("\n");
                ModelNode parts = new ModelNode();
                for (String viewHistoryElement : viewHistoryElements) {
                    parts.add(new ModelNode(viewHistoryElement));
                }
                result.add(rsp.getResponder().getName(), parts);
            } else {
                result.add(rsp.getResponder().getName(), new ModelNode());
            }
        }
        return result;
    }
}

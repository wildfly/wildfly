package org.wildfly.clustering.monitor.extension;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

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
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.monitor.services.channel.ChannelManagement;
import org.wildfly.clustering.monitor.services.channel.ChannelManagementService;
import org.wildfly.clustering.monitor.services.channel.ChannelState;

/**
 * Handler for reading run-time only attributes from an underlying channel service.
 *
 * @author Richard Achmatowicz (c) 2013 Red Hat Inc.
 */
public class ClusterMetricsHandler extends AbstractRuntimeOnlyHandler {

    public static final ClusterMetricsHandler INSTANCE = new ClusterMetricsHandler();

    // JSON string formats for results
    private final String RPC_STATS = "{ \"unicasts\":\"%s\", \"multicasts\":\"%s\", \"anycasts\":\"%s\" }";

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
        ServiceName serviceName = ChannelManagementService.getServiceName(channelName);
        ServiceController<ChannelManagement> controller = ServiceContainerHelper.findService(context.getServiceRegistry(false), serviceName);

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
            ChannelManagement management = controller.getValue();

            try {
                Map<Node, ChannelState> states = management.getClusterState();

                switch (metric) {
                    case RPC_STATS:
                        result.set(createClusterRPCStats(states));
                        break;
                    case VIEW:
                        result.set(createClusterView(states));
                        break;
                    case VIEW_HISTORY:
                        result.set(createClusterViewHistory(states));
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

    private ModelNode createClusterRPCStats(Map<Node, ChannelState> states) {
        ModelNode result = new ModelNode();
        for (Map.Entry<Node, ChannelState> entry: states.entrySet()) {
            Node node = entry.getKey();
            ChannelState state = entry.getValue();
            // create a LIST of PROPERTY
            int unicasts = state.getRpcStatistics().get(ChannelState.RpcType.ASYNC_UNICAST).intValue() + state.getRpcStatistics().get(ChannelState.RpcType.SYNC_UNICAST).intValue();
            int multicasts = state.getRpcStatistics().get(ChannelState.RpcType.ASYNC_MULTICAST).intValue() + state.getRpcStatistics().get(ChannelState.RpcType.SYNC_MULTICAST).intValue();
            int anycasts = state.getRpcStatistics().get(ChannelState.RpcType.ASYNC_ANYCAST).intValue() + state.getRpcStatistics().get(ChannelState.RpcType.SYNC_ANYCAST).intValue();

            String JSONString = String.format(RPC_STATS, unicasts, multicasts, anycasts);
            ModelNode stats = ModelNode.fromJSONString(JSONString);
            result.add(node.getName(), stats.toJSONString(true));
        }
        return result;
    }

    private ModelNode createClusterView(Map<Node, ChannelState> states) {
        ModelNode result = new ModelNode();
        for (Map.Entry<Node, ChannelState> entry: states.entrySet()) {
            Node node = entry.getKey();
            ChannelState state = entry.getValue();
            // create a LIST of PROPERTY
            result.add(node.getName(), state.getView());
        }
        return result;
    }

    private ModelNode createClusterViewHistory(Map<Node, ChannelState> states) {
        ModelNode result = new ModelNode();
        for (Map.Entry<Node, ChannelState> entry: states.entrySet()) {
            Node node = entry.getKey();
            ChannelState state = entry.getValue();
            // create a LIST of PROPERTY
            String viewHistory = state.getViewHistory();
            if (viewHistory != null) {
                String[] viewHistoryElements = viewHistory.split("\n");
                ModelNode parts = new ModelNode();
                for (String viewHistoryElement : viewHistoryElements) {
                    parts.add(new ModelNode(viewHistoryElement));
                }
                result.add(node.getName(), parts);
            } else {
                result.add(node.getName(), new ModelNode());
            }
        }
        return result;
    }
}

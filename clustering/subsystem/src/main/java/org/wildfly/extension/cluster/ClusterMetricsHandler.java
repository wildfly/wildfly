package org.wildfly.extension.cluster;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.as.clustering.jgroups.JGroupsMessages;
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
        VIEW(ClusterInstanceResourceDefinition.VIEW);

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
            context.getFailureDescription().set(JGroupsMessages.MESSAGES.unknownMetric(attrName));
        } else if (!started) {
            System.out.println("RPC service not started");
        } else {
            ManagementAPIClusterSupport support = (ManagementAPIClusterSupport) controller.getValue();
            List<RemoteClusterResponse> rsps = support.getClusterState(channelName);

            switch (metric) {
                case VIEW:
                    result.set(createClusterView(rsps));
                    break;
            }
            context.getResult().set(result);
        }
        context.completeStep(OperationContext.ResultHandler.NOOP_RESULT_HANDLER);
    }

    private ModelNode createClusterView(List<RemoteClusterResponse> rsps) {
        ModelNode result = new ModelNode();
        for (RemoteClusterResponse rsp : rsps) {
            // create a LIST of PROPERTY
            result.add(rsp.getResponder().getName(), rsp.getView());
        }
        return result;
    }
}

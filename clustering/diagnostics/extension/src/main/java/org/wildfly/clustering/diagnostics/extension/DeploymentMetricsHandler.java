package org.wildfly.clustering.diagnostics.extension;

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
import org.jboss.as.ejb3.deployment.DeploymentModuleIdentifier;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.diagnostics.services.channel.ChannelManagement;
import org.wildfly.clustering.diagnostics.services.channel.ChannelManagementService;

/**
 * Handler for reading run-time only attributes from an underlying deployment service.
 * <p/>
 * The deployment service can be referenced by using its service name.
 *
 * @author Richard Achmatowicz (c) 2013 Red Hat Inc.
 */
public class DeploymentMetricsHandler extends AbstractRuntimeOnlyHandler {

    public static final DeploymentMetricsHandler INSTANCE = new DeploymentMetricsHandler();

    // JSON string format for results
    private final String MODULE_IDENTIFIER_STATS = "{ \"application name\":\"%s\", \"module name\":\"%s\", \"distinct name\":\"%s\" }";

    public enum DeploymentMetrics {
        MODULE_IDENTIFIER(DeploymentInstanceResourceDefinition.MODULE_IDENTIFIER);

        private static final Map<String, DeploymentMetrics> MAP = new HashMap<String, DeploymentMetrics>();

        static {
            for (DeploymentMetrics metric : DeploymentMetrics.values()) {
                MAP.put(metric.toString(), metric);
            }
        }

        final AttributeDefinition definition;

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

        // get the channel name and channel attribute
        PathAddress pathAddress = PathAddress.pathAddress(operation.require(OP_ADDR));
        String channelName = pathAddress.getElement(pathAddress.size() - 2).getValue();
        String deploymentName = pathAddress.getElement(pathAddress.size() - 1).getValue();
        String attrName = operation.require(NAME).asString();
        DeploymentMetrics metric = DeploymentMetrics.getStat(attrName);

        // lookup the channel RPC support for this channel
        ServiceName serviceName = ChannelManagementService.getServiceName(channelName);
        ServiceController<ChannelManagement> controller = ServiceContainerHelper.findService(context.getServiceRegistry(false), serviceName);

        // trace logging
        ClusteringDiagnosticsSubsystemLogger.ROOT_LOGGER.processingDeploymentMetricsRequest(channelName, deploymentName, attrName);

        // check that the service has been installed and started
        boolean started = controller != null && controller.getState().in(ServiceController.State.UP);
        ModelNode result = new ModelNode();

        if (metric == null) {
            context.getFailureDescription().set(ClusteringDiagnosticsSubsystemMessages.MESSAGES.unknownMetric(attrName));
        } else if (!started) {
            context.getFailureDescription().set(ClusteringDiagnosticsSubsystemMessages.MESSAGES.rpcServiceNotStarted(channelName));
        } else {
            // we don't actually use this at the moment - this may change
            ChannelManagement management = controller.getValue();

            switch (metric) {
                case MODULE_IDENTIFIER:
                    DeploymentModuleIdentifier id = ClusteringDiagnosticsSubsystemHelper.getDeploymentModuleIdentifier(context.getServiceRegistry(false), deploymentName);
                    String JSONString = String.format(MODULE_IDENTIFIER_STATS, id.getApplicationName(), id.getModuleName(), id.getDistinctName());
                    ModelNode stats = ModelNode.fromJSONString(JSONString);
                    result.set(stats.toJSONString(true));
                    break;
            }

            // trace logging
            ClusteringDiagnosticsSubsystemLogger.ROOT_LOGGER.processedRequestResult(result.toString());

            context.getResult().set(result);
        }
        context.completeStep(OperationContext.ResultHandler.NOOP_RESULT_HANDLER);
    }

}

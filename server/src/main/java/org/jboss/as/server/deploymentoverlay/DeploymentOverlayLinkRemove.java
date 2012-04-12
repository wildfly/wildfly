package org.jboss.as.server.deploymentoverlay;

import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.server.deploymentoverlay.service.DeploymentOverlayLinkService;
import org.jboss.as.server.deploymentoverlay.service.DeploymentOverlayPriority;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

/**
 *
 * Removes a link between a deployment overlay and a deployment
 *
 * @author Stuart Douglas
 */
public class DeploymentOverlayLinkRemove extends AbstractRemoveStepHandler {

    private final DeploymentOverlayPriority priority;

    public DeploymentOverlayLinkRemove(final DeploymentOverlayPriority priority) {
        this.priority = priority;
    }

    @Override
    protected void recoverServices(final OperationContext context, final ModelNode operation, final ModelNode model) throws OperationFailedException {
        final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        final String name = address.getLastElement().getValue();
        final String deployment = DeploymentOverlayLinkDefinition.DEPLOYMENT.resolveModelAttribute(context, model).asString();
        final String deploymentOverlay = DeploymentOverlayLinkDefinition.DEPLOYMENT_OVERLAY.resolveModelAttribute(context, model).asString();
        DeploymentOverlayLinkAdd.installServices(context, null, null, name, deployment, deploymentOverlay, priority);
    }

    @Override
    protected void performRuntime(final OperationContext context, final ModelNode operation, final ModelNode model) throws OperationFailedException {
        final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        final String name = address.getLastElement().getValue();
        final ServiceName serviceName = DeploymentOverlayLinkService.SERVICE_NAME.append(name);
        context.removeService(serviceName);
    }

}

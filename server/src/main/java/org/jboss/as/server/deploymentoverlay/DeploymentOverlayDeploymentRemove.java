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
 * Removes a link between a deployment overlay and a deployment
 *
 * @author Stuart Douglas
 */
public class DeploymentOverlayDeploymentRemove extends AbstractRemoveStepHandler {

    private final DeploymentOverlayPriority priority;

    public DeploymentOverlayDeploymentRemove(final DeploymentOverlayPriority priority) {
        this.priority = priority;
    }

    @Override
    protected void recoverServices(final OperationContext context, final ModelNode operation, final ModelNode model) throws OperationFailedException {
        final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        final String name = address.getLastElement().getValue();
        final String deploymentOverlay = address.getElement(address.size() - 2).getValue();
        final Boolean regularExpression = DeploymentOverlayDeploymentDefinition.REGULAR_EXPRESSION.resolveModelAttribute(context, model).asBoolean();
        DeploymentOverlayDeploymentAdd.installServices(context, null, null, name, deploymentOverlay, regularExpression, priority);
    }

    @Override
    protected void performRuntime(final OperationContext context, final ModelNode operation, final ModelNode model) throws OperationFailedException {
        final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        final String name = address.getLastElement().getValue();
        final String deploymentOverlay =address.getElement(address.size() - 2).getValue();
        final ServiceName serviceName = DeploymentOverlayLinkService.SERVICE_NAME.append(deploymentOverlay).append(name);
        context.removeService(serviceName);
    }

}

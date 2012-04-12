package org.jboss.as.server.deploymentoverlay;

import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.server.deploymentoverlay.service.DeploymentOverlayPriority;
import org.jboss.as.server.deploymentoverlay.service.DeploymentOverlayService;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT_OVERLAY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

/**
 *
 * Removes a deployment overlay
 *
 * @author Stuart Douglas
 */
public class DeploymentOverlayRemove extends AbstractRemoveStepHandler {

    public static final DeploymentOverlayRemove INSTANCE = new DeploymentOverlayRemove();

    @Override
    protected void recoverServices(final OperationContext context, final ModelNode operation, final ModelNode model) throws OperationFailedException {
        final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        final String name = address.getLastElement().getValue();
        final DeploymentOverlayPriority priority = address.getElement(0).getKey().equals(DEPLOYMENT_OVERLAY) ? DeploymentOverlayPriority.SERVER : DeploymentOverlayPriority.SERVER_GROUP;

        DeploymentOverlayAdd.INSTANCE.installServices(context, null, null, name);
    }

    @Override
    protected void performRuntime(final OperationContext context, final ModelNode operation, final ModelNode model) throws OperationFailedException {
        final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        final String name = address.getLastElement().getValue();
        final ServiceName serviceName = DeploymentOverlayService.SERVICE_NAME.append(name);
        context.removeService(serviceName);
    }

}

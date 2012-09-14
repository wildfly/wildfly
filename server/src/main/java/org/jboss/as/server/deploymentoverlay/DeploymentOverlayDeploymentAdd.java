package org.jboss.as.server.deploymentoverlay;

import java.util.List;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.server.deploymentoverlay.service.DeploymentOverlayIndexService;
import org.jboss.as.server.deploymentoverlay.service.DeploymentOverlayLinkService;
import org.jboss.as.server.deploymentoverlay.service.DeploymentOverlayPriority;
import org.jboss.as.server.deploymentoverlay.service.DeploymentOverlayService;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

/**
 * @author Stuart Douglas
 */
public class DeploymentOverlayDeploymentAdd extends AbstractAddStepHandler {

    private final DeploymentOverlayPriority priority;

    public DeploymentOverlayDeploymentAdd(final DeploymentOverlayPriority priority) {
        this.priority = priority;
    }

    @Override
    protected void populateModel(final ModelNode operation, final ModelNode model) throws OperationFailedException {
        for(AttributeDefinition attr : DeploymentOverlayDeploymentDefinition.attributes()) {
            attr.validateAndSet(operation, model);
        }
    }

    @Override
    protected void performRuntime(final OperationContext context, final ModelNode operation, final ModelNode model, final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers) throws OperationFailedException {
        final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        final String name = address.getLastElement().getValue();
        final String deploymentOverlay =address.getElement(address.size() - 2).getValue();
        final Boolean regularExpression = DeploymentOverlayDeploymentDefinition.REGULAR_EXPRESSION.resolveModelAttribute(context, model).asBoolean();
        installServices(context, verificationHandler, newControllers, name, deploymentOverlay, regularExpression, priority);
    }

    static void installServices(final OperationContext context, final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers, final String name, final String deploymentOverlay, final boolean regularExpression, final DeploymentOverlayPriority priority) {
        final DeploymentOverlayLinkService service = new DeploymentOverlayLinkService(name, regularExpression, priority);

        final ServiceName serviceName = DeploymentOverlayLinkService.SERVICE_NAME.append(deploymentOverlay).append(name);
        ServiceBuilder<DeploymentOverlayLinkService> builder = context.getServiceTarget().addService(serviceName, service)
                .addDependency(DeploymentOverlayIndexService.SERVICE_NAME, DeploymentOverlayIndexService.class, service.getDeploymentOverlayIndexServiceInjectedValue())
                .addDependency(DeploymentOverlayService.SERVICE_NAME.append(deploymentOverlay), DeploymentOverlayService.class, service.getDeploymentOverlayServiceInjectedValue());
        if(verificationHandler != null) {
            builder.addListener(verificationHandler);
        }
        final ServiceController<DeploymentOverlayLinkService> controller = builder.install();
        if(newControllers != null) {
            newControllers.add(controller);
        }
    }
}

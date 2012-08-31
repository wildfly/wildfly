package org.jboss.as.server.deploymentoverlay;

import java.util.List;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.server.deploymentoverlay.service.DeploymentOverlayService;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT_OVERLAY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

/**
 * @author Stuart Douglas
 */
public class DeploymentOverlayAdd extends AbstractAddStepHandler {

    public static final DeploymentOverlayAdd INSTANCE = new DeploymentOverlayAdd();

    @Override
    public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {

        //check that if this is a server group level op the referenced deployment overlay exists
        final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        if (address.size() > 1) {
            final String name = address.getLastElement().getValue();
            final Resource deploymentOverlayResource = context.readResourceFromRoot(PathAddress.pathAddress(PathElement.pathElement(DEPLOYMENT_OVERLAY, name)));
        }
        super.execute(context, operation);
    }

    @Override
    protected void populateModel(final ModelNode operation, final ModelNode model) throws OperationFailedException {
        for (AttributeDefinition attr : DeploymentOverlayDefinition.attributes()) {
            attr.validateAndSet(operation, model);
        }
    }

    @Override
    protected void performRuntime(final OperationContext context, final ModelNode operation, final ModelNode model, final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers) throws OperationFailedException {
        final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        final String name = address.getLastElement().getValue();
        installServices(context, verificationHandler, newControllers, name);
    }

    void installServices(final OperationContext context, final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers, final String name) {
        final DeploymentOverlayService service = new DeploymentOverlayService(name);
        final ServiceName serviceName = DeploymentOverlayService.SERVICE_NAME.append(name);
        ServiceBuilder<DeploymentOverlayService> builder = context.getServiceTarget().addService(serviceName, service);
        if (verificationHandler != null) {
            builder.addListener(verificationHandler);
        }
        final ServiceController<DeploymentOverlayService> controller = builder.install();
        if (newControllers != null) {
            newControllers.add(controller);
        }
    }
}

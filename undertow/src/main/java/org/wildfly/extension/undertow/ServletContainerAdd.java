package org.wildfly.extension.undertow;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.List;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 */
final class ServletContainerAdd extends AbstractBoottimeAddStepHandler {
    static final ServletContainerAdd INSTANCE = new ServletContainerAdd();

    ServletContainerAdd() {
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        for (AttributeDefinition def : ServletContainerDefinition.INSTANCE.getAttributes()) {
            def.validateAndSet(operation, model);
        }
    }

    @Override
    protected void performBoottime(OperationContext context, ModelNode operation, final ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {
        final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        final String name = address.getLastElement().getValue();

        final ServletContainerService container = new ServletContainerService();
        final ServiceTarget target = context.getServiceTarget();
        newControllers.add(target.addService(UndertowService.SERVLET_CONTAINER.append(name), container)
                .setInitialMode(ServiceController.Mode.ON_DEMAND)
                .install());

    }
}

package org.jboss.as.undertow;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.List;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 */
class ServerAdd extends AbstractBoottimeAddStepHandler {
    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        for (SimpleAttributeDefinition def : ServerDefinition.ATTRIBUTES) {
            def.validateAndSet(operation, model);
        }
    }

    @Override
    protected void performBoottime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {
        final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        final String name = address.getLastElement().getValue();
        final String defaultHost = ServerDefinition.DEFAULT_HOST.resolveModelAttribute(context, model).asString();
        final String servletContainer = ServerDefinition.SERVLET_CONTAINER.resolveModelAttribute(context, model).asString();

        final ServiceName serverName = UndertowService.SERVER.append(name);
        final Server service = new Server(name,defaultHost);
        final ServiceBuilder<Server> builder = context.getServiceTarget().addService(serverName, service)
                .addDependency(UndertowService.SERVLET_CONTAINER.append(servletContainer), ServletContainerService.class, service.getServletContainer())
                .addDependency(UndertowService.UNDERTOW, UndertowService.class, service.getUndertowService());

        builder.setInitialMode(ServiceController.Mode.ACTIVE);
        final ServiceController<Server> serviceController = builder.install();
        if (newControllers != null) {
            newControllers.add(serviceController);
        }
    }
}

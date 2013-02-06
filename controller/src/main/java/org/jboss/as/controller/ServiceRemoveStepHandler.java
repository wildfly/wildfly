package org.jboss.as.controller;

import java.util.ArrayList;

import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

/**
 * Abstract remove step handler that simply removes a service. If the operation is rolled
 * back it delegates the rollback to the corresponding add operations
 * {@link AbstractAddStepHandler#performRuntime(OperationContext, org.jboss.dmr.ModelNode, org.jboss.dmr.ModelNode, ServiceVerificationHandler, java.util.List)}
 * method
 *
 * @author Stuart Douglas
 */
public class ServiceRemoveStepHandler extends AbstractRemoveStepHandler {

    private final ServiceName baseServiceName;
    private final AbstractAddStepHandler addOperation;

    public ServiceRemoveStepHandler(final ServiceName baseServiceName, final AbstractAddStepHandler addOperation) {
        this.baseServiceName = baseServiceName;
        this.addOperation = addOperation;
    }

    protected ServiceRemoveStepHandler(final AbstractAddStepHandler addOperation) {
        this(null, addOperation);
    }

    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) {
        if (context.isResourceServiceRestartAllowed()) {
            final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
            final String name = address.getLastElement().getValue();
            context.removeService(serviceName(name));
        } else {
            context.reloadRequired();
        }
    }

    /**
     * The service name to be removed. Can be overridden for unusual service naming patterns
     * @param name The name of the resource being removed
     * @return The service name to remove
     */
    protected ServiceName serviceName(final String name) {
        return baseServiceName.append(name);
    }

    protected void recoverServices(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        if (context.isResourceServiceRestartAllowed()) {
            addOperation.performRuntime(context, operation, model, new ServiceVerificationHandler(), new ArrayList<ServiceController<?>>());
        } else {
            context.revertReloadRequired();
        }
    }
}

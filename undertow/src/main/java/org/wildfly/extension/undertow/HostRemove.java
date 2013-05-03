package org.wildfly.extension.undertow;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.ArrayList;

import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 */
class HostRemove extends AbstractRemoveStepHandler {

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) {
        final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        final PathAddress parent = address.subAddress(0, address.size() - 1);
        final String name = address.getLastElement().getValue();
        final String serverName = parent.getLastElement().getValue();
        final ServiceName virtualHostServiceName = UndertowService.virtualHostName(serverName, name);
        context.removeService(virtualHostServiceName);
    }

    protected void recoverServices(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        if (context.isResourceServiceRestartAllowed()) {
            HostAdd.INSTANCE.performRuntime(context, operation, model, new ServiceVerificationHandler(), new ArrayList<ServiceController<?>>());
        } else {
            context.revertReloadRequired();
        }
    }
}

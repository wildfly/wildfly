/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.mail.extension;

import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.dmr.ModelNode;

/**
 * Remove operation handler for the session resource.
 * @author Paul Ferraro
 */
class MailSessionRemove extends AbstractRemoveStepHandler {

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        if (context.isResourceServiceRestartAllowed()) {
            removeRuntimeServices(context, model);
        } else {
            context.reloadRequired();
        }
    }

    static void removeSessionProviderService(OperationContext context, ModelNode model) {
        removeSessionProviderService(context, context.getCurrentAddress(), model);
    }

    static void removeSessionProviderService(OperationContext context, PathAddress address, ModelNode model) {
        context.removeService(MailSessionDefinition.SESSION_CAPABILITY.getCapabilityServiceName(address).append("provider"));
    }

    static void removeBinderService(OperationContext context, ModelNode model) throws OperationFailedException {
        final String jndiName = MailSessionAdd.getJndiName(model, context);
        context.removeService(ContextNames.bindInfoFor(jndiName).getBinderServiceName());
    }

    static void removeRuntimeServices(OperationContext context, ModelNode model) throws OperationFailedException {
        removeSessionProviderService(context, model);

        context.removeService(MailSessionDefinition.SESSION_CAPABILITY.getCapabilityServiceName(context.getCurrentAddress()));

        removeBinderService(context, model);
    }

    @Override
    protected void recoverServices(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        if (context.isResourceServiceRestartAllowed()) {
            MailSessionAdd.installRuntimeServices(context, Resource.Tools.readModel(context.readResource(PathAddress.EMPTY_ADDRESS)));
        } else {
            context.revertReloadRequired();
        }
    }
}

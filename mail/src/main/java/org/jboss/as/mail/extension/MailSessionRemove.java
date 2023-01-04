/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2023, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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

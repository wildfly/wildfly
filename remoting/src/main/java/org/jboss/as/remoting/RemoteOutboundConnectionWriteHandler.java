/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.remoting;

import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;

/**
 * @author Jaikiran Pai
 */
class RemoteOutboundConnectionWriteHandler extends AbstractWriteAttributeHandler<Void> {

    static final RemoteOutboundConnectionWriteHandler INSTANCE = new RemoteOutboundConnectionWriteHandler();

    private RemoteOutboundConnectionWriteHandler() {
        super(RemoteOutboundConnnectionResourceDefinition.OUTBOUND_SOCKET_BINDING_REF);
    }

    @Override
    protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode resolvedValue, ModelNode currentValue, HandbackHolder<Void> voidHandbackHolder) throws OperationFailedException {
        final ModelNode model = context.readResource(PathAddress.EMPTY_ADDRESS).getModel();
        applyModelToRuntime(context, operation, attributeName, model);

        return false;

    }

    @Override
    protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode valueToRestore, ModelNode valueToRevert, Void handback) throws OperationFailedException {
        final ModelNode restored = context.readResource(PathAddress.EMPTY_ADDRESS).getModel().clone();
        restored.get(attributeName).set(valueToRestore);
        applyModelToRuntime(context, operation, attributeName, restored);
    }

    private void applyModelToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode model) throws OperationFailedException {

        final String connectionName = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR)).getLastElement().getValue();
        final ServiceName serviceName = RemoteOutboundConnectionService.OUTBOUND_CONNECTION_BASE_SERVICE_NAME.append(connectionName);
        final ServiceRegistry registry = context.getServiceRegistry(true);
        ServiceController sc = registry.getService(serviceName);
        if (sc != null) {
            RemoteOutboundConnectionService outboundConnectionService = RemoteOutboundConnectionService.class.cast(sc.getValue());
            // remove the service and re-install it with the new values
            if (outboundConnectionService != null) {
                context.removeService(serviceName);
            }
        }
        // install the service with new values
        RemoteOutboundConnectionAdd.INSTANCE.installRuntimeService(context, model, null);
    }
}

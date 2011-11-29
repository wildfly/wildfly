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

package org.jboss.as.server.operations;

import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.remoting.management.ManagementRemotingServices;
import org.jboss.as.server.mgmt.NativeManagementResourceDefinition;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;

/**
 * Removes the native management interface.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class NativeManagementRemoveHandler extends AbstractRemoveStepHandler {

    public static final NativeManagementRemoveHandler INSTANCE = new NativeManagementRemoveHandler();

    private NativeManagementRemoveHandler() {
    }

    @Override
    protected boolean requiresRuntime(OperationContext context) {
        return true;
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {

        final ServiceName endpointName = ManagementRemotingServices.MANAGEMENT_ENDPOINT;

        // Remove management Channel
        ManagementRemotingServices.removeManagementChannelServices(context, endpointName, ManagementRemotingServices.MANAGEMENT_CHANNEL);

        // Remove management Connector
        final ModelNode portNode = NativeManagementResourceDefinition.NATIVE_PORT.resolveModelAttribute(context, model);
        int port = portNode.isDefined() ? portNode.asInt() : 0;
        ManagementRemotingServices.removeConnectorServices(context, ManagementRemotingServices.MANAGEMENT_CONNECTOR);

        // Remove endpoint
        context.removeService(endpointName);

    }

    @Override
    protected void recoverServices(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        NativeManagementAddHandler.INSTANCE.performRuntime(context, operation, model, null, null);
    }
}

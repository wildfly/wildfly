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

package org.jboss.as.host.controller.operations;

import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.host.controller.resources.NativeManagementResourceDefinition;
import org.jboss.as.remoting.management.ManagementRemotingServices;
import org.jboss.dmr.ModelNode;

/**
 * {@code OperationStepHandler} for changing attributes on the native management interface.
 *
 * @author Emanuel Muckenhuber
 */
public class NativeManagementWriteAttributeHandler extends AbstractWriteAttributeHandler<Void> {

    private final LocalHostControllerInfoImpl hostControllerInfo;

    public NativeManagementWriteAttributeHandler(final LocalHostControllerInfoImpl hostControllerInfo) {
        super(NativeManagementResourceDefinition.ATTRIBUTE_DEFINITIONS);
        this.hostControllerInfo = hostControllerInfo;
    }

    @Override
    protected boolean applyUpdateToRuntime(final OperationContext context, final ModelNode operation,
                                           final String attributeName, final ModelNode resolvedValue,
                                           final ModelNode currentValue, HandbackHolder<Void> handbackHolder) throws OperationFailedException {

        final ModelNode subModel = context.readResource(PathAddress.EMPTY_ADDRESS).getModel();
        final ServiceVerificationHandler verificationHandler = new ServiceVerificationHandler();
        context.addStep(verificationHandler, OperationContext.Stage.VERIFY);
        installNativeManagementService(context, subModel, verificationHandler);
        return false;
    }

    @Override
    protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode valueToRestore, ModelNode valueToRevert, Void handback) throws OperationFailedException {
        final ModelNode subModel = context.readResource(PathAddress.EMPTY_ADDRESS).getModel().clone();
        subModel.get(attributeName).set(valueToRestore);
        installNativeManagementService(context, subModel, null);
    }

    private void installNativeManagementService(final OperationContext context, final ModelNode subModel, final ServiceVerificationHandler verificationHandler) throws OperationFailedException {

        // Remove the old connector
        ManagementRemotingServices.removeConnectorServices(context, ManagementRemotingServices.MANAGEMENT_CONNECTOR);

        NativeManagementAddHandler.populateHostControllerInfo(hostControllerInfo, context, subModel);
        NativeManagementAddHandler.installNativeManagementServices(context.getServiceTarget(), hostControllerInfo, verificationHandler, null, false);

    }


}

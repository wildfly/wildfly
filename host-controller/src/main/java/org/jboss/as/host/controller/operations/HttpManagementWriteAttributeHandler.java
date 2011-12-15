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
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.host.controller.HostControllerEnvironment;
import org.jboss.dmr.ModelNode;

/**
 * {@code OperationStepHandler} for changing attributes on the http management interface.
 *
 * @author Emanuel Muckenhuber
 */
public class HttpManagementWriteAttributeHandler extends AbstractWriteAttributeHandler<Void> {

    private final LocalHostControllerInfoImpl hostControllerInfo;
    private final HostControllerEnvironment environment;

    public HttpManagementWriteAttributeHandler(final LocalHostControllerInfoImpl hostControllerInfo, final HostControllerEnvironment environment) {
        this.hostControllerInfo = hostControllerInfo;
        this.environment = environment;
    }

    @Override
    protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode resolvedValue, ModelNode currentValue, HandbackHolder<Void> handbackHolder) throws OperationFailedException {
        final Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS);
        final ModelNode subModel = resource.getModel();
        final ServiceVerificationHandler handler = new ServiceVerificationHandler();
        updateHttpManagementService(context, subModel, hostControllerInfo, environment, handler);
        context.addStep(handler, OperationContext.Stage.VERIFY);

        return false;
    }

    @Override
    protected void revertUpdateToRuntime(final OperationContext context, final ModelNode operation,
                                         final String attributeName, final ModelNode valueToRestore,
                                         final ModelNode valueToRevert, final Void handback) throws OperationFailedException {
        final Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS);
        final ModelNode subModel = resource.getModel().clone();
        subModel.get(attributeName).set(valueToRestore);
        updateHttpManagementService(context, subModel, hostControllerInfo, environment, null);
    }



    static void updateHttpManagementService(final OperationContext context, final ModelNode subModel, final LocalHostControllerInfoImpl hostControllerInfo,
                                            final HostControllerEnvironment environment, final ServiceVerificationHandler verificationHandler) throws OperationFailedException {
        HttpManagementRemoveHandler.removeHttpManagementService(context);
        HttpManagementAddHandler.populateHostControllerInfo(hostControllerInfo, context, subModel);
        HttpManagementAddHandler.installHttpManagementServices(context.getRunningMode(), context.getServiceTarget(), hostControllerInfo, environment, verificationHandler);
    }

}

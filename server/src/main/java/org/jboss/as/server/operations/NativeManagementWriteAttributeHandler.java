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

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.server.mgmt.NativeManagementResourceDefinition;
import org.jboss.dmr.ModelNode;

/**
 * {@code OperationStepHandler} for changing attributes on the native management interface.
 *
 * @author Emanuel Muckenhuber
 */
public class NativeManagementWriteAttributeHandler extends ReloadRequiredWriteAttributeHandler {

    public static OperationStepHandler INSTANCE = new NativeManagementWriteAttributeHandler();

    private NativeManagementWriteAttributeHandler() {
        super(NativeManagementResourceDefinition.ATTRIBUTE_DEFINITIONS);
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        // Add a Stage.MODEL handler to validate that when this step and all other steps that may be part of
        // a containing composite operation are done, the model doesn't violate the requirement that "interface"
        // is an alternative to "socket-binding"
        context.addStep(new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                final ModelNode model = context.readResource(PathAddress.EMPTY_ADDRESS).getModel();
                if (model.hasDefined(NativeManagementResourceDefinition.INTERFACE.getName())
                        && (model.hasDefined(NativeManagementResourceDefinition.SOCKET_BINDING.getName()))) {
                    final ModelNode failure = new ModelNode().set(String.format("%s cannot be defined when %s is also defined",
                            NativeManagementResourceDefinition.INTERFACE.getName(),
                            NativeManagementResourceDefinition.SOCKET_BINDING.getName()));
                    throw new OperationFailedException(failure);
                }
                context.completeStep();
            }
        }, OperationContext.Stage.MODEL);

        super.execute(context, operation);
    }

}

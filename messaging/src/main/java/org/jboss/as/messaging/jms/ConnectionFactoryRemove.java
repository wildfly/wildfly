/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.messaging.jms;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import org.jboss.as.controller.Cancellable;
import org.jboss.as.controller.ModelRemoveOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.server.RuntimeOperationContext;
import org.jboss.as.server.RuntimeOperationHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;

/**
 * Update handler removing a connection factory from the JMS subsystem. The
 * runtime action will remove the corresponding {@link ConnectionFactoryService}.
 *
 * @author Emanuel Muckenhuber
 */
class ConnectionFactoryRemove implements ModelRemoveOperationHandler, RuntimeOperationHandler {

    static final ConnectionFactoryRemove INSTANCE = new ConnectionFactoryRemove();

    /** {@inheritDoc} */
    @Override
    public Cancellable execute(final OperationContext context, final ModelNode operation, ResultHandler resultHandler) {

        final ModelNode operationAddress = operation.require(OP_ADDR);
        final PathAddress address = PathAddress.pathAddress(operationAddress);
        final String name = address.getLastElement().getValue();

        final ModelNode subModel = context.getSubModel();
        final ModelNode compensatingOperation = ConnectionFactoryAdd.getAddOperation(operationAddress, subModel);

        if(context instanceof RuntimeOperationContext) {
            final RuntimeOperationContext runtimeContext = (RuntimeOperationContext) context;
            final ServiceController<?> service = runtimeContext.getServiceRegistry().getService(JMSServices.JMS_CF_BASE.append(name));
            if(service != null) {
                service.setMode(Mode.REMOVE);
            }
        }

        resultHandler.handleResultComplete(compensatingOperation);

        return Cancellable.NULL;
    }

}

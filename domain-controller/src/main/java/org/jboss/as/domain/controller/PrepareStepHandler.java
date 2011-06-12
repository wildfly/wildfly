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

package org.jboss.as.domain.controller;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER;

import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.NewStepHandler;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;

/**
 * Initial step handler for a {@link org.jboss.as.controller.NewModelController} that is the model controller for a host controller.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class PrepareStepHandler  implements NewStepHandler {

    private static final String EXECUTE_FOR_COORDINATOR = "execute-for-coordinator";

    private final LocalHostControllerInfo localHostControllerInfo;
    private final OperationCoordinatorStepHandler coordinatorHandler;
    private final OperationSlaveStepHandler slaveHandler;

    public PrepareStepHandler(final LocalHostControllerInfo localHostControllerInfo) {
        this.localHostControllerInfo = localHostControllerInfo;
        this.coordinatorHandler = new OperationCoordinatorStepHandler(localHostControllerInfo);
        this.slaveHandler = new OperationSlaveStepHandler(localHostControllerInfo);
    }

    @Override
    public void execute(NewOperationContext context, ModelNode operation) throws OperationFailedException {

        if (context.isBooting()) {
            executeDirect(context, operation);
        }
        else if (operation.hasDefined(OPERATION_HEADERS) && operation.get(OPERATION_HEADERS, EXECUTE_FOR_COORDINATOR).asBoolean(false)) {
            slaveHandler.execute(context, operation);
        } else if (isServerOperation(operation)) {
            // Pass direct requests for the server through whether they come from the master or not
            executeDirect(context, operation);
        } else {
            coordinatorHandler.execute(context, operation);
        }
    }

    private boolean isServerOperation(ModelNode operation) {
        PathAddress addr = PathAddress.pathAddress(operation.get(OP_ADDR));
        return addr.size() > 1
                && HOST.equals(addr.getElement(0).getKey())
                && localHostControllerInfo.getLocalHostName().equals(addr.getElement(0).getValue())
                && SERVER.equals(addr.getElement(1).getKey());
    }

    /**
     * Directly handles the op in the standard way the default prepare step handler would
     * @param context the operation execution context
     * @param operation the operation
     * @throws OperationFailedException
     */
    private void executeDirect(NewOperationContext context, ModelNode operation) throws OperationFailedException {
        final String operationName =  operation.require(OP).asString();
        final NewStepHandler stepHandler = context.getModelNodeRegistration().getOperationHandler(PathAddress.EMPTY_ADDRESS, operationName);
        if(stepHandler != null) {
            context.addStep(stepHandler, NewOperationContext.Stage.MODEL);
        } else {
            context.getFailureDescription().set(String.format("No handler for operation %s at address %s", operationName, PathAddress.pathAddress(operation.get(OP_ADDR))));
        }
        context.completeStep();
    }

}

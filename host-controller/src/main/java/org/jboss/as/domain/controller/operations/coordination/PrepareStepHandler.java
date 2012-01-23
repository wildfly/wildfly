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

package org.jboss.as.domain.controller.operations.coordination;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RUNNING_SERVER;
import static org.jboss.as.domain.controller.DomainControllerLogger.HOST_CONTROLLER_LOGGER;
import static org.jboss.as.domain.controller.DomainControllerMessages.MESSAGES;

import java.util.Map;
import java.util.concurrent.ExecutorService;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ProxyController;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.domain.controller.LocalHostControllerInfo;
import org.jboss.as.repository.ContentRepository;
import org.jboss.dmr.ModelNode;

/**
 * Initial step handler for a {@link org.jboss.as.controller.ModelController} that is the model controller for a host controller.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class PrepareStepHandler  implements OperationStepHandler {

    public static final String EXECUTE_FOR_COORDINATOR = "execute-for-coordinator";

    private final LocalHostControllerInfo localHostControllerInfo;
    private final OperationCoordinatorStepHandler coordinatorHandler;
    private final OperationSlaveStepHandler slaveHandler;

    public PrepareStepHandler(final LocalHostControllerInfo localHostControllerInfo,
                              ContentRepository contentRepository, final Map<String, ProxyController> hostProxies,
                              final Map<String, ProxyController> serverProxies) {
        this.localHostControllerInfo = localHostControllerInfo;
        this.slaveHandler = new OperationSlaveStepHandler(localHostControllerInfo, serverProxies);
        this.coordinatorHandler = new OperationCoordinatorStepHandler(localHostControllerInfo, contentRepository, hostProxies, serverProxies, slaveHandler);
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

        if (context.isBooting()) {
            executeDirect(context, operation);
        }
        else if (operation.hasDefined(OPERATION_HEADERS)
                && operation.get(OPERATION_HEADERS).hasDefined(EXECUTE_FOR_COORDINATOR)
                && operation.get(OPERATION_HEADERS).get(EXECUTE_FOR_COORDINATOR).asBoolean()) {
            // Coordinator wants us to execute locally and send result including the steps needed for execution on the servers
            slaveHandler.execute(context, operation);
        } else if (isServerOperation(operation)) {
            // Pass direct requests for the server through whether they come from the master or not
            executeDirect(context, operation);
        } else {
            coordinatorHandler.execute(context, operation);
        }
    }

    public void setExecutorService(final ExecutorService executorService) {
        coordinatorHandler.setExecutorService(executorService);
    }

    private boolean isServerOperation(ModelNode operation) {
        PathAddress addr = PathAddress.pathAddress(operation.get(OP_ADDR));
        return addr.size() > 1
                && HOST.equals(addr.getElement(0).getKey())
                && localHostControllerInfo.getLocalHostName().equals(addr.getElement(0).getValue())
                && RUNNING_SERVER.equals(addr.getElement(1).getKey());
    }

    /**
     * Directly handles the op in the standard way the default prepare step handler would
     * @param context the operation execution context
     * @param operation the operation
     * @throws OperationFailedException
     */
    private void executeDirect(OperationContext context, ModelNode operation) throws OperationFailedException {
        if (HOST_CONTROLLER_LOGGER.isTraceEnabled()) {
            HOST_CONTROLLER_LOGGER.tracef("%s executing direct", getClass().getSimpleName());
        }
        final String operationName =  operation.require(OP).asString();
        OperationStepHandler stepHandler = null;
        final ImmutableManagementResourceRegistration registration = context.getResourceRegistration();
        if (registration != null) {
            stepHandler = registration.getOperationHandler(PathAddress.EMPTY_ADDRESS, operationName);
        }
        if(stepHandler != null) {
            context.addStep(stepHandler, OperationContext.Stage.MODEL);
        } else {
            context.getFailureDescription().set(MESSAGES.noHandlerForOperation(operationName, PathAddress.pathAddress(operation.get(OP_ADDR))));
        }
        context.completeStep();
    }

}

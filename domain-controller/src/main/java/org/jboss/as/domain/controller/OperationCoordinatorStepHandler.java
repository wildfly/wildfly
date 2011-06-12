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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jboss.as.controller.BasicOperationResult;
import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.NewProxyController;
import org.jboss.as.controller.NewStepHandler;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.ModelNodeRegistration;
import org.jboss.as.domain.controller.operations.coordination.OperationRouting;
import org.jboss.dmr.ModelNode;

/**
 * Coordinates the overall execution of an operation on behalf of the domain.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class OperationCoordinatorStepHandler implements NewStepHandler {

    private final LocalHostControllerInfo localHostControllerInfo;
    private final Map<String, NewProxyController> hostProxies;

    OperationCoordinatorStepHandler(final LocalHostControllerInfo localHostControllerInfo, final Map<String, NewProxyController> hostProxies) {
        this.localHostControllerInfo = localHostControllerInfo;
        this.hostProxies = hostProxies;
    }

    @Override
    public void execute(NewOperationContext context, ModelNode operation) throws OperationFailedException {

        // Determine routing
        ModelNodeRegistration opRegistry = context.getModelNodeRegistration();
        OperationRouting routing = OperationRouting.determineRouting(operation, localHostControllerInfo, opRegistry);

        if (!localHostControllerInfo.isMasterDomainController()
                && (routing.isRouteToMaster() || !routing.isLocalOnly(localHostControllerInfo.getLocalHostName()))) {
            // We cannot handle this ourselves
            routetoMasterDomainController(context, operation);
        }
        else if (routing.getSingleHost() != null && !localHostControllerInfo.getLocalHostName().equals(routing.getSingleHost())) {
            // Possibly a two step operation, but not coordinated by this host. Execute direct and let the remote HC
            // coordinate any two step process (if there is one)
            executeDirect(context, operation);
        }
        else if (!routing.isTwoStep()) {
            // It's a domain level op (probably a read) that does not require bringing in other hosts or servers
            executeDirect(context, operation);
        }
        else {
            // Else we are responsible for coordinating a two-phase op
            // -- apply to DomainController models across domain and then push to servers
            executeTwoPhaseOperation(context, operation, routing);
        }


    }

    private void routetoMasterDomainController(NewOperationContext context, ModelNode operation) {
        // System.out.println("------ route to master ");
        // Per discussion on 2011/03/07, routing requests from a slave to the
        // master may overly complicate the security infrastructure. Therefore,
        // the ability to do this is being disabled until it's clear that it's
        // not a problem
        context.getFailureDescription().set(String.format("Operation %s for address %s can only handled by the " +
                "master Domain Controller; this host is not the master Domain Controller",
                operation.get(OP).asString(), PathAddress.pathAddress(operation.get(OP_ADDR))));
        context.completeStep();
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

    private void executeTwoPhaseOperation(NewOperationContext context, ModelNode operation, OperationRouting routing) {

        if (1 == 1) {
            throw new UnsupportedOperationException();
        }

//        1) Create overall tx control
//        2) if local host affected, add DPSH step for local host
//        3) if a single other host is affected, add DPSH step for that host
//        4) if multiple hosts are affected, add a concurrent DPSH step for that host
//        5) Add a rollout plan assembly step
//        6) Add DPSH for each server (actually, for each in-series step)
//        7) Add a result creation step -- this one actually writes the response, triggers failed or succeeded
        context.completeStep();
    }
}

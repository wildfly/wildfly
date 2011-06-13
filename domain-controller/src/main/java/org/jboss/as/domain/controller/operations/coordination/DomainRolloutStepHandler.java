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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.jboss.as.controller.NewModelController;
import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.NewStepHandler;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.domain.controller.ServerIdentity;
import org.jboss.dmr.ModelNode;

/**
 * Formulates a rollout plan, adds steps to execute it and to formulate the overall result.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class DomainRolloutStepHandler implements NewStepHandler {

    private final DomainOperationContext domainOperationContext;
    private final ExecutorService executorService;

    public DomainRolloutStepHandler(final DomainOperationContext domainOperationContext, final ExecutorService executorService) {
        this.domainOperationContext = domainOperationContext;
        this.executorService = executorService;
    }

    @Override
    public void execute(NewOperationContext context, ModelNode operation) throws OperationFailedException {

        final Map<ServerIdentity, ProxyTask> tasks = new HashMap<ServerIdentity, ProxyTask>();

        // 1) Confirm no host failures
        boolean pushToServers = true;
        ModelNode ourResult = domainOperationContext.getCoordinatorResult();
        if (ourResult.has(FAILURE_DESCRIPTION)) {
            pushToServers = false;
        } else {
            for (ModelNode hostResult : domainOperationContext.getHostControllerResults().values()) {
                if (!operation.hasDefined(OUTCOME) || !SUCCESS.equals(operation.get(OUTCOME))) {
                    pushToServers = false;
                    break;
                }
            }
        }

        boolean interrupted = false;
        try {
            if (pushToServers) {
                // 2) Formulate rollout plan

                // 3) Add a step for each server (actually, for each in-series step)

                final Map<ServerIdentity, Future<ModelNode>> futures = new HashMap<ServerIdentity, Future<ModelNode>>();
                //TODO implement
                if (1 == 1) {
                    throw new UnsupportedOperationException();
                }
            }
            context.completeStep();
        } finally {

            try {
                // Inform the remote hosts whether to commit or roll back their updates
                // Do this in parallel
                // TODO consider blocking until all return?
                boolean completeRollback = domainOperationContext.isCompleteRollback();
                for (Map.Entry<ServerIdentity, ProxyTask> entry : tasks.entrySet()) {
                    NewModelController.OperationTransaction tx = entry.getValue().getRemoteTransaction();
                    if (tx != null) {
                        boolean rollback = completeRollback || domainOperationContext.isServerGroupRollback(entry.getKey().getServerGroupName());
                        executorService.submit(new ProxyCommitRollbackTask(tx, rollback));
                    }
                }
            } finally {
                if (interrupted) {
                    Thread.currentThread().interrupt();
                }
            }
        }

    }
}

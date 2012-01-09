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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CANCELLED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.domain.controller.DomainControllerLogger.CONTROLLER_LOGGER;
import static org.jboss.as.domain.controller.DomainControllerLogger.HOST_CONTROLLER_LOGGER;
import static org.jboss.as.domain.controller.DomainControllerMessages.MESSAGES;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.ProxyController;
import org.jboss.dmr.ModelNode;

/**
 * Executes the first phase of a two phase operation on one or more remote, slave host controllers.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class DomainSlaveHandler implements OperationStepHandler {

    private final ExecutorService executorService;
    private final DomainOperationContext domainOperationContext;
    private final Map<String, ProxyController> hostProxies;

    public DomainSlaveHandler(final Map<String, ProxyController> hostProxies,
                              final DomainOperationContext domainOperationContext,
                              final ExecutorService executorService) {
        this.hostProxies = hostProxies;
        this.domainOperationContext = domainOperationContext;
        this.executorService = executorService;
    }

    @Override
    public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {

        if (context.hasFailureDescription()) {
            // abort
            context.setRollbackOnly();
            context.completeStep();
            return;
        }

        final Map<String, ProxyTask> tasks = new HashMap<String, ProxyTask>();
        final Map<String, Future<ModelNode>> futures = new HashMap<String, Future<ModelNode>>();

        for (Map.Entry<String, ProxyController> entry : hostProxies.entrySet()) {
            String host = entry.getKey();
            ProxyTask task = new ProxyTask(host, operation.clone(), context, entry.getValue());
            tasks.put(host, task);
            futures.put(host, executorService.submit(task));
        }

        boolean interrupted = false;
        try {
            for (Map.Entry<String, ProxyTask> entry : tasks.entrySet()) {
                ProxyTask task = entry.getValue();
                ModelNode result = null;
                try {
                    result = entry.getValue().getUncommittedResult();
                } catch (Exception e) {
                    result = new ModelNode();
                    result.get(OUTCOME).set(FAILED);
                    if (e instanceof InterruptedException) {
                        result.get(FAILURE_DESCRIPTION).set(MESSAGES.interruptedAwaitingResultFromHost(entry.getKey()));
                        interrupted = true;
                    } else {
                        result.get(FAILURE_DESCRIPTION).set(MESSAGES.exceptionAwaitingResultFromHost(entry.getKey(), e.getMessage()));
                    }
                    task.cancel();
                    futures.get(entry.getKey()).cancel(true);
                }

                if (HOST_CONTROLLER_LOGGER.isTraceEnabled()) {
                    HOST_CONTROLLER_LOGGER.tracef("ParsedResult for remote host %s is %s", entry.getKey(), result);
                }
                domainOperationContext.addHostControllerResult(entry.getKey(), result);
            }

            context.completeStep();

        } finally {
            try {
                // Inform the remote hosts whether to commit or roll back their updates
                // Do this in parallel
                boolean rollback = domainOperationContext.isCompleteRollback();
                for (ProxyTask task : tasks.values()) {
                    task.finalizeTransaction(!rollback);
                }
                final short timeout = 10;
                for (Map.Entry<String, Future<ModelNode>> entry : futures.entrySet()) {
                    Future<ModelNode> future = entry.getValue();
                    try {
                        ModelNode finalResult = future.isCancelled() ? getCancelledResult() : future.get();
                        domainOperationContext.addHostControllerResult(entry.getKey(), finalResult);
                    } catch (InterruptedException e) {
                        interrupted = true;
                        CONTROLLER_LOGGER.interruptedAwaitingFinalResponse(entry.getKey());

                    } catch (ExecutionException e) {
                        CONTROLLER_LOGGER.caughtExceptionAwaitingFinalResponse(e.getCause(), entry.getKey());
                    }
//                    catch (TimeoutException e) {
//                        log.warnf("Host %s did not respond to %s within [%d] seconds", entry.getKey(), (rollback ? "rollback" : "commit"), timeout);
//                    }

                }
            } finally {
                if (interrupted) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private ModelNode getCancelledResult() {
        ModelNode cancelled = new ModelNode();
        cancelled.get(OUTCOME).set(CANCELLED);
        return cancelled;
    }

}

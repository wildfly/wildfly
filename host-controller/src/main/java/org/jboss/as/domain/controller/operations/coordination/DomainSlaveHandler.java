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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import org.jboss.as.controller.remote.RemoteProxyController;
import org.jboss.as.controller.remote.TransactionalProtocolClient;
import static org.jboss.as.domain.controller.DomainControllerLogger.CONTROLLER_LOGGER;
import static org.jboss.as.domain.controller.DomainControllerLogger.HOST_CONTROLLER_LOGGER;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.ProxyController;
import static org.jboss.as.domain.controller.DomainControllerMessages.MESSAGES;
import org.jboss.dmr.ModelNode;
import org.jboss.threads.AsyncFuture;

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

        final Set<String> outstanding = new HashSet<String>(hostProxies.keySet());
        final List<TransactionalProtocolClient.PreparedOperation<HostControllerUpdateTask.ProxyOperation>> results = new ArrayList<TransactionalProtocolClient.PreparedOperation<HostControllerUpdateTask.ProxyOperation>>();
        final Map<String, AsyncFuture<ModelNode>> finalResults = new HashMap<String, AsyncFuture<ModelNode>>();
        final HostControllerUpdateTask.ProxyOperationListener listener = new HostControllerUpdateTask.ProxyOperationListener();
        for (Map.Entry<String, ProxyController> entry : hostProxies.entrySet()) {
            // Create the proxy task
            final String host = entry.getKey();
            final TransactionalProtocolClient client = ((RemoteProxyController)entry.getValue()).getTransactionalProtocolClient();
            final HostControllerUpdateTask task = new HostControllerUpdateTask(host, operation.clone(), context, client);
            // Execute the operation on the remote host
            final AsyncFuture<ModelNode> finalResult = task.execute(listener);
            finalResults.put(host, finalResult);
        }

        // Wait for all hosts to reach the prepared state
        boolean interrupted = false;
        try {
            try {
                while(outstanding.size() > 0) {
                    final TransactionalProtocolClient.PreparedOperation<HostControllerUpdateTask.ProxyOperation> prepared = listener.retrievePreparedOperation();
                    final String hostName = prepared.getOperation().getName();
                    if(! outstanding.remove(hostName)) {
                        continue;
                    }
                    final ModelNode preparedResult = prepared.getPreparedResult();
                    if (HOST_CONTROLLER_LOGGER.isTraceEnabled()) {
                        HOST_CONTROLLER_LOGGER.tracef("Preliminary result for remote host %s is %s", hostName, preparedResult);
                    }
                    domainOperationContext.addHostControllerResult(hostName, preparedResult);
                    results.add(prepared);
                }
            } catch (InterruptedException ie) {
                interrupted = true;
                // Set rollback only
                domainOperationContext.setFailureReported(true);
                // Rollback all HCs
                for(final AsyncFuture<ModelNode> finalResult : finalResults.values()) {
                    finalResult.asyncCancel(false);
                }
                // Wait that all hosts are rolled back!?
                for(final Map.Entry<String, AsyncFuture<ModelNode>> entry : finalResults.entrySet()) {
                    final String hostName = entry.getKey();
                    try {
                        final ModelNode result = entry.getValue().get();
                        domainOperationContext.addHostControllerResult(hostName, result);
                    } catch (Exception e) {
                        final ModelNode result = new ModelNode();
                        result.get(OUTCOME).set(FAILED);
                        if (e instanceof InterruptedException) {
                            result.get(FAILURE_DESCRIPTION).set(MESSAGES.interruptedAwaitingResultFromHost(entry.getKey()));
                            interrupted = true;
                        } else {
                            result.get(FAILURE_DESCRIPTION).set(MESSAGES.exceptionAwaitingResultFromHost(entry.getKey(), e.getMessage()));
                        }
                        domainOperationContext.addHostControllerResult(hostName, result);
                    }
                }
            }

            context.completeStep();

        } finally {
            try {
                // Inform the remote hosts whether to commit or roll back their updates
                // Do this in parallel
                boolean rollback = domainOperationContext.isCompleteRollback();
                for(final TransactionalProtocolClient.PreparedOperation<HostControllerUpdateTask.ProxyOperation> prepared : results) {
                    if(prepared.isDone()) {
                        continue;
                    }
                    if(! rollback) {
                        prepared.commit();
                    } else {
                        prepared.rollback();
                    }
                }
                // Now get the final results from the hosts
                for(final TransactionalProtocolClient.PreparedOperation<HostControllerUpdateTask.ProxyOperation> prepared : results) {
                    final String hostName = prepared.getOperation().getName();
                    try {
                        final ModelNode finalResult = prepared.getFinalResult().get();
                        domainOperationContext.addHostControllerResult(hostName, finalResult);

                        if (HOST_CONTROLLER_LOGGER.isTraceEnabled()) {
                            HOST_CONTROLLER_LOGGER.tracef("Final result for remote host %s is %s", hostName, finalResult);
                        }
                    } catch (InterruptedException e) {
                        interrupted = true;
                        CONTROLLER_LOGGER.interruptedAwaitingFinalResponse(hostName);
                    } catch (ExecutionException e) {
                        CONTROLLER_LOGGER.caughtExceptionAwaitingFinalResponse(e.getCause(), hostName);
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

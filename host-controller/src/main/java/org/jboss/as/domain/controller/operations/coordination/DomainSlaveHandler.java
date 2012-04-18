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

import org.jboss.as.controller.remote.RemoteProxyController;
import org.jboss.as.controller.remote.TransactionalProtocolClient;
import static org.jboss.as.domain.controller.DomainControllerLogger.CONTROLLER_LOGGER;
import static org.jboss.as.domain.controller.DomainControllerLogger.HOST_CONTROLLER_LOGGER;
import static org.jboss.as.domain.controller.DomainControllerLogger.ROOT_LOGGER;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

        final Set<String> outstanding = new HashSet<String>(hostProxies.keySet());
        final List<TransactionalProtocolClient.PreparedOperation<NewProxyTask.ProxyOperation>> results = new ArrayList<TransactionalProtocolClient.PreparedOperation<NewProxyTask.ProxyOperation>>();
        final Map<String, Future<ModelNode>> finalResults = new HashMap<String, Future<ModelNode>>();
        final NewProxyTask.ProxyOperationListener listener = new NewProxyTask.ProxyOperationListener();
        for (Map.Entry<String, ProxyController> entry : hostProxies.entrySet()) {
            // Create the proxy task
            final String host = entry.getKey();
            final TransactionalProtocolClient client = ((RemoteProxyController)entry.getValue()).getTransactionalProtocolClient();
            final NewProxyTask task = new NewProxyTask(host, operation.clone(), context, client);
            // Execute the operation on the remote host
            final Future<ModelNode> finalResult = task.execute(listener);
            finalResults.put(host, finalResult);
        }

        // Wait for all hosts to reach the prepared state
        boolean interrupted = false;
        try {
            while(outstanding.size() > 0) {
                try {
                    final TransactionalProtocolClient.PreparedOperation<NewProxyTask.ProxyOperation> prepared = listener.retrievePreparedOperation();
                    final String name = prepared.getOperation().getName();
                    if( ! outstanding.remove(name)) {
                        ROOT_LOGGER.errorf("did not expect response from host %s", name);
                        continue;
                    }
                    final ModelNode preparedResult = prepared.getPreparedResult();
                    if (HOST_CONTROLLER_LOGGER.isTraceEnabled()) {
                        HOST_CONTROLLER_LOGGER.tracef("Preliminary result for remote host %s is %s", name, preparedResult);
                    }
                    domainOperationContext.addHostControllerResult(name, preparedResult);
                    results.add(prepared);
                } catch (InterruptedException e) {
                    interrupted = true;
                    // TODO fix interruption
                }
            }

            context.completeStep();

        } finally {
            try {
                // Inform the remote hosts whether to commit or roll back their updates
                // Do this in parallel
                boolean rollback = domainOperationContext.isCompleteRollback();
                for(final TransactionalProtocolClient.PreparedOperation<NewProxyTask.ProxyOperation> prepared : results) {
                    if(! rollback) {
                        prepared.commit();
                    } else {
                        prepared.rollback();
                    }
                }
                // Now get the final results from the hosts
                for(final TransactionalProtocolClient.PreparedOperation<NewProxyTask.ProxyOperation> prepared : results) {
                    final String name = prepared.getOperation().getName();
                    try {
                        final ModelNode finalResult = prepared.getFinalResult().get();
                        domainOperationContext.addHostControllerResult(name, finalResult);

                        if (HOST_CONTROLLER_LOGGER.isTraceEnabled()) {
                            HOST_CONTROLLER_LOGGER.tracef("Final result for remote host %s is %s", name, finalResult);
                        }
                    } catch (InterruptedException e) {
                        interrupted = true;
                        CONTROLLER_LOGGER.interruptedAwaitingFinalResponse(name);
                    } catch (ExecutionException e) {
                        CONTROLLER_LOGGER.caughtExceptionAwaitingFinalResponse(e.getCause(), name);
                    }
                }
            } finally {
                if (interrupted) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
//
//    private ModelNode getCancelledResult() {
//        ModelNode cancelled = new ModelNode();
//        cancelled.get(OUTCOME).set(CANCELLED);
//        return cancelled;
//    }

}

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

import org.jboss.as.controller.TransformingProxyController;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;

import static org.jboss.as.domain.controller.DomainControllerLogger.CONTROLLER_LOGGER;
import static org.jboss.as.domain.controller.DomainControllerLogger.HOST_CONTROLLER_LOGGER;
import static org.jboss.as.domain.controller.DomainControllerMessages.MESSAGES;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.ProxyController;
import org.jboss.as.controller.operations.DomainOperationTransformer;
import org.jboss.as.controller.operations.OperationAttachments;
import org.jboss.as.controller.remote.TransactionalProtocolClient;
import org.jboss.dmr.ModelNode;

/**
 * Executes the first phase of a two phase operation on one or more remote, slave host controllers.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class DomainSlaveHandler implements OperationStepHandler {

    private final DomainOperationContext domainOperationContext;
    private final Map<String, ProxyController> hostProxies;

    public DomainSlaveHandler(final Map<String, ProxyController> hostProxies,
                              final DomainOperationContext domainOperationContext) {
        this.hostProxies = hostProxies;
        this.domainOperationContext = domainOperationContext;
    }

    @Override
    public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {

        if (context.hasFailureDescription()) {
            // abort
            context.setRollbackOnly();
            context.stepCompleted();
            return;
        }

        final Set<String> outstanding = new HashSet<String>(hostProxies.keySet());
        final List<TransactionalProtocolClient.PreparedOperation<HostControllerUpdateTask.ProxyOperation>> results = new ArrayList<TransactionalProtocolClient.PreparedOperation<HostControllerUpdateTask.ProxyOperation>>();
        final Map<String, HostControllerUpdateTask.ExecutedHostRequest> finalResults = new HashMap<String, HostControllerUpdateTask.ExecutedHostRequest>();
        final HostControllerUpdateTask.ProxyOperationListener listener = new HostControllerUpdateTask.ProxyOperationListener();
        for (Map.Entry<String, ProxyController> entry : hostProxies.entrySet()) {
            // Create the proxy task
            final String host = entry.getKey();
            final TransformingProxyController proxyController = (TransformingProxyController) entry.getValue();
            List<DomainOperationTransformer> transformers = context.getAttachment(OperationAttachments.SLAVE_SERVER_OPERATION_TRANSFORMERS);
            ModelNode op = operation;
            if(transformers != null) {
                for(final DomainOperationTransformer transformer : transformers) {
                    op = transformer.transform(context, op);
                }
            }
            final HostControllerUpdateTask task = new HostControllerUpdateTask(host, op.clone(), context, proxyController);
            // Execute the operation on the remote host
            final HostControllerUpdateTask.ExecutedHostRequest finalResult = task.execute(listener);
            domainOperationContext.recordHostRequest(host, finalResult);
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
                    // See if we have to reject the result
                    final HostControllerUpdateTask.ExecutedHostRequest request = finalResults.get(hostName);
                    boolean reject = prepared.isFailed() ? false : request.rejectOperation(preparedResult);
                    if(reject) {
                        if (HOST_CONTROLLER_LOGGER.isDebugEnabled()) {
                            HOST_CONTROLLER_LOGGER.debugf("Rejecting result for remote host %s is %s", hostName, preparedResult);
                        }
                        final ModelNode failedResult = new ModelNode();
                        failedResult.get(OUTCOME).set(FAILED);
                        failedResult.get(FAILURE_DESCRIPTION).set(request.getFailureDescription());

                        // Record the failed result
                        domainOperationContext.addHostControllerResult(hostName, failedResult);
                        results.add(prepared);

                    } else {
                        // Record the prepared result
                        domainOperationContext.addHostControllerResult(hostName, preparedResult);
                        results.add(prepared);
                    }
                }
            } catch (InterruptedException ie) {
                interrupted = true;
                // Set rollback only
                domainOperationContext.setFailureReported(true);
                // Rollback all HCs
                for(final HostControllerUpdateTask.ExecutedHostRequest finalResult : finalResults.values()) {
                    finalResult.asyncCancel();
                }
                // Wait that all hosts are rolled back!?
                for(final Map.Entry<String, HostControllerUpdateTask.ExecutedHostRequest> entry : finalResults.entrySet()) {
                    final String hostName = entry.getKey();
                    try {
                        final HostControllerUpdateTask.ExecutedHostRequest request = entry.getValue();
                        final ModelNode result = request.getFinalResult().get();
                        final ModelNode transformedResult = request.transformResult(result);
                        domainOperationContext.addHostControllerResult(hostName, transformedResult);
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
                        final HostControllerUpdateTask.ExecutedHostRequest request = finalResults.get(hostName);
                        final ModelNode finalResult = prepared.getFinalResult().get();
                        final ModelNode transformedResult = request.transformResult(finalResult);
                        domainOperationContext.addHostControllerResult(hostName, transformedResult);

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

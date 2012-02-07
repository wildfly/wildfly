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
import static org.jboss.as.domain.controller.DomainControllerLogger.HOST_CONTROLLER_LOGGER;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.ProxyController;
import org.jboss.as.controller.client.MessageSeverity;
import org.jboss.as.controller.client.OperationAttachments;
import org.jboss.as.controller.client.OperationMessageHandler;
import org.jboss.dmr.ModelNode;

/**
* Callable that invokes an operation on a proxy.
*
* @author Brian Stansberry (c) 2011 Red Hat Inc.
*/
class ProxyTask implements Callable<ModelNode> {

    private final ProxyController proxyController;
    private final String host;
    private final ModelNode operation;
    private final OperationContext context;

    private volatile Exception exception;
    private final AtomicReference<Boolean> transactionAction = new AtomicReference<Boolean>();
    private final AtomicReference<ModelNode> uncommittedResultRef = new AtomicReference<ModelNode>();
    private boolean cancelRemoteTransaction;

    public ProxyTask(String host, ModelNode operation, OperationContext context, ProxyController proxyController) {
        this.host = host;
        this.operation = operation;
        this.context = context;
        this.proxyController = proxyController;
    }

    @Override
    public ModelNode call() throws Exception {
        try {
            boolean trace = HOST_CONTROLLER_LOGGER.isTraceEnabled();
            if (trace) {
                HOST_CONTROLLER_LOGGER.tracef("Sending %s to %s", operation, host);
            }
            OperationMessageHandler messageHandler = new DelegatingMessageHandler(context);

            final AtomicReference<ModelController.OperationTransaction> txRef = new AtomicReference<ModelController.OperationTransaction>();
            final AtomicReference<ModelNode> preparedResultRef = new AtomicReference<ModelNode>();
            final AtomicReference<ModelNode> finalResultRef = new AtomicReference<ModelNode>();
            final ProxyController.ProxyOperationControl proxyControl = new ProxyController.ProxyOperationControl() {

                @Override
                public void operationPrepared(ModelController.OperationTransaction transaction, ModelNode result) {
                    txRef.set(transaction);
                    preparedResultRef.set(result);
                }

                @Override
                public void operationFailed(ModelNode response) {
                    finalResultRef.set(response);
                }

                @Override
                public void operationCompleted(ModelNode response) {
                    finalResultRef.set(response);
                }
            };

            // When this returns, one of the methods on proxyControl will have been called
            proxyController.execute(operation, messageHandler, proxyControl, new DelegatingOperationAttachments(context));

            ModelController.OperationTransaction remoteTransaction = null;
            ModelNode result = finalResultRef.get();
            if (result != null) {
                // operation failed before it could commit
                if (trace) {
                    HOST_CONTROLLER_LOGGER.tracef("Received final result %s from %s", result, host);
                }
            } else {
                result = preparedResultRef.get();
                if (trace) {
                    HOST_CONTROLLER_LOGGER.tracef("Received prepared result %s from %s", result, host);
                }
                remoteTransaction = txRef.get();
            }

            // Publish the initial result to the operation step handler who triggered this task
            synchronized (uncommittedResultRef) {
                uncommittedResultRef.set(result);
                uncommittedResultRef.notifyAll();
            }

            if (remoteTransaction != null) {
                if (cancelRemoteTransaction) {
                    // Controlling thread was cancelled
                    remoteTransaction.rollback();
                } else {
                    synchronized (transactionAction) {
                        while (transactionAction.get() == null) {
                            try {
                                transactionAction.wait();
                            }
                            catch (InterruptedException ie) {
                                // Treat as cancellation
                                transactionAction.set(Boolean.FALSE);
                            }
                        }
                        if (transactionAction.get().booleanValue()) {
                            remoteTransaction.commit();
                        } else {
                            remoteTransaction.rollback();
                        }
                    }
                }

                // All paths above result in commit() or rollback() being called. Those
                // methods will not return until proxyControl is invoked setting the final result
            }

            return finalResultRef.get();
        } catch (Exception e) {
            ModelNode node = new ModelNode();
            node.get(OUTCOME).set(FAILED);
            node.get(FAILURE_DESCRIPTION).set(e.getMessage());
            exception = e;
            uncommittedResultRef.set(node);
            synchronized (uncommittedResultRef) {
                uncommittedResultRef.notifyAll();
            }
            throw e;
        }
    }

    /**
     * Gets either the prepared (pre-commit/rollback) result from the remote node,
     * or the failure result if the remote node failed before reaching Stage.DONE.
     *
     * @return the remote node's result. Will not return {@code null}.
     *
     * @throws InterruptedException if interrupted while waiting for the result
     * @throws Exception if something went wrong executing the request
     */
    ModelNode getUncommittedResult() throws Exception {
        synchronized (uncommittedResultRef) {

            while (uncommittedResultRef.get() == null) {
                try {
                    uncommittedResultRef.wait();
                }
                catch (InterruptedException ie) {
                    cancelRemoteTransaction = true;
                    throw ie;
                }
            }
            if (exception != null) {
                throw exception;
            }
            return uncommittedResultRef.get();
        }
    }

    void finalizeTransaction(boolean commit) {
        synchronized (transactionAction) {
            transactionAction.set(Boolean.valueOf(commit));
            transactionAction.notifyAll();
        }
    }

    void cancel() {
        synchronized (uncommittedResultRef) {
            cancelRemoteTransaction = true;
        }
    }

    private static class DelegatingMessageHandler implements OperationMessageHandler {

        private final OperationContext context;

        DelegatingMessageHandler(final OperationContext context) {
            this.context = context;
        }

        @Override
        public void handleReport(MessageSeverity severity, String message) {
            context.report(severity, message);
        }
    }

    private static class DelegatingOperationAttachments implements OperationAttachments {

        private final OperationContext context;
        private DelegatingOperationAttachments(final OperationContext context) {
            this.context = context;
        }

        @Override
        public boolean isAutoCloseStreams() {
            return false;
        }

        @Override
        public List<InputStream> getInputStreams() {
            int count = context.getAttachmentStreamCount();
            List<InputStream> result = new ArrayList<InputStream>(count);
            for (int i = 0; i < count; i++) {
                result.add(context.getAttachmentStream(i));
            }
            return result;
        }

        @Override
        public void close() throws IOException {
            //
        }
    }
}

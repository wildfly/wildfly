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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.as.controller.NewModelController;
import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.NewProxyController;
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

    private final NewProxyController proxyController;
    private final String host;
    private final ModelNode operation;
    private final NewOperationContext context;

    private final AtomicReference<Boolean> transactionAction = new AtomicReference<Boolean>();
    private final AtomicReference<ModelNode> uncommittedResultRef = new AtomicReference<ModelNode>();
    private boolean cancelRemoteTransaction;

    public ProxyTask(String host, ModelNode operation, NewOperationContext context, NewProxyController proxyController) {
        this.host = host;
        this.operation = operation;
        this.context = context;
        this.proxyController = proxyController;
    }

    @Override
    public ModelNode call() throws Exception {

        System.out.println("Sending " + operation + " to " + host);
        OperationMessageHandler messageHandler = new DelegatingMessageHandler(context);

        final AtomicReference<NewModelController.OperationTransaction> txRef = new AtomicReference<NewModelController.OperationTransaction>();
        final AtomicReference<ModelNode> preparedResultRef = new AtomicReference<ModelNode>();
        final AtomicReference<ModelNode> finalResultRef = new AtomicReference<ModelNode>();
        final NewProxyController.ProxyOperationControl proxyControl = new NewProxyController.ProxyOperationControl() {

            @Override
            public void operationPrepared(NewModelController.OperationTransaction transaction, ModelNode result) {
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

        proxyController.execute(operation, messageHandler, proxyControl, new DelegatingOperationAttachments(context));

        NewModelController.OperationTransaction remoteTransaction = null;
        ModelNode result = finalResultRef.get();
        if (result != null) {
            // operation failed before it could commit
            System.out.println("Received final result " + result + " from " + host);
        } else {
            result = preparedResultRef.get();
            System.out.println("Received prepared result " + result + " from " + host);
            remoteTransaction = txRef.get();
        }

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
        }

        return finalResultRef.get();
    }

    ModelNode getUncommittedResult() throws InterruptedException {
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

        private final NewOperationContext context;

        DelegatingMessageHandler(final NewOperationContext context) {
            this.context = context;
        }

        @Override
        public void handleReport(MessageSeverity severity, String message) {
            context.report(severity, message);
        }
    }

    private static class DelegatingOperationAttachments implements OperationAttachments {

        private final NewOperationContext context;
        private DelegatingOperationAttachments(final NewOperationContext context) {
            this.context = context;
        }

        @Override
        public List<InputStream> getInputStreams() {
            int count = context.getAttachmentStreamCount();
            List<InputStream> result = new ArrayList<InputStream>(count);
            for (int i = 0; i < count; i++) {
                result.add(context.getAttachmentStream(count));
            }
            return result;
        }
    }
}

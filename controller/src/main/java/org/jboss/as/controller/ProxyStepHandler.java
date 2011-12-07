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

package org.jboss.as.controller;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.as.controller.client.MessageSeverity;
import org.jboss.as.controller.client.OperationAttachments;
import org.jboss.as.controller.client.OperationMessageHandler;
import org.jboss.dmr.ModelNode;

/**
 * Step handler that uses a proxied {@link ModelController} to execute the step.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class ProxyStepHandler implements OperationStepHandler {

    private final ProxyController proxyController;

    public ProxyStepHandler(final ProxyController proxyController) {
        this.proxyController = proxyController;
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) {
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

        proxyController.execute(operation, messageHandler, proxyControl, new DelegatingOperationAttachments(context));
        ModelNode finalResult = finalResultRef.get();
        if (finalResult != null) {
            // operation failed before it could commit
            context.getResult().set(finalResult.get(RESULT));
            context.getFailureDescription().set(finalResult.get(FAILURE_DESCRIPTION));
            context.completeStep();
        } else {

            completeRemoteTransaction(context, operation, txRef, preparedResultRef, finalResultRef);

        }
    }

    private void completeRemoteTransaction(OperationContext context, ModelNode operation, AtomicReference<ModelController.OperationTransaction> txRef, AtomicReference<ModelNode> preparedResultRef, AtomicReference<ModelNode> finalResultRef) {

        boolean txCompleted = false;
        try {

            ModelNode preparedResponse = preparedResultRef.get();
            ModelNode preparedResult =  preparedResponse.get(RESULT);
            if (preparedResponse.hasDefined(FAILURE_DESCRIPTION)) {
                context.getFailureDescription().set(preparedResponse.get(FAILURE_DESCRIPTION));
                if (preparedResult.isDefined()) {
                    context.getResult().set(preparedResult);
                }
            }
            else {
                context.getResult().set(preparedResult);
            }

            OperationContext.ResultAction resultAction = context.completeStep();
            ModelController.OperationTransaction tx = txRef.get();
            try {
                if (resultAction == OperationContext.ResultAction.KEEP) {
                    tx.commit();
                } else {
                    tx.rollback();
                }
            } finally {
                txCompleted = true;
            }

            // Get the final result from the proxy and use it to update our response.
            // Per the ProxyOperationControl contract, this will have been provided via operationCompleted
            // by the time the call to OperationTransaction.commit/rollback returns

            ModelNode finalResponse = finalResultRef.get();
            if (finalResponse != null) {
                ModelNode finalResult =  finalResponse.get(RESULT);
                if (finalResponse.hasDefined(FAILURE_DESCRIPTION)) {
                    context.getFailureDescription().set(finalResponse.get(FAILURE_DESCRIPTION));
                    if (finalResult.isDefined()) {
                        context.getResult().set(finalResult);
                    }
                } else {
                    context.getResult().set(finalResult);
                }
            } else {
                // This is an error condition
                ControllerLogger.SERVER_MANAGEMENT_LOGGER.noFinalProxyOutcomeReceived(operation.get(OP),
                        operation.get(OP_ADDR), proxyController.getProxyNodeAddress().toModelNode());
            }
        } finally {
            // Ensure the remote side gets a transaction outcome if we can't commit/rollback above
            if (!txCompleted && txRef.get() != null) {
                txRef.get().rollback();
            }
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

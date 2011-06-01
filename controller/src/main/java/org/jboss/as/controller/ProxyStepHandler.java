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

import org.jboss.as.controller.client.MessageSeverity;
import org.jboss.as.controller.client.NewOperation;
import org.jboss.as.controller.client.OperationAttachments;
import org.jboss.as.controller.client.OperationMessageHandler;
import org.jboss.dmr.ModelNode;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;

/**
 * Step handler that uses a proxied {@link NewModelController} to execute the step.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class ProxyStepHandler implements NewStepHandler {

    private final NewProxyController proxyController;

    public ProxyStepHandler(final NewProxyController proxyController) {
        this.proxyController = proxyController;
    }

    @Override
    public void execute(NewOperationContext context, ModelNode operation) {

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
        ModelNode finalResult = finalResultRef.get();
        if (finalResult != null) {
            // operation failed before it could commit
            context.getResult().set(finalResult.get(RESULT));
            context.getFailureDescription().set(finalResult.get(FAILURE_DESCRIPTION));
            context.completeStep();
        } else {
            ModelNode preparedResult = preparedResultRef.get();
            context.getResult().set(preparedResult.get(RESULT));
            if (preparedResult.hasDefined(FAILURE_DESCRIPTION)) {
                context.getFailureDescription().set(finalResult.get(FAILURE_DESCRIPTION));
            }

            NewOperationContext.ResultAction resultAction = context.completeStep();
            NewModelController.OperationTransaction tx = txRef.get();
            if (tx != null) {
                if (resultAction == NewOperationContext.ResultAction.KEEP) {
                    tx.commit();
                } else {
                    tx.rollback();
                }
            }

            // TODO what if the response on the proxy changed following the operationPrepared callback?
            // e.g. the rolled back flag? This may all be ok, just needs to be thought about, tested.
            // If it proves unnecessary, get rid of ProxyOperationControl
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
                try {
                    result.add(context.getAttachmentStream(count));
                } catch (IOException e) {
                    // TODO use NewOperationAttachments; it's a better API for this usage
                    throw new RuntimeException(e);
                }
            }
            return result;
        }
    }
}

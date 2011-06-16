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

    private NewModelController.OperationTransaction remoteTransaction;

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

        ModelNode result = finalResultRef.get();
        if (result != null) {
            // operation failed before it could commit
        } else {
            result = preparedResultRef.get();
            remoteTransaction = txRef.get();
        }

        return result;
    }


    NewModelController.OperationTransaction getRemoteTransaction() {
        return remoteTransaction;
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

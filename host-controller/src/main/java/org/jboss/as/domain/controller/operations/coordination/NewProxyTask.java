/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.client.MessageSeverity;
import org.jboss.as.controller.client.OperationAttachments;
import org.jboss.as.controller.client.OperationMessageHandler;
import org.jboss.as.controller.remote.BlockingQueueOperationListener;
import org.jboss.as.controller.remote.TransactionalOperationImpl;
import org.jboss.as.controller.remote.TransactionalProtocolClient;
import org.jboss.dmr.ModelNode;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

/**
 * @author Emanuel Muckenhuber
 */
public class NewProxyTask {

    private final String name;
    private final ModelNode operation;
    private final OperationContext context;
    private final TransactionalProtocolClient client;

    public NewProxyTask(final String name, final ModelNode operation, final OperationContext context, final TransactionalProtocolClient client) {
        this.name = name;
        this.client = client;
        this.context = context;
        this.operation = operation;
    }

    public Future<ModelNode> execute(final ProxyOperationListener listener) {
        final OperationMessageHandler messageHandler = new DelegatingMessageHandler(context);
        final OperationAttachments operationAttachments = new DelegatingOperationAttachments(context);
        final ProxyOperation proxyOperation = new ProxyOperation(name, operation, messageHandler, operationAttachments);
        try {
            return client.execute(listener, proxyOperation);
        } catch (IOException e) {
            final TransactionalProtocolClient.PreparedOperation<ProxyOperation> result = BlockingQueueOperationListener.FailedOperation.create(proxyOperation, e);
            listener.operationPrepared(result);
            return result.getFinalResult();
        }
    }

    static class ProxyOperation extends TransactionalOperationImpl {

        private final String name;
        protected ProxyOperation(final String name, final ModelNode operation, final OperationMessageHandler messageHandler, final OperationAttachments attachments) {
            super(operation, messageHandler, attachments);
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    static class ProxyOperationListener extends BlockingQueueOperationListener<ProxyOperation> {

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

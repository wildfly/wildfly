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

package org.jboss.as.domain.controller.plan;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.client.MessageSeverity;
import org.jboss.as.controller.client.OperationAttachments;
import org.jboss.as.controller.client.OperationMessageHandler;
import org.jboss.as.controller.remote.BlockingQueueOperationListener;
import org.jboss.as.controller.remote.TransactionalOperationImpl;
import org.jboss.as.controller.remote.TransactionalProtocolClient;
import org.jboss.as.domain.controller.ServerIdentity;
import org.jboss.dmr.ModelNode;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

/**
 * @author Emanuel Muckenhuber
 */
public abstract class ServerTaskExecutor {

    private final OperationContext context;
    private final ServerRolloutTaskHandler rolloutHandler;

    protected ServerTaskExecutor(OperationContext context, ServerRolloutTaskHandler rolloutHandler) {
        this.context = context;
        this.rolloutHandler = rolloutHandler;
    }

    public boolean executeTask(final TransactionalProtocolClient.TransactionalOperationListener<ServerOperation> listener, final ServerTask task) {
        return execute(listener, task.getServerIdentity(), task.getOperation());
    }

    protected abstract boolean execute(final TransactionalProtocolClient.TransactionalOperationListener<ServerOperation> listener, final ServerIdentity identity, final ModelNode operation);

    protected boolean executeOperation(final TransactionalProtocolClient.TransactionalOperationListener<ServerOperation> listener, TransactionalProtocolClient client, final ServerIdentity identity, final ModelNode operation) {
        if(client == null) {
            return false;
        }
        final OperationMessageHandler messageHandler = new DelegatingMessageHandler(context);
        final OperationAttachments operationAttachments = new DelegatingOperationAttachments(context);
        final ServerOperation serverOperation = new ServerOperation(identity, operation, messageHandler, operationAttachments);
        try {
            final Future<ModelNode> result = client.execute(listener, serverOperation);
            rolloutHandler.recordExecutedRequest(new ServerRolloutTaskHandler.ServerExecutedRequest(identity, result));
        } catch (IOException e) {
            final TransactionalProtocolClient.PreparedOperation<ServerOperation> result = BlockingQueueOperationListener.FailedOperation.create(serverOperation, e);
            listener.operationPrepared(result);
            rolloutHandler.recordExecutedRequest(new ServerRolloutTaskHandler.ServerExecutedRequest(identity, result.getFinalResult()));
        }
        return true;
    }

    static class ServerOperationListener extends BlockingQueueOperationListener<ServerOperation> {

    }

    public static class ServerOperation extends TransactionalOperationImpl {

        private final ServerIdentity identity;
        ServerOperation(ServerIdentity identity, ModelNode operation, OperationMessageHandler messageHandler, OperationAttachments attachments) {
            super(operation, messageHandler, attachments);
            this.identity = identity;
        }

        public ServerIdentity getIdentity() {
            return identity;
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

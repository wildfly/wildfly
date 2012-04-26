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

import org.jboss.as.controller.Cancellable;
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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * @author Emanuel Muckenhuber
 */
public abstract class ServerTaskExecutor {

    private final OperationContext context;
    private final Map<ServerIdentity, ExecutedServerRequest> submittedTasks;
    private final List<ServerTaskExecutor.ServerPreparedResponse> preparedResults;

    protected ServerTaskExecutor(OperationContext context, Map<ServerIdentity, ExecutedServerRequest> submittedTasks, List<ServerPreparedResponse> preparedResults) {
        this.context = context;
        this.submittedTasks = submittedTasks;
        this.preparedResults = preparedResults;
    }

    /**
     * Execute
     *
     * @param listener the transactional operation listener
     * @param identity the server identity
     * @param operation the operation
     * @return whether the operation was executed or not
     */
    protected abstract boolean execute(final TransactionalProtocolClient.TransactionalOperationListener<ServerOperation> listener, final ServerIdentity identity, final ModelNode operation);

    /**
     * Execute a server task.
     *
     * @param listener the transactional server listener
     * @param task the server task
     * @return whether the task was executed or not
     */
    public boolean executeTask(final TransactionalProtocolClient.TransactionalOperationListener<ServerOperation> listener, final ServerUpdateTask task) {
        return execute(listener, task.getServerIdentity(), task.getOperation());
    }

    /**
     * Execute the operation.
     *
     * @param listener the transactional operation listener
     * @param client the transactional protocol client
     * @param identity the server identity
     * @param operation the operation
     * @return whether the operation was executed
     */
    protected boolean executeOperation(final TransactionalProtocolClient.TransactionalOperationListener<ServerOperation> listener, TransactionalProtocolClient client, final ServerIdentity identity, final ModelNode operation) {
        if(client == null) {
            return false;
        }
        final OperationMessageHandler messageHandler = new DelegatingMessageHandler(context);
        final OperationAttachments operationAttachments = new DelegatingOperationAttachments(context);
        final ServerOperation serverOperation = new ServerOperation(identity, operation, messageHandler, operationAttachments);
        try {
            final Future<ModelNode> result = client.execute(listener, serverOperation);
            recordExecutedRequest(new ExecutedServerRequest(identity, result));
        } catch (IOException e) {
            final TransactionalProtocolClient.PreparedOperation<ServerOperation> result = BlockingQueueOperationListener.FailedOperation.create(serverOperation, e);
            listener.operationPrepared(result);
            recordExecutedRequest(new ExecutedServerRequest(identity, result.getFinalResult()));
        }
        return true;
    }

    /**
     * Record a executed request.
     *
     * @param task the executed task
     */
    void recordExecutedRequest(final ExecutedServerRequest task) {
        synchronized (submittedTasks) {
            submittedTasks.put(task.getIdentity(), task);
        }
    }

    /**
     * Record a prepare operation.
     *
     * @param preparedOperation the prepared operation
     */
    void recordPreparedOperation(final TransactionalProtocolClient.PreparedOperation<ServerTaskExecutor.ServerOperation> preparedOperation) {
        recordPreparedTask(new ServerTaskExecutor.ServerPreparedResponse(preparedOperation));
    }

    /**
     * Record a prepared operation.
     *
     * @param task the prepared operation
     */
    void recordPreparedTask(ServerTaskExecutor.ServerPreparedResponse task) {
        synchronized (preparedResults) {
            preparedResults.add(task);
        }
    }

    static class ServerOperationListener extends BlockingQueueOperationListener<ServerOperation> {

        @Override
        protected void drainTo(Collection<TransactionalProtocolClient.PreparedOperation<ServerOperation>> preparedOperations) {
            super.drainTo(preparedOperations);
        }

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

    /**
     * The prepared response.
     */
    public static class ServerPreparedResponse {

        private TransactionalProtocolClient.PreparedOperation<ServerOperation> preparedOperation;
        ServerPreparedResponse(TransactionalProtocolClient.PreparedOperation<ServerOperation> preparedOperation) {
            this.preparedOperation = preparedOperation;
        }

        public TransactionalProtocolClient.PreparedOperation<ServerOperation> getPreparedOperation() {
            return preparedOperation;
        }

        public ServerIdentity getServerIdentity() {
            return preparedOperation.getOperation().getIdentity();
        }

        public String getServerGroupName() {
            return getServerIdentity().getServerGroupName();
        }

        /**
         * Finalize the transaction. This will return {@code false} in case the local operation failed,
         * but the overall state of the operation is commit=true.
         *
         * @param commit {@code true} to commit, {@code false} to rollback
         * @return whether the local proxy operation result is in sync with the overall operation
         */
        public boolean finalizeTransaction(boolean commit) {
            final boolean failed = preparedOperation.isFailed();
            if(commit && failed) {
                return false;
            }
            if(commit) {
                preparedOperation.commit();
            } else {
                if(!failed) {
                    preparedOperation.rollback();
                }
            }
            return true;
        }

    }

    /**
     * The executed request.
     */
    public static class ExecutedServerRequest implements Cancellable {

        private final ServerIdentity identity;
        private final Future<ModelNode> finalResult;

        public ExecutedServerRequest(ServerIdentity identity, Future<ModelNode> finalResult) {
            this.identity = identity;
            this.finalResult = finalResult;
        }

        public ServerIdentity getIdentity() {
            return identity;
        }

        public Future<ModelNode> getFinalResult() {
            return finalResult;
        }

        @Override
        public boolean cancel() {
            return finalResult.cancel(false);
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

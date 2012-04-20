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
import org.jboss.as.controller.remote.TransactionalProtocolClient;
import org.jboss.as.domain.controller.ServerIdentity;
import org.jboss.dmr.ModelNode;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * Task result handler.
 *
 * @author Emanuel Muckenhuber
 */
public class ServerRolloutTaskHandler {

    private final Map<ServerIdentity, ServerExecutedRequest> submittedTasks;
    private final List<ServerPreparedResponse> preparedResults;

    public ServerRolloutTaskHandler(final Map<ServerIdentity, ServerExecutedRequest> submittedTasks, List<ServerPreparedResponse> preparedResults) {
        this.submittedTasks = submittedTasks;
        this.preparedResults = preparedResults;
    }

    /**
     * Record a executed request.
     *
     * @param task the executed task
     */
    void recordExecutedRequest(final ServerExecutedRequest task) {
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
        recordPreparedTask(new ServerPreparedResponse(preparedOperation));
    }

    /**
     * Record a prepared operation.
     *
     * @param task the prepared operation
     */
    void recordPreparedTask(ServerPreparedResponse task) {
        synchronized (preparedResults) {
            preparedResults.add(task);
        }
    }

    /**
     * The prepared response.
     */
    public static class ServerPreparedResponse {

        private TransactionalProtocolClient.PreparedOperation<ServerTaskExecutor.ServerOperation> preparedOperation;
        ServerPreparedResponse(TransactionalProtocolClient.PreparedOperation<ServerTaskExecutor.ServerOperation> preparedOperation) {
            this.preparedOperation = preparedOperation;
        }

        public TransactionalProtocolClient.PreparedOperation<ServerTaskExecutor.ServerOperation> getPreparedOperation() {
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
    public static class ServerExecutedRequest implements Cancellable {

        private final ServerIdentity identity;
        private final Future<ModelNode> finalResult;

        public ServerExecutedRequest(ServerIdentity identity, Future<ModelNode> finalResult) {
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


}

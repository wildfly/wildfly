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

package org.jboss.as.controller.remote;

import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.client.OperationAttachments;
import org.jboss.as.controller.client.OperationMessageHandler;
import org.jboss.as.protocol.mgmt.ManagementChannelAssociation;
import org.jboss.as.protocol.mgmt.ManagementChannelHandler;
import org.jboss.as.protocol.mgmt.ManagementRequestHandlerFactory;
import org.jboss.dmr.ModelNode;
import org.jboss.remoting3.Channel;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @author Emanuel Muckenhuber
 */
public final class TransactionalProtocolHandlers {

    private TransactionalProtocolHandlers() {
        //
    }

    /**
     * Add a transaction protocol request handler to an existing channel.
     *
     * @param association the channel association
     * @param controller the model controller
     */
    public static void addAsHandlerFactory(final ManagementChannelHandler association, final ModelController controller) {
        final ManagementRequestHandlerFactory handlerFactory = createHandler(association, controller);
        association.addHandlerFactory(handlerFactory);
    }

    /**
     * Create a transactional protocol request handler.
     *
     * @param association the management channel
     * @param controller the model controller
     * @return the handler factory
     */
    public static ManagementRequestHandlerFactory createHandler(final ManagementChannelAssociation association, final ModelController controller) {
        return new TransactionalProtocolOperationHandler(controller, association);
    }

    public static class OperationImpl implements TransactionalProtocolClient.Operation {

        private final ModelNode operation;
        private final OperationMessageHandler messageHandler;
        private final OperationAttachments attachments;

        protected OperationImpl(final ModelNode operation, final OperationMessageHandler messageHandler, final OperationAttachments attachments) {
            this.operation = operation;
            this.messageHandler = messageHandler;
            this.attachments = attachments;
        }

        @Override
        public ModelNode getOperation() {
            return operation;
        }

        @Override
        public OperationMessageHandler getMessageHandler() {
            return messageHandler;
        }

        @Override
        public OperationAttachments getAttachments() {
            return attachments;
        }

    }

    static class PreparedOperationImpl<T extends TransactionalProtocolClient.Operation> implements TransactionalProtocolClient.PreparedOperation<T> {

        private final T operation;
        private final ModelNode preparedResult;
        private final Future<ModelNode> finalResult;
        private final ModelController.OperationTransaction transaction;

        protected PreparedOperationImpl(T operation, ModelNode preparedResult, Future<ModelNode> finalResult, ModelController.OperationTransaction transaction) {
            this.operation = operation;
            this.preparedResult = preparedResult;
            this.finalResult = finalResult;
            this.transaction = transaction;
        }

        @Override
        public T getOperation() {
            return operation;
        }

        @Override
        public ModelNode getPreparedResult() {
            return preparedResult;
        }

        @Override
        public boolean isFailed() {
            return false;
        }

        @Override
        public boolean isDone() {
            return finalResult.isDone();
        }

        @Override
        public Future<ModelNode> getFinalResult() {
            return finalResult;
        }

        @Override
        public void commit() {
            transaction.commit();
        }

        @Override
        public void rollback() {
            transaction.rollback();
        }

    }

    public static class FailedOperation<T extends TransactionalProtocolClient.Operation> implements TransactionalProtocolClient.PreparedOperation<T> {

        private final T operation;
        private final ModelNode finalResult;

        public FailedOperation(final T operation, final ModelNode finalResult) {
            this.operation = operation;
            this.finalResult = finalResult;
        }

        @Override
        public T getOperation() {
            return operation;
        }

        @Override
        public ModelNode getPreparedResult() {
            return finalResult;
        }

        @Override
        public boolean isDone() {
            return true;
        }

        @Override
        public boolean isFailed() {
            return true;
        }

        @Override
        public Future<ModelNode> getFinalResult() {
            return new Future<ModelNode>() {
                @Override
                public boolean cancel(boolean mayInterruptIfRunning) {
                    return false;
                }

                @Override
                public boolean isCancelled() {
                    return false;
                }

                @Override
                public boolean isDone() {
                    return true;
                }

                @Override
                public ModelNode get() {
                    return finalResult;
                }

                @Override
                public ModelNode get(long timeout, TimeUnit unit) {
                    return finalResult;
                }
            };
        }

        @Override
        public void commit() {
            throw new IllegalStateException();
        }

        @Override
        public void rollback() {
            throw new IllegalStateException();
        }

    }

    /**
     * Basic operation listener backed by a blocking queue. If the limit of the queue is reached prepared operations
     * are going to be rolled back automatically.
     *
     * @param <T> the operation type
     */
    public static class BlockingQueueOperationListener<T extends TransactionalProtocolClient.Operation> implements TransactionalProtocolClient.OperationListener<T> {

        private final BlockingQueue<TransactionalProtocolClient.PreparedOperation<T>> queue;
        public BlockingQueueOperationListener(final BlockingQueue<TransactionalProtocolClient.PreparedOperation<T>> queue) {
            this.queue = queue;
        }

        @Override
        public void operationPrepared(final TransactionalProtocolClient.PreparedOperation<T> prepared) {
            if(! queue.offer(prepared)) {
                prepared.rollback();
            }
        }

        @Override
        public void operationFailed(T operation, ModelNode result) {
            queue.offer(new FailedOperation<T>(operation, result));
        }

        @Override
        public void operationComplete(T operation, ModelNode result) {
            //
        }

        /**
         * Retrieves and removes the head of the underlying queue, waiting if necessary until an element becomes available.
         *
         * @return the prepared operation
         * @throws InterruptedException
         */
        public TransactionalProtocolClient.PreparedOperation<T> retrievePreparedOperation() throws InterruptedException {
            return queue.take();
        }

        /**
         * Retrieves and removes the head of this queue, waiting up to the specified wait time if necessary for an element to become available.
         *
         * @param timeout the timeout
         * @param timeUnit the time unit
         * @return the prepared operation
         * @throws InterruptedException
         */
        public TransactionalProtocolClient.PreparedOperation<T> retrievePreparedOperation(final long timeout, final TimeUnit timeUnit) throws InterruptedException {
            return queue.poll(timeout, timeUnit);
        }

    }

}

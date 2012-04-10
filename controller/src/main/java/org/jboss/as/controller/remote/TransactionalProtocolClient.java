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
import org.jboss.dmr.ModelNode;

import java.io.IOException;
import java.util.concurrent.Future;

/**
 * A transactional protocol client.
 *
 * @author Emanuel Muckenhuber
 */
public interface TransactionalProtocolClient {

    /**
     * Execute an operation. This returns a future for the final result, which will only available after the prepared
     * operation is committed.
     *
     * @param listener the operation listener
     * @param operation the operation
     * @param messageHandler the operation message handler
     * @param attachments the operation attachments
     * @return the future result
     * @throws IOException
     */
    Future<ModelNode> execute(TransactionalOperationListener<Operation> listener, ModelNode operation, OperationMessageHandler messageHandler, OperationAttachments attachments) throws IOException;

    /**
     * Execute an operation. This returns a future for the final result, which will only available after the prepared
     * operation is committed.
     *
     * @param listener the operation listener
     * @param operation the operation
     * @param <T> the operation type
     * @return the future result
     * @throws IOException
     */
    <T extends Operation> Future<ModelNode> execute(TransactionalOperationListener<T> listener, T operation) throws IOException;

    /**
     * The transactional operation listener.
     *
     * @param <T> the operation type
     */
    interface TransactionalOperationListener<T extends Operation> {

        /**
         * Notification that an operation was prepared.
         *
         * @param prepared the prepared operation
         */
        void operationPrepared(PreparedOperation<T> prepared);

        /**
         * Notification that an operation failed.
         *
         * @param operation the operation
         * @param result the operation result
         */
        void operationFailed(T operation, ModelNode result);

        /**
         * Notification that an operation completed.
         *
         * @param operation the operation
         * @param result the final result
         */
        void operationComplete(T operation, ModelNode result);

    }

    /**
     * A operation wrapper.
     */
    interface Operation {

        /**
         * Get the underlying operation.
         *
         * @return the operation
         */
        ModelNode getOperation();

        /**
         * Get the operation message handler.
         *
         * @return the message handler
         */
        OperationMessageHandler getMessageHandler();

        /**
         * Get the operation attachments.
         *
         * @return the attachments
         */
        OperationAttachments getAttachments();

    }

    /**
     * The prepared result.
     *
     * @param <T> the operation type
     */
    interface PreparedOperation<T extends Operation> extends ModelController.OperationTransaction {

        /**
         * Get the initial operation.
         *
         * @return the operation
         */
        T getOperation();

        /**
         * Get the prepared result
         * @return
         */
        ModelNode getPreparedResult();

        /**
         * Check if prepare failed
         *
         * @return
         */
        boolean isFailed();

        /**
         * Is done.
         *
         * @return whether the operation is complete (done or failed).
         */
        boolean isDone();

        /**
         * Get the final result.
         *
         * @return
         */
        Future<ModelNode> getFinalResult();

    }

}

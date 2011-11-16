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

package org.jboss.as.protocol.mgmt;

import org.jboss.as.protocol.ProtocolLogger;
import org.jboss.threads.AsyncFuture;
import org.jboss.threads.AsyncFutureTask;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;

/**
 * Management operation support.
 *
 * @author Emanuel Muckenhuber
 */
class ActiveOperationSupport<T, A> {

    private static final Executor directExecutor = new Executor() {

        @Override
        public void execute(final Runnable command) {
            command.run();
        }
    };

    private final ActiveOperation.CompletedCallback<T> NO_OP_CALLBACK = new ActiveOperation.CompletedCallback<T>() {

        @Override
        public void completed(T result) {
            //
        }

        @Override
        public void failed(Exception e) {
            //
        }

        @Override
        public void cancelled() {
            //
        }
    };

    private final Executor executor;
    private final ManagementBatchIdManager operationIdManager = ManagementBatchIdManager.DEFAULT;
    private final ConcurrentMap<Integer, ActiveOperationImpl<T, A>> activeRequests = new ConcurrentHashMap<Integer, ActiveOperationImpl<T, A>>();

    protected ActiveOperationSupport() {
        this(directExecutor);
    }

    protected ActiveOperationSupport(final Executor executor) {
        this.executor = executor;
    }

    /**
     * Register an active operation. The operation-id will be generated.
     *
     * @param attachment the shared attachment
     * @return the active operation
     */
    protected ActiveOperation<T, A> registerActiveOperation(A attachment) {
        return registerActiveOperation(attachment, NO_OP_CALLBACK);
    }

    /**
     * Register an active operation. The operation-id will be generated.
     *
     * @param attachment the shared attachment
     * @param callback the completed callback
     * @return the active operation
     */
    protected ActiveOperation<T, A> registerActiveOperation(A attachment, ActiveOperation.CompletedCallback<T> callback) {
        final Integer i = operationIdManager.createBatchId();
        return registerActiveOperation(i, attachment, callback);
    }

    /**
     * Register an active operation with a specific operation id.
     *
     * @param id the operation id
     * @param attachment the shared attachemnt
     * @return
     */
    protected ActiveOperation<T, A> registerActiveOperation(final Integer id, A attachment) {
        return registerActiveOperation(id, attachment, NO_OP_CALLBACK);
    }

    /**
     * Register an active operation with a specific operatino id.
     *
     * @param id the operation id
     * @param attachment the shared attachment
     * @param callback the completed callback
     * @return the active operation
     */
    protected ActiveOperation<T, A> registerActiveOperation(final Integer id, A attachment, ActiveOperation.CompletedCallback<T> callback) {
        final ActiveOperationImpl<T, A> request = new ActiveOperationImpl(id, attachment, callback);
        final ActiveOperation<?, ?> existing =  activeRequests.putIfAbsent(id, request);
        if(existing != null) {
            throw new IllegalStateException();
        }
        return request;
    }

    /**
     * Get an active operation.
     *
     * @param header the request header
     * @return the active operation, {@code null} if if there is no registered operation
     */
    protected ActiveOperation<T, A> getActiveOperation(final ManagementRequestHeader header) {
        return getActiveOperation(header.getBatchId());
    }

    /**
     * Get the active operation.
     *
     * @param id the active operation id
     * @return the active operation, {@code null} if if there is no registered operation
     */
    protected ActiveOperation<T, A> getActiveOperation(final Integer id) {
        return activeRequests.get(id);
    }

    /**
     * Remove an active operation.
     *
     * @param id the operation id
     * @return the removed active operation, {@code null} if there was no registered operation
     */
    protected ActiveOperation<T, A> removeActiveOperation(final Integer id) {
        final ActiveOperation<T, A> removed = activeRequests.remove(id);
        if(removed != null) {
            operationIdManager.freeBatchId(id);
        }
        return removed;
    }

    /**
     * Cancel all currently active operations.
     */
    protected void cancelAllActiveOperations() {
        for(final ActiveOperationImpl<T, A> activeOperation : activeRequests.values()) {
            activeOperation.cancel();
        }
    }

    /**
     * Cancel a specific operation.
     *
     * @param id the operation id
     * @return {@code true} if the operation was cancelled, {@code false} otherwise
     */
    protected boolean cancelActiveOperation(final Integer id) {
        final ActiveOperationImpl<T, A> request = activeRequests.get(id);
        if(request != null) {
            return request.cancel();
        }
        return false;
    }

    protected class ActiveOperationImpl<T, A> extends AsyncFutureTask<T> implements ActiveOperation<T, A> {

        private final A attachment;
        private final Integer operationId;
        private ResultHandler<T> completionHandler = new ResultHandler<T>() {
            @Override
            public boolean done(T result) {
                try {
                    return ActiveOperationImpl.this.setResult(result);
                } finally {
                    removeActiveOperation(operationId);
                }
            }

            @Override
            public boolean failed(Exception e) {
                try {
                    return ActiveOperationImpl.this.setFailed(e);
                } finally {
                    removeActiveOperation(operationId);
                    ProtocolLogger.ROOT_LOGGER.debugf(e, "active-op (%d) failed %s", operationId, attachment);
                }
            }

            @Override
            public void cancel() {
                ActiveOperationImpl.this.cancel();
            }
        };

        private ActiveOperationImpl(final Integer operationId, final A attachment, final CompletedCallback<T> callback) {
            super(executor);
            this.operationId = operationId;
            this.attachment = attachment;
            addListener(new Listener<T, Object>() {
                @Override
                public void handleComplete(AsyncFuture<? extends T> asyncFuture, Object attachment) {
                    try {
                        callback.completed(asyncFuture.get());
                    } catch (Exception e) {
                        //
                    }
                }

                @Override
                public void handleFailed(AsyncFuture<? extends T> asyncFuture, Throwable cause, Object attachment) {
                    if(cause instanceof Exception) {
                        callback.failed((Exception) cause);
                    } else {
                        callback.failed(new RuntimeException(cause));
                    }
                }

                @Override
                public void handleCancelled(AsyncFuture<? extends T> asyncFuture, Object attachment) {
                    removeActiveOperation(operationId);
                    callback.cancelled();
                    ProtocolLogger.ROOT_LOGGER.infof("cancelled operation (%d) attachment: (%s) this: %s.", getOperationId(), getAttachment(), ActiveOperationSupport.this);
                }
            }, null);
        }

        @Override
        public Integer getOperationId() {
            return operationId;
        }

        @Override
        public ResultHandler<T> getResultHandler() {
            return completionHandler;
        }

        @Override
        public A getAttachment() {
            return attachment;
        }

        @Override
        public AsyncFuture<T> getResult() {
            return this;
        }

        @Override
        public void asyncCancel(boolean interruptionDesired) {
            // TODO also interrupt associated threads
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    setCancelled();
                }
            });
        }

        public boolean cancel() {
            return super.cancel(true);
        }

    }

}

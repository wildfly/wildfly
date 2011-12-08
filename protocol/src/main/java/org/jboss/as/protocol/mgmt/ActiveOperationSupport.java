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
import org.jboss.as.protocol.ProtocolMessages;
import org.jboss.threads.AsyncFuture;
import org.jboss.threads.AsyncFutureTask;
import org.xnio.Cancellable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Management operation support encapsulating active operations.
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
    private final ConcurrentMap<Integer, ActiveOperationImpl<T, A>> activeRequests = new ConcurrentHashMap<Integer, ActiveOperationImpl<T, A>>();
    private final ManagementBatchIdManager operationIdManager = new ManagementBatchIdManager.DefaultManagementBatchIdManager();

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition();
    // mutable variables, have to be guarded by the lock
    private int activeCount = 0;
    private volatile boolean shutdown = false;

    protected ActiveOperationSupport(final Executor executor) {
        this.executor = executor != null ? executor : directExecutor;
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
        return registerActiveOperation(null, attachment, callback);
    }

    /**
     * Register an active operation with a specific operation id.
     *
     * @param id the operation id
     * @param attachment the shared attachment
     * @return the created active operation
     */
    protected ActiveOperation<T, A> registerActiveOperation(final Integer id, A attachment) {
        return registerActiveOperation(id, attachment, NO_OP_CALLBACK);
    }

    /**
     * Register an active operation with a specific operation id.
     *
     * @param id the operation id
     * @param attachment the shared attachment
     * @param callback the completed callback
     * @return the created active operation
     */
    protected ActiveOperation<T, A> registerActiveOperation(final Integer id, A attachment, ActiveOperation.CompletedCallback<T> callback) {
        lock.lock(); try {
            // Check that we still allow registration
            assert ! shutdown;
            final Integer operationId;
            if(id == null) {
                // If we did not get a operationId, create a new one
                operationId = operationIdManager.createBatchId();
            } else {
                // Check that the operationId is not already taken
                if(! operationIdManager.lockBatchId(id)) {
                    throw ProtocolMessages.MESSAGES.operationIdAlreadyExists(id);
                }
                operationId = id;
            }
            final ActiveOperationImpl<T, A> request = new ActiveOperationImpl(operationId, attachment, callback);
            final ActiveOperation<?, ?> existing =  activeRequests.putIfAbsent(operationId, request);
            if(existing != null) {
                throw ProtocolMessages.MESSAGES.operationIdAlreadyExists(operationId);
            }
            activeCount++; // condition.signalAll();
            return request;
        } finally {
            lock.unlock();
        }
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
    private ActiveOperation<T, A> removeActiveOperation(final Integer id) {
        lock.lock(); try {
            final ActiveOperation<T, A> removed = activeRequests.remove(id);
            if(removed != null) {
                activeCount--;
                operationIdManager.freeBatchId(id);
                condition.signalAll();
            }
            return removed;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Cancel all currently active operations.
     */
    protected void cancelAllActiveOperations() {
        final List<ActiveOperationImpl<T, A>> operations = new ArrayList<ActiveOperationImpl<T, A>>();
        for(final ActiveOperationImpl<T, A> activeOperation : activeRequests.values()) {
            activeOperation.asyncCancel(false);
            operations.add(activeOperation);
        }
    }

    /**
     * Is shutdown.
     *
     * @return {@code true} if the shutdown method was called, {@code false} otherwise
     */
    protected boolean isShutdown() {
        return shutdown;
    }

    /**
     * Prevent new active operations get registered.
     */
    protected void shutdown() {
        lock.lock(); try {
            shutdown = true;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Await the completion of all currently active operations.
     *
     * @param timeout the timeout
     * @param unit the time unit
     * @return {@code } false if the timeout was reached and there were still active operations
     * @throws InterruptedException
     */
    protected boolean awaitCompletion(long timeout, TimeUnit unit) throws InterruptedException {
        long deadline = unit.toMillis(timeout) + System.currentTimeMillis();
        lock.lock(); try {
            assert shutdown;
            while(activeCount != 0) {
                long remaining = deadline - System.currentTimeMillis();
                if (remaining <= 0) {
                    return activeCount == 0;
                }
                condition.await(remaining, TimeUnit.MILLISECONDS);
            }
            return activeCount == 0;
        } finally {
            lock.unlock();
        }
    }

    private static final List<Cancellable> CANCEL_REQUESTED = Collections.emptyList();

    protected class ActiveOperationImpl<T, A> extends AsyncFutureTask<T> implements ActiveOperation<T, A> {

        private final A attachment;
        private final Integer operationId;
        private List<Cancellable> cancellables;

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
                    ProtocolLogger.ROOT_LOGGER.debugf("cancelled operation (%d) attachment: (%s) this: %s.", getOperationId(), getAttachment(), ActiveOperationSupport.this);
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
            final List<Cancellable> cancellables;
            synchronized (this) {
                cancellables = this.cancellables;
                if (cancellables == null || cancellables == CANCEL_REQUESTED) {
                    return;
                }
                this.cancellables = CANCEL_REQUESTED;
            }
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    for (Cancellable cancellable : cancellables) {
                        cancellable.cancel();
                    }
                    setCancelled();
                }
            });
        }

        @Override
        public void addCancellable(final Cancellable cancellable) {
            // Perhaps just use the IOFuture from XNIO...
            synchronized (lock) {
                switch (getStatus()) {
                    case CANCELLED:
                        break;
                    case WAITING:
                        final List<Cancellable> cancellables = this.cancellables;
                        if (cancellables == CANCEL_REQUESTED) {
                            break;
                        } else {
                            ((cancellables == null) ? (this.cancellables = new ArrayList<Cancellable>()) : cancellables).add(cancellable);
                        }
                    default:
                        return;
                }
            }
            cancellable.cancel();
        }

        public boolean cancel() {
            return super.cancel(true);
        }

    }

}

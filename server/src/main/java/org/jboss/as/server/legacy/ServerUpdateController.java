/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.server.legacy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.as.model.AbstractServerModelUpdate;
import org.jboss.as.model.ServerModel;
import org.jboss.as.model.UpdateContext;
import org.jboss.as.model.UpdateFailedException;
import org.jboss.as.model.UpdateResultHandler;
import org.jboss.logging.Logger;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceRegistryException;

/**
 * Coordinates the execution of a set of {@link AbstractServerModelUpdate updates}.
 */
public class ServerUpdateController {

    /** The status of the overall model update */
    public enum Status {
        /**
         * The {@link ServerUpdateController#executeUpdates()} method has not
         * yet been invoked
         */
        PENDING,
        /** The {@link ServerUpdateController#executeUpdates()} method has been
         * invoked and updates are being applied.*/
        ACTIVE,
        /** An error has occurred with one or more updates */
        MARKED_ROLLBACK,
        /** Updates are being rolled back */
        ROLLING_BACK,
        /**
         * All updates have completed or been rolled back; the
         * overall update process is finishing.
         */
        COMMITING,
        /**
         * The overall update process has finished following a rollback of
         * attempted updates.
         */
        ROLLED_BACK,
        COMMITTED
    }

    public static interface ServerUpdateCommitHandler {
        void handleUpdateCommit(ServerUpdateController controller, Status priorStatus);
    }

    private static Logger logger = Logger.getLogger("org.jboss.as.server.deployment");

    private volatile Status status = Status.PENDING;
    private final boolean allowOverallRollback;
    private final boolean allowRuntimeUpdates;
    /** The updates to execute */
    private final List<ServerModelUpdateTuple<?,?>> updates = new ArrayList<ServerModelUpdateTuple<?,?>>();
    /** Compensating updates for updates that that have succeeded and may need rollback */
    private final List<ServerModelUpdateTuple<?,?>> rollbacks = new ArrayList<ServerModelUpdateTuple<?,?>>();
    /** ServerModel against which updates are executed */
    private final ServerModel serverModel;
    /** ServiceContainer to use for runtime changes */
    private final ServiceContainer serviceContainer;
    /** Thread pool for async rollback and commit handling */
    private final Executor executor;
    private final ServerUpdateCommitHandler commitHandler;
    /** Tracks completion of non-rollback updates */
    private final AtomicInteger updatedCount = new AtomicInteger();
    /** Tracks completion of rollback updates */
    private final AtomicInteger rolledBackCount = new AtomicInteger();

    public ServerUpdateController(final ServerModel serverModel,
            final ServiceContainer serviceContainer,
            final Executor executor,
            final ServerUpdateCommitHandler commitHandler,
            final boolean allowOverallRollback,
            final boolean allowRuntimeUpdates) {
        this.serverModel = serverModel;
        this.serviceContainer = serviceContainer;
        this.executor = executor;
        this.commitHandler = commitHandler;
        this.allowOverallRollback = allowOverallRollback;
        this.allowRuntimeUpdates = allowRuntimeUpdates;
    }

    /**
     * Add a new update to the set of updates to be executed.
     *
     * @param <R> the result type provided by the update
     * @param <P> the parameter type expected by the result handler
     * @param update the update. Cannot be {@code null}
     * @param resultHandler a handler for the update result. May be {@code null}
     * @param param parameter to pass to {@code resultHandler}. May be {@code null}
     *               if {@code <P>} is {@code Void} or {@code resultHandler} is
     *               {@code null}.
     *
     * @throws IllegalStateException if {@link #getStatus()} is not {@link Status#PENDING}
     */
    public <R, P> void addServerModelUpdate(AbstractServerModelUpdate<R> update, UpdateResultHandler<? super R, P> resultHandler, P param) {
        synchronized (this) {
            if (status != Status.PENDING)
                throw new IllegalStateException("Cannot add updates after executeUpdates() has been invoked");
            updates.add(new ServerModelUpdateTuple<R, P>(update, resultHandler, param));
        }
    }

    /**
     * Execute the updates
     * {@link #addServerModelUpdate(AbstractServerModelUpdate, UpdateResultHandler, Object) previously added}
     */
    public void executeUpdates() {
        synchronized (this) {
            if (status != Status.PENDING)
                throw new IllegalStateException("Status is " + status + " -- must be " + Status.PENDING);
            status = Status.ACTIVE;
            try {
                applyUpdates();
            } catch (Exception e) {
                handleRollback();
            }
        }
    }

    /**
     * Gets the current {@link Status}.
     *
     * @return the status. Will not return {@code null}
     */
    public Status getStatus() {
        return status;
    }

    /** Only invoke with the object monitor held */
    private void applyUpdates() {

        if (updates.size() == 0) {
            // No other event is going to trigger a transition from ACTIVE,
            // so we do it directly
            transition(false);
            return;
        }

        BatchBuilder batchBuilder = serviceContainer.batchBuilder();
        UpdateContext updateContext = new SimpleUpdateContext(serviceContainer, batchBuilder);
        for (ServerModelUpdateTuple<?,?> update : updates) {

            logger.debugf("Applying update %s", update.getUpdate().toString());

            if (status != Status.ACTIVE) {
                // Don't execute this update; just notify any handler and
                // count it as updated for overall completion tracking
                update.handleCancellation();
                continue;
            }

            boolean appliedToModel = false;
            ServerModelUpdateTuple<Object, ?> rollbackTuple = null;
            try {
                rollbackTuple = update.getRollbackTuple(serverModel);

                serverModel.update(update.getUpdate());
                appliedToModel = true;
                if (allowRuntimeUpdates) {
                    update.applyUpdate(updateContext);
                }
                else {
                    // We won't get a callback from a result handler, so
                    // directly record the completion for overall completion tracking
                    updateComplete();
                }

                // As the last thing in this try block, add the rollbackTuple
                // to the rollback list. Do it last because if this update
                // directly fails, we roll it back in catch block below.
                // The 'rollbacks' list is for updates that succeeeded.
                if (allowOverallRollback && rollbackTuple != null) {
                    // Add this latest update's rollback to the list
                    rollbacks.add(0, rollbackTuple);
                }
            }
            catch (Exception e) {
                update.handleFailure(e);

                if (rollbackTuple != null) {
                    if (appliedToModel) {
                        try {
                            if (allowRuntimeUpdates) {
                                // FIXME this is likely incorrect given we are now
                                // using a batch!!!
                                rollbackTuple.applyUpdate(updateContext);
                            }
                            serverModel.update(rollbackTuple.getUpdate());
                        } catch (UpdateFailedException e1) {
                            rollbackTuple.handleFailure(e1);
                        }
                    }
                }
                // else there was no compensating update or creating the
                // rollbackTuple failed at the beginning of 'try'
                // and there is nothing else needing to be done here
            }
        }

        if (status == Status.ACTIVE) {
            try {
                batchBuilder.install();
            } catch (ServiceRegistryException e) {
                handleRollback();
            }
        }

        if (logger.isDebugEnabled()) {
            logger.debugf("%s update(s) applied", updates.size());
        }
    }

    /** Only invoke with the object monitor held */
    private void applyRollbacks() {

        if (rollbacks.size() == 0) {
            // No other event is going to trigger a transition from ROLLING_BACK,
            // so we do it directly
            transition(false);
            return;
        }

        BatchBuilder batchBuilder = serviceContainer.batchBuilder();
        UpdateContext updateContext = new SimpleUpdateContext(serviceContainer, batchBuilder);
        boolean failed = false;
        try {
            for (ServerModelUpdateTuple<?, ?> update : rollbacks) {

                if (failed) {
                    update.handleCancellation();
                }
                else {
                    try {
                        serverModel.update(update.getUpdate());
                    }
                    catch (Exception e) {
                        update.handleFailure(e);
                    }
                    finally {
                        try {
                            if (allowRuntimeUpdates) {
                                update.applyUpdate(updateContext);
                            }
                            else {
                                rollbackComplete();
                            }
                        }
                        catch (Exception e) {
                            update.handleFailure(e);
                        }
                    }
                }
            }

            batchBuilder.install();

            if (logger.isDebugEnabled()) {
                logger.debugf("%s rollbacks applied", rollbacks.size());
            }
        }
        catch (Exception e) {
            // FIXME what to do
            logger.error("Caught exception applying rollbacks", e);
        }
    }

    private void updateComplete() {
        updatedCount.incrementAndGet();
        transition(false);
    }

    private void updateFailed() {
        updatedCount.incrementAndGet();
        transition(true);
    }

    private void rollbackComplete() {
        rolledBackCount.incrementAndGet();
        transition(false);
    }

    private void rollbackFailed() {
        rolledBackCount.incrementAndGet();
        transition(false);
    }

    private void handleCommit() {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                logger.debug("Committing");
                synchronized (ServerUpdateController.this) {
                    Status priorStatus = status;
                    status = Status.COMMITING;
                    commitHandler.handleUpdateCommit(ServerUpdateController.this, priorStatus);
                    Status newStatus = (priorStatus == Status.ACTIVE || allowOverallRollback) ? Status.COMMITTED : Status.ROLLED_BACK;
                    status = newStatus;
                }
                logger.debug("Committed");
            }
        };
        this.executor.execute(r);
    }

    private void handleRollback() {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                logger.debug("Rolling back");
                synchronized (ServerUpdateController.this) {
                    if (status != Status.MARKED_ROLLBACK) {
                        // Failure has already been handled by executeUpdate()
                        return;
                    }
                    status = Status.ROLLING_BACK;
                    applyRollbacks();
                }
            }
        };
        this.executor.execute(r);

    }

    private void transition(boolean updateFailure) {

        if (logger.isTraceEnabled()) {
            logger.tracef("transition(): status=%s updates.size()=%s updatedCount=%s rollbacks.size()=%s rolledBackCount=%s", status, updates.size(), updatedCount.get(), rollbacks.size(), rolledBackCount.get());
        }

        switch (status) {
            case ACTIVE: {
                if (updateFailure) {
                    status = Status.MARKED_ROLLBACK;
                }

                if (updateFailure && allowOverallRollback) {
                    handleRollback();
                }
                else if (updatedCount.get() == updates.size()) {
                    handleCommit();
                }
                break;
            }
            case MARKED_ROLLBACK: {
                if (rolledBackCount.get() == rollbacks.size()) {
                    // Getting here shouldn't really be possible since we
                    // shouldn't get rollback updates until RollbackTask switches
                    // us to Status.ROLLING_BACK
                    handleCommit();
                }
                break;
            }
            case ROLLING_BACK: {
                if (rolledBackCount.get() == rollbacks.size()) {
                    handleCommit();
                }
                break;
            }
            case ROLLED_BACK:
            case COMMITING:
            case COMMITTED:
                // TODO something spurious came in late. Log a WARN?
                break;
            default :
                throw new IllegalStateException("Unexpected status " + status);
        }

    }

    /** Data object that associates an update with its result handler and param */
    private class ServerModelUpdateTuple<R, P> {

        private final AbstractServerModelUpdate<R> update;
        private final DelegatingUpdateResultHandler<? super R, P> resultHandler;
        private final P param;

        public ServerModelUpdateTuple(final AbstractServerModelUpdate<R> update,
                final UpdateResultHandler<? super R, P> resultHandler,
                final P param) {
            if (update == null) {
                throw new IllegalArgumentException("update is null");
            }
            this.update = update;
            this.resultHandler = new DelegatingUpdateResultHandler<R, P>(resultHandler);
            this.param = param;
        }

        public AbstractServerModelUpdate<R> getUpdate() {
            return update;
        }

        private void handleCancellation() {
            resultHandler.handleCancellation(param);
        }

        private void handleFailure(Throwable cause) {
            resultHandler.handleFailure(cause, param);
        }

        private void applyUpdate(UpdateContext container) {
            update.applyUpdate(container, resultHandler, param);
        }

        private ServerModelUpdateTuple<Object, P> getRollbackTuple(ServerModel serverModel) {
            ServerModelUpdateTuple<Object, P> rollbackTuple = null;
            @SuppressWarnings("unchecked") // Safe because we aren't going to use the result
            AbstractServerModelUpdate<Object> compensating = (AbstractServerModelUpdate<Object>) update.getCompensatingUpdate(serverModel);
            if (compensating != null) {
                RollbackUpdateResultHandler<P> rollbackHandler = RollbackUpdateResultHandler.getRollbackUpdateResultHandler(resultHandler.getDelegate());
                rollbackTuple = new ServerModelUpdateTuple<Object, P>(compensating, rollbackHandler, param);
            }
            return rollbackTuple;
        }
    }

    /**
     * The actual result handler we pass to the applyUpdate method when we execute updates.
     * Uses the callback notifications to track the completion of the updates,
     * then passes the notifications on to any actual end-user handler that was passed in.
     */
    private class DelegatingUpdateResultHandler<R, P> implements UpdateResultHandler<R, P> {

        private final UpdateResultHandler<? super R, P> delegate;

        private DelegatingUpdateResultHandler(final UpdateResultHandler<? super R, P> delegate) {
            this.delegate = delegate;
        }

        @Override
        public void handleCancellation(P param) {
            updateComplete();
            if (delegate != null) {
                delegate.handleCancellation(param);
            }
        }

        @Override
        public void handleFailure(Throwable cause, P param) {
            updateFailed();
            if (delegate != null) {
                delegate.handleFailure(cause, param);
            }
            logger.errorf(cause, "Caught exception handling update (param is %s)", param);
        }

        @Override
        public void handleSuccess(R result, P param) {
            updateComplete();
            if (delegate != null) {
                delegate.handleSuccess(result, param);
            }
        }

        @Override
        public void handleTimeout(P param) {
            updateFailed();
            if (delegate != null) {
                delegate.handleTimeout(param);
            }
        }

        @Override
        public void handleRollbackFailure(Throwable cause, P param) {
            rollbackFailed();
            if (delegate != null) {
                delegate.handleRollbackFailure(cause, param);
            }
            logger.errorf(cause, "Caught exception handling rollback of an update (param is %s)", param);
        }

        @Override
        public void handleRollbackSuccess(P param) {
            rollbackComplete();
            if (delegate != null) {
                delegate.handleRollbackSuccess(param);
            }
        }

        @Override
        public void handleRollbackCancellation(P param) {
            rollbackFailed();
            if (delegate != null) {
                delegate.handleRollbackCancellation(param);
            }
        }

        @Override
        public void handleRollbackTimeout(P param) {
            rollbackFailed();
            if (delegate != null) {
                delegate.handleRollbackTimeout(param);
            }
        }

        private UpdateResultHandler<? super R, P> getDelegate() {
            return this.delegate;
        }
    }


}

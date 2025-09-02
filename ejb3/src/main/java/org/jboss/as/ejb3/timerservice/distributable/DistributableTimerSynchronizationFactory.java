/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.timerservice.distributable;

import java.util.function.Consumer;
import java.util.function.Supplier;

import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;

import org.jboss.as.ejb3.context.CurrentInvocationContext;
import org.jboss.as.ejb3.timerservice.spi.ManagedTimer;
import org.jboss.invocation.InterceptorContext;
import org.wildfly.clustering.cache.batch.Batch;
import org.wildfly.clustering.cache.batch.SuspendedBatch;
import org.wildfly.clustering.context.Context;
import org.wildfly.clustering.ejb.timer.Timer;

/**
 * Factory for creating {@link Synchronization} instances for a distributed timer service.
 * @author Paul Ferraro
 * @param <I> the timer identifier type
 */
public class DistributableTimerSynchronizationFactory<I> implements TimerSynchronizationFactory<I> {

    private final Consumer<Timer<I>> activateTask;
    private final Consumer<Timer<I>> cancelTask;

    public DistributableTimerSynchronizationFactory(Consumer<Timer<I>> activateTask, Consumer<Timer<I>> cancelTask) {
        this.activateTask = activateTask;
        this.cancelTask = cancelTask;
    }

    @Override
    public Consumer<Timer<I>> getActivateTask() {
        return this.activateTask;
    }

    @Override
    public Consumer<Timer<I>> getCancelTask() {
        return this.cancelTask;
    }

    @Override
    public Synchronization createActivateSynchronization(Timer<I> timer, Supplier<Batch> batchFactory, SuspendedBatch suspendedBatch) {
        return new DistributableTimerSynchronization<>(timer, batchFactory, suspendedBatch, this.activateTask, this.cancelTask);
    }

    @Override
    public Synchronization createCancelSynchronization(Timer<I> timer, Supplier<Batch> batchFactory, SuspendedBatch suspendedBatch) {
        return new DistributableTimerSynchronization<>(timer, batchFactory, suspendedBatch, this.cancelTask, this.activateTask);
    }

    private static class DistributableTimerSynchronization<I> implements Synchronization {

        private final Supplier<Batch> batchFactory;
        private final SuspendedBatch suspendedBatch;
        private final Timer<I> timer;
        private final Consumer<Timer<I>> commitTask;
        private final Consumer<Timer<I>> rollbackTask;

        DistributableTimerSynchronization(Timer<I> timer, Supplier<Batch> batchFactory, SuspendedBatch suspendedBatch, Consumer<Timer<I>> commitTask, Consumer<Timer<I>> rollbackTask) {
            this.timer = timer;
            this.batchFactory = batchFactory;
            this.suspendedBatch = suspendedBatch;
            this.commitTask = commitTask;
            this.rollbackTask = rollbackTask;
        }

        @Override
        public void beforeCompletion() {
            // Do nothing
        }

        @Override
        public void afterCompletion(int status) {
            InterceptorContext interceptorContext = CurrentInvocationContext.get();
            ManagedTimer currentTimer = (interceptorContext != null) ? (ManagedTimer) interceptorContext.getTimer() : null;

            try (Context<Batch> context = this.suspendedBatch.resumeWithContext()) {
                Supplier<Batch> batchFactory = ((currentTimer != null) && currentTimer.getId().equals(this.timer.getId().toString())) || !context.get().getStatus().isActive() ? this.batchFactory : context;
                try (Batch batch = batchFactory.get()) {
                    if (!this.timer.isCanceled()) {
                        if (status == Status.STATUS_COMMITTED) {
                            this.commitTask.accept(this.timer);
                        } else {
                            this.rollbackTask.accept(this.timer);
                        }
                    }
                }
            }
        }
    }
}

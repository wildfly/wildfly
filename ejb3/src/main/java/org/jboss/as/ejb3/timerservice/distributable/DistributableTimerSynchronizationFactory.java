/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.timerservice.distributable;

import java.util.function.Consumer;

import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;

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
    public Synchronization createActivateSynchronization(Timer<I> timer, SuspendedBatch suspendedBatch) {
        return new DistributableTimerSynchronization<>(timer, suspendedBatch, this.activateTask, this.cancelTask);
    }

    @Override
    public Synchronization createCancelSynchronization(Timer<I> timer, SuspendedBatch suspendedBatch) {
        return new DistributableTimerSynchronization<>(timer, suspendedBatch, this.cancelTask, this.activateTask);
    }

    private static class DistributableTimerSynchronization<I> implements Synchronization {

        private final Timer<I> timer;
        private final SuspendedBatch suspendedBatch;
        private final Consumer<Timer<I>> commitTask;
        private final Consumer<Timer<I>> rollbackTask;

        DistributableTimerSynchronization(Timer<I> timer, SuspendedBatch suspendedBatch, Consumer<Timer<I>> commitTask, Consumer<Timer<I>> rollbackTask) {
            this.timer = timer;
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
            try (Context<Batch> context = this.suspendedBatch.resumeWithContext()) {
                try (Batch batch = context.get()) {
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

/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.timerservice.distributable;

import java.util.function.Consumer;

import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;

import org.jboss.as.ejb3.context.CurrentInvocationContext;
import org.jboss.as.ejb3.timerservice.spi.ManagedTimer;
import org.jboss.invocation.InterceptorContext;
import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.ee.BatchContext;
import org.wildfly.clustering.ee.Batcher;
import org.wildfly.clustering.ee.Batch.State;
import org.wildfly.clustering.ejb.timer.Timer;
import org.wildfly.clustering.ejb.timer.TimerRegistry;

/**
 * @author Paul Ferraro
 */
public class DistributableTimerSynchronizationFactory<I> implements TimerSynchronizationFactory<I> {

    private final Consumer<Timer<I>> activateTask;
    private final Consumer<Timer<I>> cancelTask;

    public DistributableTimerSynchronizationFactory(TimerRegistry<I> registry) {
        this.activateTask = new Consumer<>() {
            @Override
            public void accept(Timer<I> timer) {
                timer.activate();
                registry.register(timer.getId());
            }
        };
        this.cancelTask = new Consumer<>() {
            @Override
            public void accept(Timer<I> timer) {
                registry.unregister(timer.getId());
                timer.cancel();
            }
        };
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
    public Synchronization createActivateSynchronization(Timer<I> timer, Batch batch, Batcher<Batch> batcher) {
        return new DistributableTimerSynchronization<>(timer, batch, batcher, this.activateTask, this.cancelTask);
    }

    @Override
    public Synchronization createCancelSynchronization(Timer<I> timer, Batch batch, Batcher<Batch> batcher) {
        return new DistributableTimerSynchronization<>(timer, batch, batcher, this.cancelTask, this.activateTask);
    }

    private static class DistributableTimerSynchronization<I> implements Synchronization {

        private final Batcher<Batch> batcher;
        private final Timer<I> timer;
        private final Batch batch;
        private final Consumer<Timer<I>> commitTask;
        private final Consumer<Timer<I>> rollbackTask;

        DistributableTimerSynchronization(Timer<I> timer, Batch batch, Batcher<Batch> batcher, Consumer<Timer<I>> commitTask, Consumer<Timer<I>> rollbackTask) {
            this.timer = timer;
            this.batch = batch;
            this.batcher = batcher;
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

            try (BatchContext context = this.batcher.resumeBatch(this.batch)) {
                try (Batch currentBatch = ((currentTimer != null) && currentTimer.getId().equals(this.timer.getId().toString())) || this.batch.getState() != State.ACTIVE ? this.batcher.createBatch() : this.batch) {
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

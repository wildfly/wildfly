/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.timerservice.distributable;

import java.util.function.Consumer;
import java.util.function.Supplier;

import jakarta.transaction.Synchronization;

import org.wildfly.clustering.cache.batch.Batch;
import org.wildfly.clustering.cache.batch.SuspendedBatch;
import org.wildfly.clustering.ejb.timer.Timer;

/**
 * Factory for creating {@link Synchronization} instances for a distributed timer service.
 * Used to defer timer activation or cancellation until an active transaction is committed.
 * @author Paul Ferraro
 * @param <I> the timer identifier type
 */
public interface TimerSynchronizationFactory<I> {

    Synchronization createActivateSynchronization(Timer<I> timer, Supplier<Batch> batchFactory, SuspendedBatch suspendedBatch);
    Synchronization createCancelSynchronization(Timer<I> timer, Supplier<Batch> batchFactory, SuspendedBatch suspendedBatch);

    Consumer<Timer<I>> getActivateTask();
    Consumer<Timer<I>> getCancelTask();
}

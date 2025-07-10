/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.timerservice.distributable;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;

import org.jboss.as.ejb3.component.EJBComponentUnavailableException;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.timerservice.spi.ManagedTimer;
import org.jboss.as.ejb3.timerservice.spi.TimedObjectInvoker;
import org.wildfly.clustering.cache.batch.Batch;
import org.wildfly.clustering.cache.batch.SuspendedBatch;
import org.wildfly.clustering.context.Context;
import org.wildfly.clustering.ejb.timer.TimeoutListener;
import org.wildfly.clustering.ejb.timer.Timer;
import org.wildfly.clustering.ejb.timer.TimerManager;

/**
 * Timeout listener implementation that invokes the relevant timeout callback method of an EJB.
 * @author Paul Ferraro
 * @param <I> the timer identifier type
 */
public class DistributableTimeoutListener<I> implements TimeoutListener<I> {

    private final TimedObjectInvoker invoker;
    private final TimerSynchronizationFactory<I> synchronizationFactory;

    public DistributableTimeoutListener(TimedObjectInvoker invoker, TimerSynchronizationFactory<I> synchronizationFactory) {
        this.invoker = invoker;
        this.synchronizationFactory = synchronizationFactory;
    }

    @Override
    public void timeout(TimerManager<I> manager, Timer<I> timer) throws ExecutionException {
        try (Batch batch = manager.getBatchFactory().get()) {
            try (Context<SuspendedBatch> context = batch.suspendWithContext()) {
                ManagedTimer managedTimer = new DistributableTimer<>(manager, timer, context.get(), this.invoker, this.synchronizationFactory);
                try {
                    invoke(managedTimer);
                } catch (ExecutionException e) {
                    EjbLogger.EJB3_TIMER_LOGGER.errorInvokeTimeout(managedTimer, e);
                    // Retry once on execution failure
                    EjbLogger.EJB3_TIMER_LOGGER.timerRetried(managedTimer);
                    invoke(managedTimer);
                }
            }
        }
    }

    private static void invoke(ManagedTimer timer) throws ExecutionException {
        try {
            timer.invoke();
        } catch (EJBComponentUnavailableException e) {
            throw new RejectedExecutionException(e);
        } catch (Throwable e) {
            throw new ExecutionException(e);
        }
    }
}

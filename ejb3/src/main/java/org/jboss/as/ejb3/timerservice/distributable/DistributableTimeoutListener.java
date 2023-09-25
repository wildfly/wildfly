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
import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.ee.Batcher;
import org.wildfly.clustering.ejb.timer.TimeoutListener;
import org.wildfly.clustering.ejb.timer.Timer;
import org.wildfly.clustering.ejb.timer.TimerManager;

/**
 * Timeout listener implementation that invokes the relevant timeout callback method of an EJB.
 * @author Paul Ferraro
 * @param <I> the timer identifier type
 */
public class DistributableTimeoutListener<I> implements TimeoutListener<I, Batch> {

    private final TimedObjectInvoker invoker;
    private final TimerSynchronizationFactory<I> synchronizationFactory;

    DistributableTimeoutListener(TimedObjectInvoker invoker, TimerSynchronizationFactory<I> synchronizationFactory) {
        this.invoker = invoker;
        this.synchronizationFactory = synchronizationFactory;
    }

    @Override
    public void timeout(TimerManager<I, Batch> manager, Timer<I> timer) throws ExecutionException {
        Batcher<Batch> batcher = manager.getBatcher();
        Batch suspendedBatch = batcher.suspendBatch();
        try {
            ManagedTimer managedTimer = new DistributableTimer<>(manager, timer, suspendedBatch, this.invoker, this.synchronizationFactory);
            try {
                invoke(managedTimer);
            } catch (ExecutionException e) {
                EjbLogger.EJB3_TIMER_LOGGER.errorInvokeTimeout(managedTimer, e);
                // Retry once on execution failure
                EjbLogger.EJB3_TIMER_LOGGER.timerRetried(managedTimer);
                invoke(managedTimer);
            }
        } finally {
            batcher.resumeBatch(suspendedBatch);
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

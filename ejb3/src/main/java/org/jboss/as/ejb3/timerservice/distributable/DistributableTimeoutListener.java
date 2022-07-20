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
 * @author Paul Ferraro
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

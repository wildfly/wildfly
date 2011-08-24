/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.ejb3.timerservice.mk2;

import org.jboss.as.ejb3.timerservice.mk2.persistence.TimerPersistence;
import org.jboss.as.ejb3.timerservice.spi.ScheduleTimer;
import org.jboss.as.ejb3.timerservice.spi.TimedObjectInvoker;
import org.jboss.as.ejb3.timerservice.spi.TimerServiceFactory;
import org.jboss.logging.Logger;

import javax.ejb.TimerService;
import javax.transaction.TransactionManager;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.ExecutorService;

/**
 * Implementation of {@link TimerServiceFactory}, responsible for
 * creating and managing MK2 timer services
 *
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 * @author Stuart Douglas
 * @version $Revision: $
 */
public class TimerServiceFactoryImpl implements TimerServiceFactory {
    /**
     * Logger
     */
    private static final Logger logger = Logger.getLogger(TimerServiceFactoryImpl.class);

    /**
     * Entity manager factory for JPA backed persistence
     */
    private final TimerPersistence timerPersistence;

    /**
     * Transaction manager for transaction management
     */
    private final TransactionManager transactionManager;

    /**
     * Exceutor service for creating the scheduled timer tasks
     */
    private final ExecutorService executor;

    /**
     * The timer to use
     */
    private final Timer timer;

    public TimerServiceFactoryImpl(final TimerPersistence timerPersistence, final TransactionManager transactionManager, final ExecutorService executor) {
        this.timerPersistence = timerPersistence;
        this.transactionManager = transactionManager;
        this.executor = executor;
        this.timer = new Timer("EJB Timer Thread", true);
    }

    /**
     * Creates a timer service for the passed <code>invoker</code>.
     * <p/>
     * <p>
     * This method also registers the created timer service, with the {@link TimerServiceRegistry}
     * </p>
     */
    public TimerService createTimerService(TimedObjectInvoker invoker) {
        // create the timer service
        TimerServiceImpl timerService = new TimerServiceImpl(timer, invoker, timerPersistence, transactionManager, executor);

        String timedObjectId = invoker.getTimedObjectId();
        // EJBTHREE-2209 I'm not too happy with this "fix". Ideally,
        // the TimerService should be registered irrespective of whether the deployment successfully undeploys
        // or fails during deployment. But unfortunately, the EJBContainer along with MC doesn't seem to be
        // doing it right. Fixing that is the ultimate solution, but due to lack of time for testing, let's
        // just unregister an already registered timerservice and log a WARN message, instead of failing with
        // "already registered error".
        if (TimerServiceRegistry.isRegistered(timedObjectId)) {
            TimerServiceRegistry.unregisterTimerService(timedObjectId);
            logger.warn("Unregistered an already registered Timerservice with id " + timedObjectId + " and a new instance will be registered");
        }
        // register this new created timer service in our registry
        TimerServiceRegistry.registerTimerService(timerService);
        return timerService;
    }

    /**
     * Restores the <code>timerService</code>.
     * <p/>
     * <p>
     * This involves restoring, any persisted, active timer instances
     * </p>
     * <p>
     * This method additionally registers (if it is not already registered)
     * the timer service with the {@link TimerServiceRegistry}
     * </p>
     */
    public void restoreTimerService(TimerService timerService, final List<ScheduleTimer> autoTimers) {
        TimerServiceImpl mk2TimerService = (TimerServiceImpl) timerService;
        String timedObjectId = mk2TimerService.getInvoker().getTimedObjectId();
        // if the timer service is not registered (maybe it was unregistered when it
        // was suspended) then register it with the timer service registry
        if (!TimerServiceRegistry.isRegistered(timedObjectId)) {
            TimerServiceRegistry.registerTimerService(mk2TimerService);
        }

        logger.debug("Restoring timerservice for timedObjectId: " + timedObjectId);
        // restore the timers
        mk2TimerService.restoreTimers(autoTimers);

    }

    /**
     * Suspends the <code>timerService</code>
     * <p/>
     * <p>
     * This involves suspending any scheduled timer tasks. Note that this method
     * does not <i>cancel</i> any timers. The timer will continue to stay active
     * although their <i>currently scheduled tasks</i> will be cancelled.
     * </p>
     * <p>
     * A suspended timer service (and the associated) timers can be restored by invoking
     * {@link #restoreTimerService(javax.ejb.TimerService, java.util.List)}
     * </p>
     * <p>
     * This method additionally unregisters the the timer service from the {@link TimerServiceRegistry}
     * </p>
     *
     * @see org.jboss.as.ejb3.timerservice.spi.TimerServiceFactory#suspendTimerService(javax.ejb.TimerService)
     */
    public void suspendTimerService(TimerService timerService) {
        TimerServiceImpl mk2TimerService = (TimerServiceImpl) timerService;
        try {
            logger.debug("Suspending timerservice for timedObjectId: " + mk2TimerService.getInvoker().getTimedObjectId());
            // suspend the timers
            mk2TimerService.suspendTimers();
        } finally {
            String timedObjectId = mk2TimerService.getInvoker().getTimedObjectId();
            // remove from our registry too
            if (TimerServiceRegistry.isRegistered(timedObjectId)) {
                TimerServiceRegistry.unregisterTimerService(timedObjectId);
            }
        }
    }

}

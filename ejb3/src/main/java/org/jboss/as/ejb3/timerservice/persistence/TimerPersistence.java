/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.ejb3.timerservice.persistence;

import java.io.Closeable;
import java.util.List;

import javax.transaction.TransactionManager;

import org.jboss.as.ejb3.timerservice.TimerImpl;
import org.jboss.as.ejb3.timerservice.TimerServiceImpl;
import org.jboss.msc.service.ServiceName;

/**
 * @author Stuart Douglas
 */
public interface TimerPersistence {

    ServiceName SERVICE_NAME = ServiceName.JBOSS.append("ejb3", "timerService", "timerPersistence");

    /**
     * Called when a timer is being persisted
     *
     * @param timer The timer
     */
    void addTimer(TimerImpl timer);

    /**
     * Called when a timer is being persisted
     *
     * @param timer The timer
     */
    void persistTimer(TimerImpl timer);

    /**
     * Invoked before running a timer in order to determine if this node should run the timer.
     * @param timer The timer
     * @param txManager ignored
     * @return true if the timer should be run
     */
    boolean shouldRun(TimerImpl timer, @Deprecated TransactionManager txManager);

    /**
     * Signals that a timer is being undeployed, and all cached data relating to this object should
     * be dropped to prevent a class loader leak
     *
     * @param timedObjectId
     */
    void timerUndeployed(String timedObjectId);

    /**
     * Load all active timers for the given object. If the object is an entity bean timers for all beans will be returned.
     *
     * @param timedObjectId The timed object id to load timers for
     * @return A list of all active timers
     */
    List<TimerImpl> loadActiveTimers(String timedObjectId, final TimerServiceImpl timerService);

    /**
     *
     * Registers a listener to listed for new timers that are added to the database.
     *
     * @param timedObjectId The timed object
     * @param listener The listener
     * @return A Closable that can be used to unregister the listener
     */
    Closeable registerChangeListener(String timedObjectId, final TimerChangeListener listener);

    /**
     * Listener that gets invoked when a new timer is added to the underlying store.
     */
    interface TimerChangeListener {

        /**
         * Invoked when a timer is added to the underlying store.
         * @param timer The timer
         */
        void timerAdded(TimerImpl timer);

        /**
         * Invoked when a timer is removed from the underlying store
         * @param timerId The timer
         */
        void timerRemoved(String timerId);

        /**
         * Gets the timer service associated with this listener
         *
         * @return The timer service
         */
        TimerServiceImpl getTimerService();

    }

}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.timerservice.persistence;

import java.io.Closeable;
import java.util.List;

import org.jboss.as.ejb3.timerservice.TimerImpl;
import org.jboss.as.ejb3.timerservice.TimerServiceImpl;
import org.jboss.msc.service.ServiceName;
import org.wildfly.service.descriptor.UnaryServiceDescriptor;

/**
 * @author Stuart Douglas
 */
public interface TimerPersistence {

    UnaryServiceDescriptor<TimerPersistence> SERVICE_DESCRIPTOR = UnaryServiceDescriptor.of("org.wildfly.ejb3.timer-service.timer-persistence-service", TimerPersistence.class);
    @Deprecated(forRemoval = true)
    ServiceName SERVICE_NAME = ServiceName.JBOSS.append("ejb3", "timerService", "timerPersistence");

    /**
     * Called when a timer is being persisted.
     * In a clustered environment, if an auto timer has already been persisted
     * by another concurrent node, it should not be persisted again, and its
     * state should be set to {@code CANCELED}.
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
     * @return true if the timer should be run
     */
    boolean shouldRun(TimerImpl timer);

    /**
     * Signals that the timer is being deployed and any internal structured required should be added.
     * @param timedObjectId
     */
    default void timerDeployed(String timedObjectId) {}

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
         * Invoked when a timer needs to be sync with the underlying store
         * @param oldTimer The timer in Server memory
         * @param newtimer The timer coming from the store
         */
        void timerSync(TimerImpl oldTimer, TimerImpl newTimer);

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

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.timer;

import java.lang.reflect.Method;
import java.util.stream.Stream;

import org.wildfly.clustering.server.manager.Manager;

/**
 * Manages creation, retrieval, and scheduling of timers.
 * @author Paul Ferraro
 * @param <I> the timer identifier type
 */
public interface TimerManager<I> extends Manager<I>, AutoCloseable {

    Timer<I> createTimer(I id, IntervalTimerConfiguration config, Object context);

    Timer<I> createTimer(I id, ScheduleTimerConfiguration config, Object context);

    Timer<I> createTimer(I id, ScheduleTimerConfiguration config, Object context, Method method, int index);

    /**
     * Returns exclusive access to the timer with the specified identifier.
     * @param id a timer identifier
     * @return the timer with the specified identifier, or null if no such timer exists
     */
    Timer<I> getTimer(I id);

    /**
     * Returns non-exclusive access to the timer with the specified identifier.
     * This should only be used to read fixed (i.e. non-dynamic) timer meta data, as this would otherwise result in a dirty read.
     * @param id a timer identifier
     * @return the timer with the specified identifier, or null if no such timer exists
     */
    default Timer<I> readTimer(I id) {
        return this.getTimer(id);
    }

    Stream<I> getActiveTimers();

    @Override
    void close();
}

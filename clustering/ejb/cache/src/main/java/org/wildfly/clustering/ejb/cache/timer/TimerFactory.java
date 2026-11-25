/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.timer;

import org.wildfly.clustering.ejb.timer.Timer;
import org.wildfly.clustering.ejb.timer.TimerManager;
import org.wildfly.clustering.server.scheduler.Scheduler;

import org.wildfly.clustering.ejb.timer.ImmutableTimerMetaData;
import org.wildfly.clustering.ejb.timer.TimeoutMetaData;

/**
 * @author Paul Ferraro
 * @param <I> the timer identifier type
 * @param <V> the timer metadata value type
 */
public interface TimerFactory<I, V> {

    TimerMetaDataFactory<I, V> getMetaDataFactory();

    Timer<I> createTimer(I id, ImmutableTimerMetaData metaData, TimerManager<I> manager, Scheduler<I, TimeoutMetaData> scheduler);
}

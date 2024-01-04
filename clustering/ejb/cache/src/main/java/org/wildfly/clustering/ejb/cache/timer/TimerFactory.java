/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.timer;

import org.wildfly.clustering.ejb.timer.Timer;
import org.wildfly.clustering.ejb.timer.TimerManager;
import org.wildfly.clustering.ee.Scheduler;
import org.wildfly.clustering.ee.cache.tx.TransactionBatch;
import org.wildfly.clustering.ejb.timer.ImmutableTimerMetaData;

/**
 * @author Paul Ferraro
 */
public interface TimerFactory<I, V, C> {

    TimerMetaDataFactory<I, V, C> getMetaDataFactory();

    Timer<I> createTimer(I id, ImmutableTimerMetaData metaData, TimerManager<I, TransactionBatch> manager, Scheduler<I, ImmutableTimerMetaData> scheduler);
}

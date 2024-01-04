/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.timer;

import org.wildfly.clustering.ee.Scheduler;
import org.wildfly.clustering.ee.cache.tx.TransactionBatch;
import org.wildfly.clustering.ejb.timer.Timer;
import org.wildfly.clustering.ejb.timer.TimerManager;
import org.wildfly.clustering.ejb.cache.timer.TimerFactory;
import org.wildfly.clustering.ejb.cache.timer.TimerMetaDataFactory;
import org.wildfly.clustering.ejb.timer.ImmutableTimerMetaData;
import org.wildfly.clustering.ejb.timer.TimeoutListener;
import org.wildfly.clustering.ejb.timer.TimerRegistry;

/**
 * @author Paul Ferraro
 */
public class InfinispanTimerFactory<I, V, C> implements TimerFactory<I, V, C> {

    private final TimerMetaDataFactory<I, V, C> factory;
    private final TimeoutListener<I, TransactionBatch> listener;
    private final TimerRegistry<I> registry;

    public InfinispanTimerFactory(TimerMetaDataFactory<I, V, C> factory, TimeoutListener<I, TransactionBatch> listener, TimerRegistry<I> registry) {
        this.factory = factory;
        this.listener = listener;
        this.registry = registry;
    }

    @Override
    public Timer<I> createTimer(I id, ImmutableTimerMetaData metaData, TimerManager<I, TransactionBatch> manager, Scheduler<I, ImmutableTimerMetaData> scheduler) {
        return new InfinispanTimer<>(manager, id, metaData, scheduler, this.listener, this.factory, this.registry);
    }

    @Override
    public TimerMetaDataFactory<I, V, C> getMetaDataFactory() {
        return this.factory;
    }
}

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

package org.wildfly.clustering.ejb.infinispan.timer;

import org.wildfly.clustering.ee.Scheduler;
import org.wildfly.clustering.ee.cache.tx.TransactionBatch;
import org.wildfly.clustering.ejb.timer.Timer;
import org.wildfly.clustering.ejb.timer.TimerManager;
import org.wildfly.clustering.ejb.timer.ImmutableTimerMetaData;
import org.wildfly.clustering.ejb.timer.TimeoutListener;
import org.wildfly.clustering.ejb.timer.TimerRegistry;

/**
 * @author Paul Ferraro
 */
public class InfinispanTimerFactory<I, V> implements TimerFactory<I, V> {

    private final TimerMetaDataFactory<I, V> factory;
    private final TimeoutListener<I, TransactionBatch> listener;
    private final TimerRegistry<I> registry;

    public InfinispanTimerFactory(TimerMetaDataFactory<I, V> factory, TimeoutListener<I, TransactionBatch> listener, TimerRegistry<I> registry) {
        this.factory = factory;
        this.listener = listener;
        this.registry = registry;
    }

    @Override
    public Timer<I> createTimer(I id, ImmutableTimerMetaData metaData, TimerManager<I, TransactionBatch> manager, Scheduler<I, ImmutableTimerMetaData> scheduler) {
        return new InfinispanTimer<>(manager, id, metaData, scheduler, this.listener, this.factory, this.registry);
    }

    @Override
    public TimerMetaDataFactory<I, V> getMetaDataFactory() {
        return this.factory;
    }
}

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

import org.wildfly.clustering.ejb.timer.Timer;
import org.wildfly.clustering.ejb.timer.TimerManager;
import org.wildfly.clustering.ejb.timer.TimerRegistry;
import org.wildfly.clustering.ee.Remover;
import org.wildfly.clustering.ee.Scheduler;
import org.wildfly.clustering.ee.cache.tx.TransactionBatch;
import org.wildfly.clustering.ejb.timer.ImmutableTimerMetaData;
import org.wildfly.clustering.ejb.timer.TimeoutListener;

/**
 * @author Paul Ferraro
 */
public class InfinispanTimer<I> implements Timer<I> {

    private final TimerManager<I, TransactionBatch> manager;
    private final I id;
    private final ImmutableTimerMetaData metaData;
    private final Scheduler<I, ImmutableTimerMetaData> scheduler;
    private final TimeoutListener<I, TransactionBatch> listener;
    private final Remover<I> remover;
    private final TimerRegistry<I> registry;

    private volatile boolean canceled = false;

    public InfinispanTimer(TimerManager<I, TransactionBatch> manager, I id, ImmutableTimerMetaData metaData, Scheduler<I, ImmutableTimerMetaData> scheduler, TimeoutListener<I, TransactionBatch> listener, Remover<I> remover, TimerRegistry<I> registry) {
        this.manager = manager;
        this.id = id;
        this.metaData = metaData;
        this.scheduler = scheduler;
        this.listener = listener;
        this.remover = remover;
        this.registry = registry;
    }

    @Override
    public I getId() {
        return this.id;
    }

    @Override
    public ImmutableTimerMetaData getMetaData() {
        return this.metaData;
    }

    @Override
    public boolean isActive() {
        return this.scheduler.contains(this.id);
    }

    @Override
    public boolean isCanceled() {
        return this.canceled;
    }

    @Override
    public void cancel() {
        this.suspend();
        this.remove();
        this.canceled = true;
    }

    private void remove() {
        this.registry.unregister(this.id);
        this.remover.remove(this.id);
    }

    @Override
    public void invoke() throws Exception {
        this.listener.timeout(this.manager, this);
    }

    @Override
    public void suspend() {
        this.scheduler.cancel(this.id);
    }

    @Override
    public void activate() {
        if (!this.isActive()) {
            this.scheduler.schedule(this.id, this.metaData);
        }
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof Timer)) return false;
        return this.id.equals(((Timer<?>) object).getId());
    }

    @Override
    public String toString() {
        return this.id.toString();
    }
}

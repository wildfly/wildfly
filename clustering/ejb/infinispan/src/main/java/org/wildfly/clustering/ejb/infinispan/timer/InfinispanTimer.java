/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.timer;

import java.util.concurrent.ExecutionException;

import org.wildfly.clustering.cache.CacheEntryRemover;
import org.wildfly.clustering.ejb.timer.ImmutableTimerMetaData;
import org.wildfly.clustering.ejb.timer.TimeoutListener;
import org.wildfly.clustering.ejb.timer.TimeoutMetaData;
import org.wildfly.clustering.ejb.timer.Timer;
import org.wildfly.clustering.ejb.timer.TimerManager;
import org.wildfly.clustering.server.scheduler.Scheduler;

/**
 * @author Paul Ferraro
 */
public class InfinispanTimer<I> implements Timer<I> {

    private final TimerManager<I> manager;
    private final I id;
    private final ImmutableTimerMetaData metaData;
    private final Scheduler<I, TimeoutMetaData> scheduler;
    private final TimeoutListener<I> listener;
    private final CacheEntryRemover<I> remover;

    private volatile boolean canceled = false;

    public InfinispanTimer(TimerManager<I> manager, I id, ImmutableTimerMetaData metaData, Scheduler<I, TimeoutMetaData> scheduler, TimeoutListener<I> listener, CacheEntryRemover<I> remover) {
        this.manager = manager;
        this.id = id;
        this.metaData = metaData;
        this.scheduler = scheduler;
        this.listener = listener;
        this.remover = remover;
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
        this.remover.remove(this.id);
    }

    @Override
    public void invoke() throws ExecutionException {
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

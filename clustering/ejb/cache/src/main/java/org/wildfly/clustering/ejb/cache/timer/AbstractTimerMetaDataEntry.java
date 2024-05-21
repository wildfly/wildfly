/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.timer;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.function.Supplier;

import org.wildfly.clustering.server.offset.Offset;
import org.wildfly.clustering.ejb.timer.TimerConfiguration;

/**
 * The base timer metadata cache entry.
 * @author Paul Ferraro
 * @param <C> the timer context type
 */
public abstract class AbstractTimerMetaDataEntry<C> implements RemappableTimerMetaDataEntry<C> {

    private final C context;
    private final Instant start;
    private volatile Duration lastTimeout = null;

    protected AbstractTimerMetaDataEntry(C context, TimerConfiguration config) {
        this(context, config.getStart().truncatedTo(ChronoUnit.MILLIS));
    }

    protected AbstractTimerMetaDataEntry(C context, Instant start) {
        this.context = context;
        this.start = start;
    }

    @Override
    public C getContext() {
        return this.context;
    }

    @Override
    public Instant getStart() {
        return this.start;
    }

    @Override
    public Duration getLastTimeout() {
        return this.lastTimeout;
    }

    @Override
    public void setLastTimeout(Duration timeout) {
        this.lastTimeout = timeout;
    }

    @Override
    public RemappableTimerMetaDataEntry<C> remap(Supplier<Offset<Duration>> lastTimeoutOffset) {
        RemappableTimerMetaDataEntry<C> entry = this.clone();
        entry.setLastTimeout(Optional.ofNullable(this.getLastTimeout()).map(lastTimeoutOffset.get()).orElse(Duration.ZERO));
        return entry;
    }

    @Override
    protected abstract RemappableTimerMetaDataEntry<C> clone();
}

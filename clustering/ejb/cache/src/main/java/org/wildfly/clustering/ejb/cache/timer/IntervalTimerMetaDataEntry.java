/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.timer;

import java.time.Duration;
import java.time.Instant;

import org.wildfly.clustering.ejb.timer.IntervalTimerConfiguration;

/**
 * An interval-based timer metadata cache entry.
 * @param <C> the timer context type
 * @author Paul Ferraro
 * @param <C> the timer metadata context type
 */
public class IntervalTimerMetaDataEntry<C> extends AbstractTimerMetaDataEntry<C> implements ImmutableIntervalTimerMetaDataEntry<C> {

    private final Duration interval;

    public IntervalTimerMetaDataEntry(C context, IntervalTimerConfiguration config) {
        super(context, config);
        this.interval = config.getInterval();
    }

    public IntervalTimerMetaDataEntry(C context, Instant start, Duration interval) {
        super(context, start);
        this.interval = interval;
    }

    @Override
    public Duration getInterval() {
        return this.interval;
    }

    @Override
    public Instant apply(Instant lastTimeout) {
        return (this.interval != null) ? lastTimeout.plus(this.interval) : null;
    }

    @Override
    protected RemappableTimerMetaDataEntry<C> clone() {
        return new IntervalTimerMetaDataEntry<>(this.getContext(), this.getStart(), this.interval);
    }
}

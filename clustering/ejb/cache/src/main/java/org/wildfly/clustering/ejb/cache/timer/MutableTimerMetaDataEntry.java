/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.timer;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Predicate;

import org.wildfly.clustering.server.offset.OffsetValue;
import org.wildfly.clustering.ejb.timer.TimerType;

/**
 * @author Paul Ferraro
 * @param <C> the timer context type
 */
public class MutableTimerMetaDataEntry<C> implements TimerMetaDataEntry<C> {

    private final ImmutableTimerMetaDataEntry<C> entry;
    private final OffsetValue<Duration> lastTimeout;

    public MutableTimerMetaDataEntry(ImmutableTimerMetaDataEntry<C> entry, OffsetValue<Duration> lastTimeout) {
        this.entry = entry;
        this.lastTimeout = lastTimeout;
    }

    @Override
    public TimerType getType() {
        return this.entry.getType();
    }

    @Override
    public C getContext() {
        return this.entry.getContext();
    }

    @Override
    public Instant getStart() {
        return this.entry.getStart();
    }

    @Override
    public Predicate<Method> getTimeoutMatcher() {
        return this.entry.getTimeoutMatcher();
    }

    @Override
    public Duration getLastTimeout() {
        return this.lastTimeout.get();
    }

    @Override
    public void setLastTimeout(Duration timeout) {
        this.lastTimeout.set(timeout);
    }

    @Override
    public Instant apply(Instant instant) {
        return this.entry.apply(instant);
    }
}

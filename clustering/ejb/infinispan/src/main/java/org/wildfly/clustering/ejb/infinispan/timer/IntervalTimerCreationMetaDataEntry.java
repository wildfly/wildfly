/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.timer;

import java.time.Duration;
import java.time.Instant;

import org.wildfly.clustering.ejb.timer.IntervalTimerConfiguration;

/**
 * @author Paul Ferraro
 */
public class IntervalTimerCreationMetaDataEntry<V> extends AbstractTimerCreationMetaDataEntry<V> implements IntervalTimerCreationMetaData<V> {

    private final Duration interval;

    public IntervalTimerCreationMetaDataEntry(V context, IntervalTimerConfiguration config) {
        this(context, config.getStart(), config.getInterval());
    }

    public IntervalTimerCreationMetaDataEntry(V context, Instant start, Duration interval) {
        super(context, start);
        this.interval = interval;
    }

    @Override
    public Duration getInterval() {
        return this.interval;
    }

    @Override
    public Instant apply(Instant lastTimeout) {
        return (lastTimeout == null) ? this.getStart() : (this.interval != null) ? lastTimeout.plus(this.interval) : null;
    }
}

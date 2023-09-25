/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.timer;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.wildfly.clustering.ejb.timer.TimerConfiguration;

/**
 * @author Paul Ferraro
 */
public abstract class AbstractTimerCreationMetaDataEntry<V> implements TimerCreationMetaData<V> {

    private final V context;
    private final Instant start;

    protected AbstractTimerCreationMetaDataEntry(V context, TimerConfiguration config) {
        this(context, config.getStart());
    }

    protected AbstractTimerCreationMetaDataEntry(V context, Instant start) {
        this.context = context;
        this.start = (start != null) ? start.truncatedTo(ChronoUnit.MILLIS) : null;
    }

    @Override
    public V getContext() {
        return this.context;
    }

    @Override
    public Instant getStart() {
        return this.start;
    }
}

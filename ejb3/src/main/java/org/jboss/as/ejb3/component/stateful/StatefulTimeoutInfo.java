/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.component.stateful;

import jakarta.ejb.StatefulTimeout;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * @author Stuart Douglas
 */
public class StatefulTimeoutInfo implements Supplier<Duration> {

    private final long value;
    private final TimeUnit timeUnit;

    public StatefulTimeoutInfo(final long value, final TimeUnit timeUnit) {
        this.value = value;
        this.timeUnit = timeUnit;
    }

    public StatefulTimeoutInfo(final StatefulTimeout statefulTimeout) {
        this.value = statefulTimeout.value();
        this.timeUnit = statefulTimeout.unit();
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    public long getValue() {
        return value;
    }

    @Override
    public Duration get() {
        return (this.value >= 0) ? Duration.of(this.value, this.timeUnit.toChronoUnit()) : null;
    }
}

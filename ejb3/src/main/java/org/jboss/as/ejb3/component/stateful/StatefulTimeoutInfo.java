/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.component.stateful;

import jakarta.ejb.StatefulTimeout;
import java.util.concurrent.TimeUnit;

/**
 * @author Stuart Douglas
 */
public class StatefulTimeoutInfo {

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
}

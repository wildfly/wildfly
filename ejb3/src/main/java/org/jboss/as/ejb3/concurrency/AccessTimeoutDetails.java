/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.concurrency;

import java.util.concurrent.TimeUnit;

/**
 * @author Stuart Douglas
 */
public class AccessTimeoutDetails {

    private final long value;

    private final TimeUnit timeUnit;

    public AccessTimeoutDetails(final long value, final TimeUnit timeUnit) {
        this.value = value;
        this.timeUnit = timeUnit;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    public long getValue() {
        return value;
    }
}

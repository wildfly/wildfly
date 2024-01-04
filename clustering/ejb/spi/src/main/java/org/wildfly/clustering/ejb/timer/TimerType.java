/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.timer;

/**
 * Enumerates the supported timer types.
 * @author Paul Ferraro
 */
public enum TimerType {

    INTERVAL(false),
    SCHEDULE(true),
    ;
    private final boolean calendar;

    TimerType(boolean calendar) {
        this.calendar = calendar;
    }

    public boolean isCalendar() {
        return this.calendar;
    }
}

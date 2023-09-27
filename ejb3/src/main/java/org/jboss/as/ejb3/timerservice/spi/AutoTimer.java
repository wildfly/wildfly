/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.timerservice.spi;

import java.lang.reflect.Method;
import jakarta.ejb.ScheduleExpression;
import jakarta.ejb.TimerConfig;

/**
 * Holds data about an automatic timer
 * @author Stuart Douglas
 */
public final class AutoTimer {
    private final ScheduleExpression scheduleExpression;
    private final TimerConfig timerConfig;
    private Method method;

    public AutoTimer() {
        scheduleExpression = new ScheduleExpression();
        timerConfig = new TimerConfig();
    }

    public AutoTimer(final ScheduleExpression scheduleExpression, final TimerConfig timerConfig, final Method method) {
        this.scheduleExpression = scheduleExpression;
        this.timerConfig = timerConfig;
        this.method = method;
    }

    public ScheduleExpression getScheduleExpression() {
        return scheduleExpression;
    }

    public TimerConfig getTimerConfig() {
        return timerConfig;
    }

    public Method getMethod() {
        return method;
    }
}

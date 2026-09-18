/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.ejb.timer.beans;

import java.util.function.Function;

import jakarta.ejb.NoSuchObjectLocalException;
import jakarta.ejb.Timeout;
import jakarta.ejb.Timer;
import jakarta.ejb.TimerService;

/**
 * @author Paul Ferraro
 */
public abstract class AbstractManualTimerBean extends AbstractTimerBean implements ManualTimerBean {

    private final Function<TimerService, Timer> factory;

    protected AbstractManualTimerBean(Function<TimerService, Timer> factory) {
        this.factory = factory;
    }

    @Override
    public void createTimer() {
        this.factory.apply(this.service);
        java.util.Collection<Timer> timers = this.service.getTimers();
        assert timers.size() == 1 : timers.toString();
    }

    @Override
    public void cancel() {
        for (Timer timer : this.service.getTimers()) {
            try {
                timer.cancel();
            } catch (NoSuchObjectLocalException e) {
                // This can happen if timer expired concurrently
                // In this case, verify that a fresh call to getTimers() no longer returns it.
                if (this.service.getTimers().contains(timer)) {
                    throw e;
                }
            }
        }
    }

    @Timeout
    public void timeout(Timer timer) {
        this.record(timer);
    }
}

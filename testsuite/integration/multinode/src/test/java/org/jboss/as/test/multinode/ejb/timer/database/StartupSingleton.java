/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.multinode.ejb.timer.database;

import java.io.Serializable;
import java.util.Collection;
import jakarta.annotation.Resource;
import jakarta.ejb.ConcurrencyManagement;
import jakarta.ejb.ConcurrencyManagementType;
import jakarta.ejb.Schedule;
import jakarta.ejb.Schedules;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.ejb.Timer;
import jakarta.ejb.TimerService;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;

@Singleton
@Startup
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@TransactionManagement(TransactionManagementType.BEAN)
public class StartupSingleton implements RemoteTimedBean {
    @Resource
    private TimerService timerService;

    @Override
    public void scheduleTimer(final long date, final String info) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasTimerRun() {
        final Collection<Timer> timers = timerService.getTimers();
        if (timers.size() != 2) {
            throw new IllegalStateException("Expected 2 timers for StartupSingleton, but got " + timers.size());
        }
        for (Timer t : timers) {
            final Serializable info = t.getInfo();
            if (!info.equals("ZERO") && !info.equals("ONE")) {
                throw new IllegalStateException("Unexpected timer info: " + info);
            }
        }
        return true;
    }

    @Schedules({
    @Schedule(dayOfMonth = "*", second = "*", minute = "*", hour = "*", year="9999", persistent = true, info = "ZERO", timezone = "America/New_York")
    })
    private void schedule0() {
    }

    @Schedules({
    @Schedule(dayOfMonth = "*", second = "*", minute = "*", hour = "*", year="9999", persistent = true, info = "ONE")
    })
    private void schedule1(Timer timer) {
    }

}

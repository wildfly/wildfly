/*
 * JBoss, Home of Professional Open Source
 * Copyright 2021, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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

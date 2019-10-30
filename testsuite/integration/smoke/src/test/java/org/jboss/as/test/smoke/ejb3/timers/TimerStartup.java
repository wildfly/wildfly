/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

package org.jboss.as.test.smoke.ejb3.timers;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.ScheduleExpression;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@Startup
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class TimerStartup {
    private static final Logger LOG = LoggerFactory.getLogger(TimerStartup.class);
    private static final String TIMER_NAME = TimerStartup.class.getName() + "-T1";
    private static final String TIMER_CANCEL_NAME = TIMER_NAME + "-CANCEL";

    @Resource
    TimerService ts;

    @Timeout
    @Lock(LockType.READ)
    public void timeout(Timer t) {
        LOG.info("executing timer {}", t);
    }

    @PostConstruct
    public void init() {
        Timer normalTimer = createPersistenceTimer(TIMER_NAME, new ScheduleExpression().hour(1));
        Timer cancelTimer = createPersistenceTimer(TIMER_CANCEL_NAME, new ScheduleExpression().hour(2));

        // Then cancelling it
        cancelTimer.cancel();
    }

    private Timer createPersistenceTimer(String timerName, ScheduleExpression schedule) {
        // Let's create a persistent timer configuration
        TimerConfig persistentTimerConfiguration = new TimerConfig(timerName, true);

        // Creating it
        return ts.createCalendarTimer(schedule, persistentTimerConfiguration);
    }

    @PreDestroy
    public void clearTimers() {
        for (Timer t: ts.getAllTimers()) {
            t.cancel();
        }
    }
}

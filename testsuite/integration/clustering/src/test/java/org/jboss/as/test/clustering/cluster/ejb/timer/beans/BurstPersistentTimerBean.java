/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2024, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.clustering.cluster.ejb.timer.beans;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import jakarta.ejb.Local;
import jakarta.ejb.ScheduleExpression;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.ejb.TimerConfig;

/**
 * @author Paul Ferraro
 */
@Singleton
@Startup
@Local(ManualTimerBean.class)
public class BurstPersistentTimerBean extends AbstractManualTimerBean {
    public static final Duration START_DURATION = Duration.ofSeconds(5);
    public static final Duration BURST_DURATION = Duration.ofSeconds(5);

    public BurstPersistentTimerBean() {
        // Fire every second for 5 seconds after waiting 10 seconds
        super(service -> {
            Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
            Instant start = now.plus(START_DURATION);
            Instant end = start.plus(BURST_DURATION);
            return service.createCalendarTimer(new ScheduleExpression().start(Date.from(start)).end(Date.from(end)).hour("*").minute("*").second("*"), new TimerConfig(BurstPersistentTimerBean.class.getName(), true));
        });
    }
}

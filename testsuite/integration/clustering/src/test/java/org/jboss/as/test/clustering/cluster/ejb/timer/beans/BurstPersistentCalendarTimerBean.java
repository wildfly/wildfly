/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.cluster.ejb.timer.beans;

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
public class BurstPersistentCalendarTimerBean extends BurstPersistentTimerBean {

    public BurstPersistentCalendarTimerBean() {
        // Fire every second for 5 seconds after waiting 5 seconds
        super((service, entry) -> {
            return service.createCalendarTimer(new ScheduleExpression().start(Date.from(entry.getKey())).end(Date.from(entry.getValue())).hour("*").minute("*").second("*"), new TimerConfig(BurstPersistentCalendarTimerBean.class.getName(), true));
        });
    }
}

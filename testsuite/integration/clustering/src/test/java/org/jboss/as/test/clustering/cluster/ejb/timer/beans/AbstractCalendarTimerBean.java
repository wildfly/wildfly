/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.ejb.timer.beans;

import jakarta.ejb.ScheduleExpression;
import jakarta.ejb.TimerConfig;

/**
 * @author Paul Ferraro
 */
public class AbstractCalendarTimerBean extends AbstractManualTimerBean {

    public AbstractCalendarTimerBean(boolean persistent) {
        super(service -> service.createCalendarTimer(new ScheduleExpression().second("*").minute("*").hour("*").dayOfMonth("*").year("*"), new TimerConfig("calendar", persistent)));
    }
}

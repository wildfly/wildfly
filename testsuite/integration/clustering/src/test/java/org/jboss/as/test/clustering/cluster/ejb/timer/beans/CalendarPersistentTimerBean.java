/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.cluster.ejb.timer.beans;

import jakarta.ejb.Local;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;

/**
 * @author Paul Ferraro
 */
@Singleton
@Startup
@Local(ManualTimerBean.class)
public class CalendarPersistentTimerBean extends AbstractCalendarTimerBean {

    public CalendarPersistentTimerBean() {
        super(true);
    }
}

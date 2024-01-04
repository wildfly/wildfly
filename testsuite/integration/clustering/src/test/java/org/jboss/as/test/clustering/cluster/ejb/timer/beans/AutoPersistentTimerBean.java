/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.ejb.timer.beans;

import jakarta.ejb.Local;
import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.ejb.Timer;

/**
 * @author Paul Ferraro
 */
@Singleton
@Startup
@Local(TimerBean.class)
public class AutoPersistentTimerBean extends AbstractTimerBean implements AutoTimerBean {

    @Override
    @Schedule(year = "*", month = "*", dayOfMonth = "*", hour = "*", minute = "*", second = "*", info = "auto", persistent = true)
    public void timeout(Timer timer) {
        this.record(timer);
    }
}

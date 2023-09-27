/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.interceptor.serverside;

import jakarta.ejb.Local;
import jakarta.ejb.Schedule;
import jakarta.ejb.Stateless;
import jakarta.ejb.Timer;

/**
 * @author <a href="mailto:szhantem@redhat.com">Sultan Zhantemirov</a> (c) 2019 Red Hat, inc.
 */
@Stateless
@Local
public class ScheduleBean {
    private static String timerInfo;

    public String getTimerInfo() {
        return timerInfo;
    }

    @Schedule(second="0/2", minute = "*", hour = "*", info = "info")
    public void timeout(Timer timer) {
        timerInfo = (String) timer.getInfo();
    }

}

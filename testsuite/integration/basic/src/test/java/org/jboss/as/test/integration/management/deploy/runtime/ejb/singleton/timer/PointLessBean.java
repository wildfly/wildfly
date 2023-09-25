/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.management.deploy.runtime.ejb.singleton.timer;

import jakarta.annotation.Resource;
import jakarta.ejb.Singleton;
import jakarta.ejb.Timeout;
import jakarta.ejb.Timer;
import jakarta.ejb.TimerConfig;
import jakarta.ejb.TimerService;

/**
 * @author baranowb
 */
@Singleton(name = "POINT")
public class PointLessBean implements PointlessInterface {

    private static final TimerConfig TIMER_CONFIG = new TimerConfig("Eye Candy", true);

    private int count = 0;
    @Resource
    TimerService timerService;

    @Override
    public void triggerTimer() throws Exception {
        count = 0;
        timerService.createSingleActionTimer(100, TIMER_CONFIG);

    }

    @Override
    public int getTimerCount() {
        return count;
    }

    @Timeout
    public void timeout(Timer timer) {
        count++;
    }

}

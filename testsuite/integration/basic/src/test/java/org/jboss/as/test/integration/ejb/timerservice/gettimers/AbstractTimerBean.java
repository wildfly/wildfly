/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.timerservice.gettimers;

import java.util.Collection;

import jakarta.annotation.Resource;
import jakarta.ejb.Timeout;
import jakarta.ejb.Timer;
import jakarta.ejb.TimerService;

import org.jboss.logging.Logger;

/**
 * @author Tomas Hofman (thofman@redhat.com)
 */
public abstract class AbstractTimerBean {

    public static final int NUMBER_OF_TIMERS = 5;

    private Logger logger = Logger.getLogger(getClass());

    @Resource
    private TimerService timerService;

    public void startTimers() {
        logger.trace("Initially had these timers:");
        for (Timer timer: timerService.getTimers()) {
            logger.trace("   " + timer.getInfo());
        }
        for (int i = 0; i < NUMBER_OF_TIMERS; i++) {
            String name = getClass().getSimpleName() + "#" + i;
            logger.debugf("Starting timer %s", name);
            timerService.createTimer(100000, 100000, name); // doesn't really need any timeouts to happen
        }
    }

    public Collection<Timer> getTimers() {
        return timerService.getTimers();
    }

    public void stopTimers() {
        logger.debug("Stopping all timers.");
        for (Timer timer: timerService.getTimers()) {
            logger.debugf("Stopping timer %s.", timer.getInfo().toString());
            timer.cancel();
        }
    }

    @Timeout
    public void timeout(Timer timer) {
        logger.infof("Timeout %s", timer.getInfo());
    }

}

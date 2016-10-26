/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat Middleware LLC, and individual contributors
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
 *
 */

package org.jboss.as.test.integration.ejb.timerservice.gettimers;

import java.util.Collection;

import javax.annotation.Resource;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerService;

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
            logger.infof("Starting timer %s", name);
            timerService.createTimer(100000, 100000, name); // doesn't really need any timeouts to happen
        }
    }

    public Collection<Timer> getTimers() {
        return timerService.getTimers();
    }

    public void stopTimers() {
        logger.infof("Stopping all timers.");
        for (Timer timer: timerService.getTimers()) {
            logger.infof("Stopping timer %s.", timer.getInfo().toString());
            timer.cancel();
        }
    }

    @Timeout
    public void timeout(Timer timer) {
        logger.infof("Timeout %s", timer.getInfo());
    }

}

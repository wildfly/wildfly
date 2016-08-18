/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.management.deploy.runtime.ejb.singleton.timer;

import java.util.concurrent.TimeUnit;
import javax.annotation.Resource;
import javax.ejb.Singleton;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;

/**
 * @author baranowb
 */
@Singleton(name = "POINT")
public class PointLessBean implements PointlessInterface {

    private static final int WAIT_TIME = 1;
    private static final TimeUnit WAIT_UNIT = TimeUnit.SECONDS;
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

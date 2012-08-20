/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.test.integration.ejb.timerservice.schedule.descriptor;

import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.ejb.Timer;

/**
 * Ejb with it's timers managed by a descriptor
 *
 * @author Stuart Douglas
 */
public class DescriptorScheduleBean {

    private static volatile String timerInfo;
    private static volatile Date start;

    private static int TIMER_CALL_WAITING_S = 30;
    private static volatile CountDownLatch latch = new CountDownLatch(1);

    public void descriptorScheduledMethod(final Timer timer) {
        timerInfo = (String) timer.getInfo();
        start = timer.getSchedule().getStart();
        latch.countDown();
    }

    public static boolean awaitTimer(){
        try {
            final boolean success = latch.await(TIMER_CALL_WAITING_S, TimeUnit.SECONDS);
            if (!success)
                throw new IllegalStateException("Timeout method was not called");
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return timerInfo != null;
    }

    public static Date getStart() {
        return start;
    }

    public static String getTimerInfo() {
        return timerInfo;
    }
}

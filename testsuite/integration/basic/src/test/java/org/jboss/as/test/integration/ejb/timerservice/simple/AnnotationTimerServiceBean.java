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
package org.jboss.as.test.integration.ejb.timerservice.simple;

import javax.annotation.Resource;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerService;
import com.kenai.jaffl.annotations.Synchronized;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author Stuart Douglas
 */
@Stateless
public class AnnotationTimerServiceBean {
    private static CountDownLatch latch = new CountDownLatch(1);

    private static boolean timerServiceCalled = false;
    
    private String timerInfo;
    private boolean isPersistent;
    private boolean isCalendar;

    @Resource
    private SessionContext sessionContext;
    
    private TimerService timerService;

    public synchronized TimerService getTimerService() {
        if(timerService == null) {
            timerService = (TimerService) sessionContext.lookup("java:comp/TimerService"); 
        }
        return timerService;
    }
    
    public void resetTimerServiceCalled() {
        timerServiceCalled = false;
        latch = new CountDownLatch(1);
    }
    
    public String getTimerInfo() {
        return timerInfo;
    }
    public boolean isPersistent() {
        return isPersistent;
    }
    public boolean isCalendar() {
        return isCalendar;
    }

    @Timeout
    public void timeout(Timer timer) {
        timerInfo = (String) timer.getInfo();
        isPersistent = timer.isPersistent();
        isCalendar = timer.isCalendarTimer();
        
        timerServiceCalled = true;
        latch.countDown();
    }

    public static boolean awaitTimerCall() {
        try {
            latch.await(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return timerServiceCalled;
    }

}

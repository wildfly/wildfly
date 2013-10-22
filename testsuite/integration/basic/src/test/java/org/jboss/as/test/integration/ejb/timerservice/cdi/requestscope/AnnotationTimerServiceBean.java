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
package org.jboss.as.test.integration.ejb.timerservice.cdi.requestscope;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.ejb.Timeout;
import javax.ejb.TimerService;
import javax.inject.Inject;

/**
 * @author Stuart Douglas
 */
@Stateless
public class AnnotationTimerServiceBean {

    private static final int TIMER_TIMEOUT_TIME_MS = 100;
    // has to be greater than timeout time
    private static final int TIMER_CALL_WAITING_MS = 30000;
    private static final CountDownLatch latch = new CountDownLatch(1);

    private static volatile String cdiMessage = null;

    @Inject
    private CdiBean cdiBean;

    @Resource
    private TimerService timerService;

    public void createTimer() {
        timerService.createTimer(TIMER_TIMEOUT_TIME_MS, null);
    }

    @Timeout
    public void timeout() {
        cdiMessage = cdiBean.getMessage();
        latch.countDown();
    }

    public static String awaitTimerCall() {
        try {
            latch.await(TIMER_CALL_WAITING_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return cdiMessage;
    }

}

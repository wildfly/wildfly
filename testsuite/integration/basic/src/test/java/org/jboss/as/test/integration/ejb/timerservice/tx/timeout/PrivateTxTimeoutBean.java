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
package org.jboss.as.test.integration.ejb.timerservice.tx.timeout;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;
import javax.ejb.Remote;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;

import org.jboss.ejb3.annotation.TransactionTimeout;

/**
 * @author Tomasz Adamski
 */

@Stateless
@Remote(TimeoutBeanRemoteView.class)
public class PrivateTxTimeoutBean extends AbstractTxBean implements TimeoutBeanRemoteView {

    private static final int TIMER_CALL_WAITING_S = 30;
    private static final int DURATION = 100;

    private static volatile CountDownLatch latch = new CountDownLatch(1);

    private static volatile boolean timerServiceCalled = false;
    private static volatile int timeout = 0;

    @Resource
    private SessionContext sessionContext;

    @Resource
    private TimerService timerService;

    @Override
    public void startTimer() {
        timerService.createSingleActionTimer(DURATION, new TimerConfig());
    }

    @Timeout
    @TransactionTimeout(value = 5)
    private void timeout(final Timer timer) {
        timeout = checkTimeoutValue();
        timerServiceCalled = true;
        latch.countDown();
    }

    public static boolean awaitTimerCall() {
        try {
            latch.await(TIMER_CALL_WAITING_S, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return timerServiceCalled;
    }

    public static int getTimeout(){
        return timeout;
    }
}

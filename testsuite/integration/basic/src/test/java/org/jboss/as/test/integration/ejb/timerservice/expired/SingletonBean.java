/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ejb.timerservice.expired;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.annotation.Resource;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.NoMoreTimeoutsException;
import javax.ejb.NoSuchObjectLocalException;
import javax.ejb.Singleton;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;

import org.jboss.logging.Logger;

/**
 * @author Jaikiran Pai
 */
@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class SingletonBean {

    private static final Logger log = Logger.getLogger(SingletonBean.class);
    private static int TIMER_CALL_WAITING_S = 5;

    @Resource
    private TimerService timerService;

    private Timer timer;

    private CountDownLatch timeoutNotifyingLatch;
    private CountDownLatch timeoutWaiter;

    public void createSingleActionTimer(final long delay, final TimerConfig config,
                                        CountDownLatch timeoutNotifyingLatch, CountDownLatch timeoutWaiter) {
        this.timer = this.timerService.createSingleActionTimer(delay, config);
        this.timeoutNotifyingLatch = timeoutNotifyingLatch;
        this.timeoutWaiter = timeoutWaiter;
    }

    @Timeout
    private void onTimeout(final Timer timer) throws InterruptedException {
        log.trace("Timeout invoked for " + this + " on timer " + timer);
        this.timeoutNotifyingLatch.countDown();
        log.debug("Waiting for timer will be permitted to continue");
        this.timeoutWaiter.await(TIMER_CALL_WAITING_S, TimeUnit.SECONDS);
        log.debug("End of onTimeout on singleton");
    }

    public Timer getTimer() {
        return this.timer;
    }

    public void invokeTimeRemaining() throws NoMoreTimeoutsException, NoSuchObjectLocalException {
        this.timer.getTimeRemaining();
    }

    public void invokeGetNext() throws NoMoreTimeoutsException, NoSuchObjectLocalException {
        this.timer.getNextTimeout();
    }


}

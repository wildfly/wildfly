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

import org.jboss.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.NoSuchObjectLocalException;
import javax.ejb.Singleton;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import java.util.concurrent.CountDownLatch;

/**
 * @author Jaikiran Pai
 */
@Singleton
public class SingletonBean {

    private static final Logger logger = Logger.getLogger(SingletonBean.class);

    @Resource
    private TimerService timerService;

    private Timer timer;

    private CountDownLatch timeoutNotifyingLatch;

    public void createSingleActionTimer(final long delay, final TimerConfig config, final CountDownLatch timeoutNotifyingLatch) {
        this.timer = this.timerService.createSingleActionTimer(delay, config);
        this.timeoutNotifyingLatch = timeoutNotifyingLatch;
    }

    @Timeout
    private void onTimeout(final Timer timer) {
        logger.info("Timeout invoked for " + this + " on timer " + timer);
        this.timeoutNotifyingLatch.countDown();
    }

    public void invokeOnExpiredTimer() throws NoSuchObjectLocalException {
        this.timer.getTimeRemaining();
    }


}

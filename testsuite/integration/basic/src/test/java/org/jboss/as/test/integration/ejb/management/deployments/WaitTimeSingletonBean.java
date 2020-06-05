/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ejb.management.deployments;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.AccessTimeout;
import javax.ejb.Remote;
import javax.ejb.Singleton;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;

import org.jboss.logging.Logger;

@Singleton
@TransactionManagement(TransactionManagementType.BEAN)
@AccessTimeout(value = 10, unit = TimeUnit.SECONDS)
@Remote(BusinessInterface.class)
public class WaitTimeSingletonBean implements BusinessInterface {
    private static final Logger logger = Logger.getLogger(WaitTimeSingletonBean.class);

    private final AtomicInteger timerNumbers = new AtomicInteger();

    @Resource
    private TimerService timerService;

    @Override
    public void doIt() {
        logger.info("Entering doIt method");
        startTimer();
    }

    @Override
    public void remove() {
        for (Timer t : timerService.getTimers()) {
            try {
                t.cancel();
            } catch (Exception ignore) {
            }
        }
    }

    @PostConstruct
    private void postConstruct() {
        startTimer();
        logger.info("Finishing postConstruct method");
    }

    private void startTimer() {
        final TimerConfig timerConfig = new TimerConfig("WaitTimeSingletonBean timer " + timerNumbers.getAndIncrement(), false);
        timerService.createSingleActionTimer(1, timerConfig);
    }

    @Timeout
    private void timeout(Timer timer) {
        final Serializable info = timer.getInfo();
        logger.info("Entering timeout method for " + info);
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.interrupted();
        }
        logger.info("Finishing timeout method for " + info);
    }

}

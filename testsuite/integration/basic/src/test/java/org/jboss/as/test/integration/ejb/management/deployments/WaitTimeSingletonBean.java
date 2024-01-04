/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.management.deployments;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.ejb.AccessTimeout;
import jakarta.ejb.Remote;
import jakarta.ejb.Singleton;
import jakarta.ejb.Timeout;
import jakarta.ejb.Timer;
import jakarta.ejb.TimerConfig;
import jakarta.ejb.TimerService;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;

import org.jboss.as.test.shared.TimeoutUtil;
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
            Thread.sleep(TimeoutUtil.adjust(50));
        } catch (InterruptedException e) {
            Thread.interrupted();
        }
        logger.info("Finishing timeout method for " + info);
    }

}

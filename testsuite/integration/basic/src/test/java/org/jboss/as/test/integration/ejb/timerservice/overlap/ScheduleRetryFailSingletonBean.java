/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.timerservice.overlap;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.ejb.AccessTimeout;
import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Timer;

import org.jboss.logging.Logger;

/**
 * SingletonBean to test that the schedule continue after a second timeout happen
 * during the singleton is still retrying the timeout.<br/>
 * See JIRA AS7-2995.
 *
 * @author <a href="mailto:wfink@redhat.com">Wolf-Dieter Fink</a>
 */
@Singleton
@AccessTimeout(unit = TimeUnit.MILLISECONDS, value = 100)  // make the test faster
public class ScheduleRetryFailSingletonBean {
    private static final Logger LOGGER = Logger.getLogger(ScheduleRetryFailSingletonBean.class);
    private static final CountDownLatch started = new CountDownLatch(1);
    private static final CountDownLatch alive = new CountDownLatch(4);

    /**
     * count the number of invoked timeouts
     */
    private static int counter;

    @SuppressWarnings("unused")
    @Schedule(second = "*", minute = "*", hour = "*", persistent = false)
    private void timeout(final Timer timer) {
        int wait = 0;
        counter++;
        LOGGER.trace("Executing TimerTestBean  " + timer);
        switch(counter) {
            case 1:
                started.countDown();
                throw new RuntimeException("Force retry for test");
            case 2:
                // ensure that the timeout retry is running if longer than schedule
                wait = 1300;
                break;
            default:
                break;
        }
        alive.countDown();
        LOGGER.trace("count=" + counter + "  Sleeping "+wait+"ms");
        try {
            Thread.sleep(wait);
        } catch (InterruptedException e) {}
        LOGGER.trace("Finished executing TimerTestBean nextTimeOut=" + timer.getNextTimeout());
    }

    /**
     * Latch to test whether the first timeout is executed.
     *
     * @return latch which is blocked until the first schedule
     */
    public static CountDownLatch startLatch() {
        return started;
    }
    /**
     * Latch to test whether a timeout is fired after overlapping with a timeout retry.
     * The test should wait a minimum of 4 seconds.
     *
     * @return latch which is blocked until a timeout happen after retry
     */
    public static CountDownLatch aliveLatch() {
        return alive;
    }
}

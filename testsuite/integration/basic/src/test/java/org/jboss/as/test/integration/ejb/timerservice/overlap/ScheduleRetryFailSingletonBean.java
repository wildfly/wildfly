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
package org.jboss.as.test.integration.ejb.timerservice.overlap;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.ejb.AccessTimeout;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Timer;

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

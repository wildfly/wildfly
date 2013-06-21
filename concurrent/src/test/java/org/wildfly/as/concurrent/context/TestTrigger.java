/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.wildfly.as.concurrent.context;

import org.junit.Assert;

import javax.enterprise.concurrent.LastExecution;
import javax.enterprise.concurrent.Trigger;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author Eduardo Martins
 */
public class TestTrigger implements Trigger {

    final CountDownLatch done = new CountDownLatch(1);

    private final String identityName;
    private final boolean skip;
    private final boolean abort;

    int nextRunTimeRuns = 0;
    int skipRuns = 0;
    LastExecution nextRuntimeLastExecution1;
    LastExecution nextRuntimeLastExecution2;
    LastExecution nextRuntimeLastExecution3;
    LastExecution nextRuntimeLastExecution4;
    Date nextRuntimeDate1;
    Date nextRuntimeDate2;
    Date nextRuntimeDate3;
    Date scheduledRunTime1;
    Date scheduledRunTime2;
    Date scheduledRunTime3;
    LastExecution skipRunLastExecution1;
    LastExecution skipRunLastExecution2;
    LastExecution skipRunLastExecution3;

    public TestTrigger(String identityName, boolean skip, boolean abort) {
        this.skip = skip;
        this.abort = abort;
        this.identityName = identityName;
    }

    @Override
    public Date getNextRunTime(LastExecution lastExecution, Date taskScheduledTime) {
        System.out.println("trigger nextRunTimeRuns" + nextRunTimeRuns);
        if (nextRunTimeRuns == 0) {
            nextRuntimeLastExecution1 = lastExecution;
            nextRunTimeRuns++;
            nextRuntimeDate1 = new Date(System.currentTimeMillis() + (nextRunTimeRuns * 100));
            return nextRuntimeDate1;
        }
        if (nextRunTimeRuns == 1) {
            nextRuntimeLastExecution2 = lastExecution;
            nextRunTimeRuns++;
            nextRuntimeDate2 = new Date(System.currentTimeMillis() + (nextRunTimeRuns * 100));
            return nextRuntimeDate2;
        }
        if (nextRunTimeRuns == 2) {
            nextRuntimeLastExecution3 = lastExecution;
            nextRunTimeRuns++;
            nextRuntimeDate3 = new Date(System.currentTimeMillis() + (nextRunTimeRuns * 100));
            return nextRuntimeDate3;
        }
        if (nextRunTimeRuns == 3) {
            nextRunTimeRuns++;
            nextRuntimeLastExecution4 = lastExecution;
            // return null to signal task done
            try {
                return null;
            } finally {
                done.countDown();
            }
        }
        // too many runs already
        throw new RuntimeException();
    }

    @Override
    public boolean skipRun(LastExecution lastExecution, Date scheduledRunTime) {
        System.out.println("trigger skipRuns" + skipRuns);
        if (skipRuns == 0) {
            skipRunLastExecution1 = lastExecution;
            scheduledRunTime1 = scheduledRunTime;
            skipRuns++;
            return false;
        }
        if (skipRuns == 1) {
            skipRunLastExecution2 = lastExecution;
            scheduledRunTime2 = scheduledRunTime;
            skipRuns++;
            return skip;
        }
        if (skipRuns == 2) {
            skipRunLastExecution3 = lastExecution;
            scheduledRunTime3 = scheduledRunTime;
            skipRuns++;
            if (!abort) {
                return false;
            } else {
                throw new RuntimeException();
            }
        }
        // too many runs already
        throw new RuntimeException();
    }

    void assertResults() throws Exception {

        assertIsDone();

        Assert.assertEquals(4, nextRunTimeRuns);
        Assert.assertEquals(3, skipRuns);

        Assert.assertNull(nextRuntimeLastExecution1);
        Assert.assertNull(skipRunLastExecution1);
        Assert.assertNotNull(nextRuntimeDate1);
        Assert.assertNotNull(scheduledRunTime1);
        Assert.assertEquals(nextRuntimeDate1, scheduledRunTime1);

        Assert.assertNotNull(nextRuntimeLastExecution2);
        Assert.assertNotNull(skipRunLastExecution2);
        Assert.assertNotNull(nextRuntimeDate2);
        Assert.assertNotNull(scheduledRunTime2);
        Assert.assertEquals(nextRuntimeDate2, scheduledRunTime2);
        Assert.assertEquals(identityName, nextRuntimeLastExecution2.getIdentityName());
        Assert.assertEquals(identityName, skipRunLastExecution2.getIdentityName());


        Assert.assertNotNull(nextRuntimeLastExecution3);
        if (skip) {
            Assert.assertNotNull(nextRuntimeLastExecution3.getRunStart());
            Assert.assertNull(nextRuntimeLastExecution3.getRunEnd());
        }
        Assert.assertNotNull(skipRunLastExecution3);
        if (skip) {
            Assert.assertNotNull(skipRunLastExecution3.getRunStart());
            Assert.assertNull(skipRunLastExecution3.getRunEnd());
        }
        Assert.assertNotNull(nextRuntimeDate3);
        Assert.assertNotNull(scheduledRunTime3);
        Assert.assertEquals(nextRuntimeDate3, scheduledRunTime3);
        Assert.assertEquals(identityName, nextRuntimeLastExecution3.getIdentityName());
        Assert.assertEquals(identityName, skipRunLastExecution3.getIdentityName());

        Assert.assertNotNull(nextRuntimeLastExecution4);
        if (abort) {
            Assert.assertNotNull(nextRuntimeLastExecution4.getRunStart());
            Assert.assertNull(nextRuntimeLastExecution4.getRunEnd());
        }
        Assert.assertEquals(identityName, nextRuntimeLastExecution4.getIdentityName());
    }

    void assertIsDone() throws InterruptedException {
        assertIsDone(5);
    }

    void assertIsDone(int seconds) throws InterruptedException {
        if (!done.await(seconds, TimeUnit.SECONDS)) {
            Assert.fail();
        }
    }

}

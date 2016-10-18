/*
 * Copyright 2016 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.as.test.integration.ee.concurrent;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.chrono.ChronoZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Resource;
import javax.ejb.Singleton;
import javax.enterprise.concurrent.LastExecution;
import javax.enterprise.concurrent.ManagedScheduledExecutorService;
import javax.enterprise.concurrent.SkippedException;
import javax.enterprise.concurrent.Trigger;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@RunWith(Arquillian.class)
@Singleton
public class ManagedScheduledExecutorServiceTestCase {

    @Resource
    private ManagedScheduledExecutorService executorService;

    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class)
                .addClass(ManagedScheduledExecutorServiceTestCase.class);
    }

    @Test
    public void triggerTest() throws Exception {
        final ScheduledFuture<Integer> future = executorService.schedule(new CallCountingTask(), createTrigger(100, ChronoUnit.MILLIS));

        // Make sure we run at least once
        final int result = future.get(300, TimeUnit.MILLISECONDS);
        Assert.assertTrue("Expected at least 1 runs, but found only " + result, result > 0);

        // Cancel the future and ensure it's cancelled and done
        future.cancel(true);
        Assert.assertTrue("Future should be cancelled, but future.isCancelled() returned false", future.isCancelled());
        Assert.assertTrue("Future should be done, but future.isDone() returned false", future.isDone());
    }

    @Test
    public void triggerSkipTest() throws Exception {
        long timeout = 5000L;
        final ScheduledFuture<Integer> future = executorService.schedule(new CallCountingTask(), createTrigger(0, 100, ChronoUnit.MILLIS));
        try {
            while (!future.isDone()) {
                final long before = System.currentTimeMillis();
                try {
                    future.get(100, TimeUnit.MILLISECONDS);
                } catch (SkippedException ignore) {
                    return;
                } catch (TimeoutException ignore) {
                }
                timeout -= (System.currentTimeMillis() - before);
                if (timeout < 0) {
                    Assert.fail(String.format("Took longer than 5 seconds for %s to be thrown.", SkippedException.class.getName()));
                }
            }
        } finally {
            future.cancel(true);
        }
        Assert.fail(String.format("Expected a %s to be thrown.", SkippedException.class.getName()));
    }

    private static Trigger createTrigger(final int nextRunAmount, final TemporalUnit nextRunUnit) {
        return new RunLimitTrigger(0, nextRunAmount, nextRunUnit) {

            @Override
            public boolean skipRun(final LastExecution lastExecutionInfo, final Date scheduledRunTime) {
                return false;
            }
        };
    }

    private static Trigger createTrigger(final int maxRuns, final int nextRunAmount, final TemporalUnit nextRunUnit) {
        return new RunLimitTrigger(maxRuns, nextRunAmount, nextRunUnit);
    }

    private static class CallCountingTask implements Callable<Integer> {
        final AtomicInteger callCounter = new AtomicInteger();

        @Override
        public Integer call() throws Exception {
            return callCounter.incrementAndGet();
        }
    }

    private static class RunLimitTrigger implements Trigger {
        private final int nextRunAmount;
        private final TemporalUnit nextRunUnit;
        private final int maxRuns;
        final AtomicInteger counter = new AtomicInteger();

        private RunLimitTrigger(final int maxRuns, final int nextRunAmount, final TemporalUnit nextRunUnit) {
            this.nextRunAmount = nextRunAmount;
            this.nextRunUnit = nextRunUnit;
            this.maxRuns = maxRuns;
        }

        @Override
        public Date getNextRunTime(final LastExecution lastExecutionInfo, final Date taskScheduledTime) {
            if (lastExecutionInfo == null) {
                return addTime(ZonedDateTime.now());
            }
            final Date date = lastExecutionInfo.getScheduledStart();
            return addTime(date);
        }

        @Override
        public boolean skipRun(final LastExecution lastExecutionInfo, final Date scheduledRunTime) {
            return counter.incrementAndGet() > maxRuns;
        }

        private Date addTime(final Date date) {
            return addTime(ZonedDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault()));
        }

        private Date addTime(final ChronoZonedDateTime date) {
            return Date.from(date.plus(nextRunAmount, nextRunUnit).toInstant());
        }
    }
}
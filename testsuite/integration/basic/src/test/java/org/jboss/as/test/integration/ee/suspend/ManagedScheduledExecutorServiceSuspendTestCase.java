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

package org.jboss.as.test.integration.ee.suspend;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Resource;
import javax.ejb.Singleton;
import javax.enterprise.concurrent.LastExecution;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.concurrent.ManagedScheduledExecutorService;
import javax.enterprise.concurrent.ManagedTask;
import javax.enterprise.concurrent.ManagedTaskListener;
import javax.enterprise.concurrent.Trigger;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@SuppressWarnings({"MagicNumber", "Duplicates"})
@RunWith(Arquillian.class)
@Singleton
public class ManagedScheduledExecutorServiceSuspendTestCase {
    private static final int MILLIS_SLEEP_TIME = TimeoutUtil.adjust(120);
    private static final int MILLIS_WAIT_TIME = TimeoutUtil.adjust(300);
    private static final int SECONDS_WAIT_TIME = TimeoutUtil.adjust(1);

    @Resource
    private ManagedScheduledExecutorService executorService;
    @ArquillianResource
    private ManagementClient client;

    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class)
                .addClasses(ManagedScheduledExecutorServiceSuspendTestCase.class, TimeoutUtil.class)
                .addAsResource(new StringAsset("Dependencies: org.jboss.dmr, org.jboss.as.controller-client\n"), "META-INF/MANIFEST.MF");
    }

    @Test
    public void testExecutedRunnable() throws Exception {
        final AtomicInteger counter = new AtomicInteger();
        CountDownLatch latch = new CountDownLatch(1);

        // First submit a task with the server running
        executorService.execute(new CountdownRunnable(latch, counter));
        Assert.assertTrue("The runnable did not appear to be executed", latch.await(SECONDS_WAIT_TIME, TimeUnit.SECONDS));
        int currentCount = counter.get();
        Assert.assertEquals(String.format("Expected a count of 1, but got a count of %d", currentCount), 1, currentCount);

        // Suspend the server and attempt to submit the task again.
        latch = new CountDownLatch(1);
        suspend();
        executorService.execute(new CountdownRunnable(latch, counter));
        // The task should run while the server is suspended
        Assert.assertTrue("The runnable should not have ran", latch.await(MILLIS_WAIT_TIME, TimeUnit.MICROSECONDS));
        currentCount = counter.get();
        Assert.assertEquals(String.format("Expected a count of 2, but got a count of %d", currentCount), 2, currentCount);

        // Resume the server
        resume();
        // Just test once more after a resume to ensure everything seems normal
        latch = new CountDownLatch(1);
        executorService.execute(new CountdownRunnable(latch, counter));
        // Wait for the task to complete
        latch.await(SECONDS_WAIT_TIME, TimeUnit.SECONDS);
        currentCount = counter.get();
        Assert.assertEquals(String.format("Expected a count of 3, but got a count of %d", currentCount), 3, currentCount);
    }

    @Test
    public void testSubmittedRunnable() throws Exception {
        final AtomicInteger counter = new AtomicInteger();
        final CountingRunnable runnable = new CountingRunnable(counter);

        // First submit a call that will just run
        Future<?> future = executorService.submit(runnable);
        future.get(SECONDS_WAIT_TIME, TimeUnit.SECONDS);
        Assert.assertEquals(1, counter.get());
        Assert.assertTrue("Expected to the future to be done.", future.isDone());

        // Suspend the server and submit the task again, it should run as the ControlPoint should force the run
        suspend();
        future = executorService.submit(runnable);
        future.get(MILLIS_WAIT_TIME, TimeUnit.MILLISECONDS);
        Assert.assertEquals(2, counter.get());
        Assert.assertTrue("Expected to the future to be done.", future.isDone());

        // Resume the server
        resume();
        // Just test once more after a resume to ensure everything seems normal
        future = executorService.submit(runnable);
        future.get(SECONDS_WAIT_TIME, TimeUnit.SECONDS);
        Assert.assertEquals(3, counter.get());
        Assert.assertTrue("Expected to the future to be done.", future.isDone());
    }

    @Test
    public void testSubmittedCallable() throws Exception {
        final CountingCallable callable = new CountingCallable();

        // First submit a call that will just run
        Future<Integer> future = executorService.submit(callable);
        Assert.assertEquals(1, future.get(SECONDS_WAIT_TIME, TimeUnit.SECONDS).intValue());
        Assert.assertTrue("Expected to the future to be done.", future.isDone());

        // Suspend the server and submit the task again, it should run as the ControlPoint should force the run
        suspend();
        future = executorService.submit(callable);
        Assert.assertEquals(2, future.get(SECONDS_WAIT_TIME, TimeUnit.SECONDS).intValue());
        Assert.assertTrue("Expected to the future to be done.", future.isDone());

        // Resume the server
        resume();
        // Just test once more after a resume to ensure everything seems normal
        future = executorService.submit(callable);
        Assert.assertEquals(3, future.get(SECONDS_WAIT_TIME, TimeUnit.SECONDS).intValue());
        Assert.assertTrue("Expected to the future to be done.", future.isDone());
    }

    @Test
    public void testSubmittedRunnableFuture() throws Exception {
        final AtomicInteger counter = new AtomicInteger();
        final CountingRunnable runnable = new CountingRunnable(counter);

        // First submit a call that will just run
        Future<AtomicInteger> future = executorService.submit(runnable, counter);
        Assert.assertEquals(1, future.get(SECONDS_WAIT_TIME, TimeUnit.SECONDS).get());
        Assert.assertTrue("Expected to the future to be done.", future.isDone());

        // Suspend the server and submit the task again, it should run as the ControlPoint should force the run
        suspend();
        future = executorService.submit(runnable, counter);
        Assert.assertEquals(2, future.get(SECONDS_WAIT_TIME, TimeUnit.SECONDS).get());
        Assert.assertTrue("Expected to the future to be done.", future.isDone());

        // Resume the server
        resume();
        // Just test once more after a resume to ensure everything seems normal
        future = executorService.submit(runnable, counter);
        Assert.assertEquals(3, future.get(SECONDS_WAIT_TIME, TimeUnit.SECONDS).get());
        Assert.assertTrue("Expected to the future to be done.", future.isDone());
    }

    @Test
    public void testCancelScheduledTask() throws Exception {
        final AtomicInteger counter = new AtomicInteger();
        final CountingRunnable runnable = new CountingRunnable(counter);
        final Trigger trigger = new TimedTrigger(100, ChronoUnit.MILLIS);

        final ScheduledFuture<?> future = executorService.schedule(runnable, trigger);
        future.get(SECONDS_WAIT_TIME, TimeUnit.SECONDS);
        int currentCount = counter.get();
        // We won't know the exact count, but we should have more than 1
        Assert.assertTrue(String.format("Expected at least 1 run but found %d", currentCount), currentCount >= 1);

        // Suspend the server
        suspend();

        // We should be able to cancel this future, though it will likely return false since it did have a successful run.
        // The isCancelled() and isDone() should return true.
        final boolean cancelled = future.cancel(true);
        Assert.assertTrue(future.isCancelled());
        Assert.assertTrue(future.isDone());
        // Get the count after cancellation
        final int finalCount = counter.get();

        // Resume the server
        resume();
        // Wait at least until the next scheduled time to ensure it's really been cancelled
        TimeUnit.MILLISECONDS.sleep(MILLIS_SLEEP_TIME);
        Assert.assertEquals(finalCount, counter.get());
        // Test the get() to ensure it returns a value
        if (!cancelled) {
            future.get(SECONDS_WAIT_TIME, TimeUnit.SECONDS);
        } else {
            // If it was actually cancelled, get() should throw a CancellationException()
            try {
                future.get(SECONDS_WAIT_TIME, TimeUnit.SECONDS);
                Assert.fail("Expected ScheduledFuture.get() to throw a java.util.concurrent.CancellationException");
            } catch (CancellationException ignore) {
            }
        }
    }

    @Test
    public void testScheduledRunnableTrigger() throws Exception {
        final AtomicInteger counter = new AtomicInteger();
        final CountingRunnable runnable = new CountingRunnable(counter);
        final Trigger trigger = new TimedTrigger(100, ChronoUnit.MILLIS);

        final ScheduledFuture<?> future = executorService.schedule(runnable, trigger);
        future.get(SECONDS_WAIT_TIME, TimeUnit.SECONDS);
        // We won't know the exact count, but we should have more than 1
        int currentCount = counter.get();
        Assert.assertTrue(String.format("Expected at least 1 run but found %d", currentCount), currentCount >= 1);

        // Suspend the server
        suspend();
        // Get the current count
        final int minimumCount = counter.get();
        final long start = System.currentTimeMillis();
        // Sleep for 120ms to ensure the task has been ran at least once more
        TimeUnit.MILLISECONDS.sleep(MILLIS_SLEEP_TIME);
        Assert.assertTrue("Expected the tasks to continue running while the server is suspended.", counter.get() > minimumCount);

        resume();
        // The server is resumed we should get a result at some point as tasks should still be executing
        future.get(SECONDS_WAIT_TIME, TimeUnit.SECONDS);
        // We're going to guess at the range of the current runs, we'll know the minimum and the maximum will be a guess
        currentCount = counter.get();
        long maxCount = currentCount + ((System.currentTimeMillis() - start) / 100) + 2;
        Assert.assertTrue(String.format("Expected between %d and %d runs but found %d", minimumCount, maxCount, currentCount),
                (currentCount >= minimumCount && currentCount <= maxCount));

        // We should be able to cancel this future, though it will likely return false since it did have a successful run.
        // The isCancelled() and isDone() should return true.
        final boolean cancelled = future.cancel(true);
        Assert.assertTrue(future.isCancelled());
        Assert.assertTrue(future.isDone());
        final int finalCount = counter.get();
        // Wait at least until the next scheduled time to ensure it's really been cancelled
        TimeUnit.MILLISECONDS.sleep(MILLIS_SLEEP_TIME);
        Assert.assertEquals(finalCount, counter.get());
        // Test the get() to ensure it returns a value
        if (!cancelled) {
            future.get(SECONDS_WAIT_TIME, TimeUnit.SECONDS);
        } else {
            // If it was actually cancelled, get() should throw a CancellationException()
            try {
                future.get(SECONDS_WAIT_TIME, TimeUnit.SECONDS);
                Assert.fail("Expected ScheduledFuture.get() to throw a java.util.concurrent.CancellationException");
            } catch (CancellationException ignore) {
            }
        }
    }

    @Test
    public void testScheduledCallableTrigger() throws Exception {
        final CountingCallable callable = new CountingCallable();
        final Trigger trigger = new TimedTrigger(100, ChronoUnit.MILLIS);

        final ScheduledFuture<Integer> future = executorService.schedule(callable, trigger);
        int currentCount = future.get(SECONDS_WAIT_TIME, TimeUnit.SECONDS);
        // We won't know the exact count, but we should have more than 1
        Assert.assertTrue(String.format("Expected at least 1 run but found %d", currentCount), currentCount >= 1);

        // Suspend the server
        suspend();

        // Tasks should still be executing while the server is suspended
        final int minimumCount = future.get(SECONDS_WAIT_TIME, TimeUnit.SECONDS);
        final long start = System.currentTimeMillis();
        // Sleep for 120ms to ensure the task has been ran at least once more
        TimeUnit.MILLISECONDS.sleep(MILLIS_SLEEP_TIME);
        Assert.assertTrue("Expected the tasks to continue running while the server is suspended.", future.get(SECONDS_WAIT_TIME, TimeUnit.SECONDS) > minimumCount);

        resume();
        // The server is resumed we should get a result at some point as tasks should still be executing
        currentCount = future.get(SECONDS_WAIT_TIME, TimeUnit.SECONDS);
        // We're going to guess at the range of the current runs, we'll know the minimum and the maximum will be a guess
        long maxCount = currentCount + ((System.currentTimeMillis() - start) / 100) + 2;
        Assert.assertTrue(String.format("Expected between %d and %d runs but found %d", minimumCount, maxCount, currentCount),
                (currentCount >= minimumCount && currentCount <= maxCount));

        // We should be able to cancel this future, though it will likely return false since it did have a successful run.
        // The isCancelled() and isDone() should return true.
        final boolean cancelled = future.cancel(true);
        Assert.assertTrue(future.isCancelled());
        Assert.assertTrue(future.isDone());
        // Test the get() to ensure it returns a value
        if (!cancelled) {
            future.get(SECONDS_WAIT_TIME, TimeUnit.SECONDS);
        } else {
            // If it was actually cancelled, get() should throw a CancellationException()
            try {
                future.get(SECONDS_WAIT_TIME, TimeUnit.SECONDS);
                Assert.fail("Expected ScheduledFuture.get() to throw a java.util.concurrent.CancellationException");
            } catch (CancellationException ignore) {
            }
        }
    }

    @Test
    public void testScheduledRunnableDelay() throws Exception {
        final AtomicInteger counter = new AtomicInteger();
        final CountingRunnable runnable = new CountingRunnable(counter);

        ScheduledFuture<?> future = executorService.schedule(runnable, 10, TimeUnit.MILLISECONDS);
        future.get(SECONDS_WAIT_TIME, TimeUnit.SECONDS);
        // This should only have been run once
        int currentCount = counter.get();
        Assert.assertEquals(String.format("Expected 1 run but found %d", currentCount), 1, currentCount);
        Assert.assertTrue("Expected to the future to be done.", future.isDone());

        // Suspend the server
        suspend();
        // Schedule a new task while suspended which should run as the ControlPoint should force the run
        future = executorService.schedule(runnable, 10, TimeUnit.MILLISECONDS);
        future.get(SECONDS_WAIT_TIME, TimeUnit.SECONDS);
        currentCount = counter.get();
        Assert.assertEquals(String.format("Expected 2 run but found %d", currentCount), 2, currentCount);
        Assert.assertTrue("Expected to the future to be done.", future.isDone());

        // Resume the server
        resume();
        // Schedule the task once more to ensure everything works correctly
        future = executorService.schedule(runnable, 10, TimeUnit.MILLISECONDS);
        future.get(SECONDS_WAIT_TIME, TimeUnit.SECONDS);
        currentCount = counter.get();
        Assert.assertEquals(String.format("Expected 3 runs run but found %d", currentCount), 3, currentCount);
        Assert.assertTrue("Expected to the future to be done.", future.isDone());
    }

    @Test
    public void testScheduledCallableDelay() throws Exception {
        final CountingCallable callable = new CountingCallable();

        ScheduledFuture<Integer> future = executorService.schedule(callable, 10, TimeUnit.MILLISECONDS);
        // This should only have been run once
        int currentCount = future.get(SECONDS_WAIT_TIME, TimeUnit.SECONDS);
        Assert.assertEquals(String.format("Expected 1 run but found %d", currentCount), 1, currentCount);
        Assert.assertTrue("Expected to the future to be done.", future.isDone());

        // Suspend the server
        suspend();
        // Schedule a new task while suspended which should run as the ControlPoint should force the run
        future = executorService.schedule(callable, 10, TimeUnit.MILLISECONDS);
        currentCount = future.get(SECONDS_WAIT_TIME, TimeUnit.SECONDS);
        Assert.assertEquals(String.format("Expected 2 run but found %d", currentCount), 2, currentCount);
        Assert.assertTrue("Expected to the future to be done.", future.isDone());

        // Resume the server
        resume();
        // Schedule the task once more to ensure everything works correctly
        future = executorService.schedule(callable, 10, TimeUnit.MILLISECONDS);
        currentCount = future.get(SECONDS_WAIT_TIME, TimeUnit.SECONDS);
        Assert.assertEquals(String.format("Expected 3 runs run but found %d", currentCount), 3, currentCount);
        Assert.assertTrue("Expected to the future to be done.", future.isDone());
    }

    @Test
    public void testScheduleAtFixedRate() throws Exception {
        final AtomicInteger counter = new AtomicInteger();
        final CountingRunnable runnable = new CountingRunnable(counter);

        final ScheduledFuture<?> future = executorService.scheduleAtFixedRate(runnable, 0, 100, TimeUnit.MILLISECONDS);
        // Sleep for 120ms to ensure the task has been ran at least once
        TimeUnit.MILLISECONDS.sleep(MILLIS_SLEEP_TIME);
        // We won't know the exact count, but we should have more than 1
        int currentCount = counter.get();
        Assert.assertTrue(String.format("Expected at least 1 run but found %d", currentCount), currentCount >= 1);

        // Suspend the server
        suspend();
        // Get the current count
        final int minimumCount = counter.get();
        final long start = System.currentTimeMillis();
        // Sleep for 120ms to ensure the task has been ran at least once more
        TimeUnit.MILLISECONDS.sleep(MILLIS_SLEEP_TIME);
        Assert.assertTrue("Expected the tasks to continue running while the server is suspended.", counter.get() > minimumCount);

        // Resume the server
        resume();
        // Sleep for 120ms to ensure the task has been ran at least once more
        TimeUnit.MILLISECONDS.sleep(MILLIS_SLEEP_TIME);
        // We're going to guess at the range of the current runs, we'll know the minimum and the maximum will be a guess
        currentCount = counter.get();
        long maxCount = currentCount + ((System.currentTimeMillis() - start) / 100) + 2;
        Assert.assertTrue(String.format("Expected between %d and %d runs but found %d", minimumCount, maxCount, currentCount),
                (currentCount >= minimumCount && currentCount <= maxCount));

        // We should be able to cancel this future
        future.cancel(true);
        Assert.assertTrue(future.isCancelled());
        Assert.assertTrue(future.isDone());
        try {
            future.get(SECONDS_WAIT_TIME, TimeUnit.SECONDS);
            Assert.fail("Expected ScheduledFuture.get() to throw a java.util.concurrent.CancellationException");
        } catch (CancellationException ignore) {
        }
    }

    @Test
    public void testScheduleAWithFixedDelay() throws Exception {
        //executorService.scheduleWithFixedDelay();
        final AtomicInteger counter = new AtomicInteger();
        final CountingRunnable runnable = new CountingRunnable(counter);

        final ScheduledFuture<?> future = executorService.scheduleWithFixedDelay(runnable, 0, 100, TimeUnit.MILLISECONDS);
        // Sleep for 120ms to ensure the task has been ran at least once
        TimeUnit.MILLISECONDS.sleep(MILLIS_SLEEP_TIME);
        // We won't know the exact count, but we should have more than 1
        int currentCount = counter.get();
        Assert.assertTrue(String.format("Expected at least 1 run but found %d", currentCount), currentCount >= 1);

        // Suspend the server
        suspend();
        // Get the current count
        final int minimumCount = counter.get();
        final long start = System.currentTimeMillis();
        // Sleep for 120ms to ensure the task has been ran at least once more
        TimeUnit.MILLISECONDS.sleep(MILLIS_SLEEP_TIME);
        Assert.assertTrue("Expected the tasks to continue running while the server is suspended.", counter.get() > minimumCount);

        resume();
        // Sleep for 120ms to ensure the task has been ran at least once more
        TimeUnit.MILLISECONDS.sleep(MILLIS_SLEEP_TIME);
        // We're going to guess at the range of the current runs, we'll know the minimum and the maximum will be a guess
        currentCount = counter.get();
        long maxCount = currentCount + ((System.currentTimeMillis() - start) / 100) + 2;
        Assert.assertTrue(String.format("Expected between %d and %d runs but found %d", minimumCount, maxCount, currentCount),
                (currentCount >= minimumCount && currentCount <= maxCount));

        // We should be able to cancel this future
        future.cancel(true);
        Assert.assertTrue(future.isCancelled());
        Assert.assertTrue(future.isDone());
        try {
            future.get(SECONDS_WAIT_TIME, TimeUnit.SECONDS);
            Assert.fail("Expected ScheduledFuture.get() to throw a java.util.concurrent.CancellationException");
        } catch (CancellationException ignore) {
        }
    }

    @Test
    public void testManagedTaskCompletion() throws Exception {
        final AtomicInteger counter = new AtomicInteger();
        // The latch should be hit 3 times; taskSubmitted(), taskStarting() and taskDone()
        CountDownLatch latch = new CountDownLatch(3);
        executorService.submit(new CountingRunnableManagedTask(counter, latch));
        Assert.assertTrue("Expected the ManagedTaskListener.taskDone() to be invoked.", latch.await(SECONDS_WAIT_TIME, TimeUnit.SECONDS));
        Assert.assertEquals("Expected the task to be ran once", 1, counter.get());

        // Suspend the server and submit the task again, it should run as the ControlPoint should force the run
        suspend();
        // The latch should be hit 3 times; taskSubmitted(), taskStarting() and taskDone()
        latch = new CountDownLatch(3);
        executorService.submit(new CountingRunnableManagedTask(counter, latch));
        // We should timeout here
        Assert.assertTrue("The runnable should not have ran", latch.await(SECONDS_WAIT_TIME, TimeUnit.SECONDS));
        Assert.assertEquals("Expected the task to be ran twice", 2, counter.get());

        // Resume the server
        resume();
        // Execute one more task after resume to ensure everything is still working correctly
        // The latch should be hit 3 times; taskSubmitted(), taskStarting() and taskDone()
        latch = new CountDownLatch(3);
        executorService.submit(new CountingRunnableManagedTask(counter, latch));
        Assert.assertTrue("Expected the ManagedTaskListener.taskDone() to be invoked.", latch.await(SECONDS_WAIT_TIME, TimeUnit.SECONDS));
        Assert.assertEquals("Expected the task to be ran 3 times", 3, counter.get());
    }

    private void suspend() throws IOException {
        suspend(SECONDS_WAIT_TIME);
    }

    private void suspend(final int timeout) throws IOException {
        final ModelNode op = Operations.createOperation("suspend");
        op.get("timeout").set(timeout);
        final ModelNode result = client.getControllerClient().execute(op);
        if (!Operations.isSuccessfulOutcome(result)) {
            throw new RuntimeException("Failed to suspend server: " + Operations.getFailureDescription(result).asString());
        }
    }

    private void resume() throws IOException {
        final ModelNode result = client.getControllerClient().execute(Operations.createOperation("resume"));
        if (!Operations.isSuccessfulOutcome(result)) {
            throw new RuntimeException("Failed to resume server: " + Operations.getFailureDescription(result).asString());
        }
    }

    private static class CountdownRunnable implements Runnable {
        private final CountDownLatch latch;
        private final AtomicInteger counter;

        private CountdownRunnable(final CountDownLatch latch, final AtomicInteger counter) {
            this.latch = latch;
            this.counter = counter;
        }

        @Override
        public void run() {
            counter.incrementAndGet();
            latch.countDown();
        }
    }

    private static class CountingCallable implements Callable<Integer> {
        private final AtomicInteger counter = new AtomicInteger();

        @Override
        public Integer call() throws Exception {
            return counter.incrementAndGet();
        }
    }

    private static class CountingRunnable implements Runnable {
        private final AtomicInteger counter;

        private CountingRunnable(final AtomicInteger counter) {
            this.counter = counter;
        }

        @Override
        public void run() {
            counter.incrementAndGet();
        }
    }

    private static class TimedTrigger implements Trigger {
        private final long offset;
        private final TemporalUnit offsetUnit;

        private TimedTrigger(final int offset, final TemporalUnit offsetUnit) {
            this.offset = TimeoutUtil.adjust(offset);
            this.offsetUnit = offsetUnit;
        }

        @Override
        public Date getNextRunTime(final LastExecution lastExecutionInfo, final Date taskScheduledTime) {
            return Date.from(ZonedDateTime.ofInstant(taskScheduledTime.toInstant(), ZoneId.systemDefault()).plus(offset, offsetUnit).toInstant());
        }

        @Override
        public boolean skipRun(final LastExecution lastExecutionInfo, final Date scheduledRunTime) {
            return false;
        }
    }

    private static class CountingRunnableManagedTask implements Runnable, ManagedTask {
        private final AtomicInteger counter;
        private final CountDownLatch latch;

        private CountingRunnableManagedTask(final AtomicInteger counter, final CountDownLatch latch) {
            this.counter = counter;
            this.latch = latch;
        }

        @Override
        public void run() {
            counter.incrementAndGet();
        }

        @Override
        public Map<String, String> getExecutionProperties() {
            return null;
        }

        @Override
        public ManagedTaskListener getManagedTaskListener() {
            return new ManagedTaskListener() {
                @Override
                public void taskAborted(final Future<?> future, final ManagedExecutorService executor, final Object task, final Throwable exception) {
                    final StringWriter writer = new StringWriter();
                    exception.printStackTrace(new PrintWriter(writer));
                    Assert.fail("Task was aborted: " + writer);
                }

                @Override
                public void taskDone(final Future<?> future, final ManagedExecutorService executor, final Object task, final Throwable exception) {
                    latch.countDown();
                }

                @Override
                public void taskStarting(final Future<?> future, final ManagedExecutorService executor, final Object task) {
                    latch.countDown();
                }

                @Override
                public void taskSubmitted(final Future<?> future, final ManagedExecutorService executor, final Object task) {
                    latch.countDown();
                }
            };
        }
    }
}
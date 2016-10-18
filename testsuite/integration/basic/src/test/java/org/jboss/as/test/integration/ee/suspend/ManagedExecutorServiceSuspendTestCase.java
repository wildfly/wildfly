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
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Resource;
import javax.ejb.Singleton;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.concurrent.ManagedTask;
import javax.enterprise.concurrent.ManagedTaskListener;

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
public class ManagedExecutorServiceSuspendTestCase {
    private static final int MILLIS_WAIT_TIME = TimeoutUtil.adjust(300);
    private static final int SECONDS_WAIT_TIME = TimeoutUtil.adjust(1);

    @Resource
    private ManagedExecutorService executorService;
    @ArquillianResource
    private ManagementClient client;

    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class)
                .addClasses(ManagedExecutorServiceSuspendTestCase.class, TimeoutUtil.class)
                .addAsResource(new StringAsset("Dependencies: org.jboss.dmr, org.jboss.as.controller-client\n"), "META-INF/MANIFEST.MF");
    }

    @Test
    public void testExecutedQueuedRunnable() throws Exception {
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
        // We should timeout here
        Assert.assertFalse("The runnable should not have ran", latch.await(MILLIS_WAIT_TIME, TimeUnit.MICROSECONDS));

        // Resume the server
        resume();
        // Wait for the task to complete
        latch.await(SECONDS_WAIT_TIME, TimeUnit.SECONDS);
        currentCount = counter.get();
        Assert.assertEquals(String.format("Expected a count of 2, but got a count of %d", currentCount), 2, currentCount);
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
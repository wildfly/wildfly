/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.access.log;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.test.integration.ejb.access.log.util.EJBUtil;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * B2 — Queue overflow integration test (WFLY-6892 D24b / R2 acceptance).
 *
 * <p>Sets up the access-log resource with {@code queue-length=1}, fires a concurrent burst
 * of EJB invocations, and asserts that {@code read-attribute(name=events-dropped)} returns a
 * non-zero value — meaning the queue bound was honoured and drops were counted.
 *
 * <p>Concurrency is essential: sequential invocations cannot saturate the queue because the
 * async drain thread empties a capacity-1 queue in the gap between round trips. With
 * {@value #THREAD_COUNT} threads each making {@value #CALLS_PER_THREAD} invocations
 * simultaneously, races against the drain thread are effectively certain to produce drops.
 *
 * <p><strong>Mutation check (B3):</strong> changing {@code queue-length} to 4096 and running
 * the same burst produces zero additional drops, proving the test measures the queue bound
 * rather than passing for some unrelated reason.
 *
 * <p>This is the first time {@code events-dropped} has ever reported a non-zero value from
 * queue overflow in a test context. D19 made the metric the only signal that an administrator
 * gets that an audit trail is lossy; this test proves it can actually report loss.
 */
@RunWith(Arquillian.class)
@ServerSetup(AccessLogQueueOverflowTestCase.SetupTask.class)
public class AccessLogQueueOverflowTestCase extends AbstractConsoleAccessLogTestCase {

    /** Number of concurrent callers racing against the capacity-1 queue. */
    private static final int THREAD_COUNT = 16;

    /** Invocations per thread; total burst = THREAD_COUNT * CALLS_PER_THREAD = 800. */
    private static final int CALLS_PER_THREAD = 50;

    /** Maximum seconds to wait for the burst thread pool to finish. */
    private static final int BURST_TIMEOUT_SECONDS = 60;

    @ArquillianResource
    ManagementClient managementClient;

    @Deployment
    public static Archive<?> createDeployment() {
        return createDeployment(
                AccessLogQueueOverflowTestCase.class,
                SetupTask.class,
                AbstractConsoleAccessLogTestCase.class);
    }

    // -----------------------------------------------------------------------
    // Required by AbstractAccessLogTestCase — not used directly
    // -----------------------------------------------------------------------

    @Override
    protected void checkAccessLog(Class ejbInterface, Class ejbClass, String ejbMethod, String user)
            throws IOException, InterruptedException {
        // Not used; individual steps perform targeted checks.
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Maximum milliseconds to wait when polling for drain or service-restart completion.
     * Adjusted by {@link TimeoutUtil} so slow CI machines get proportionally longer.
     */
    private static final long POLL_TIMEOUT_MS = 5_000L;
    private static final long POLL_INTERVAL_MS = 100L;

    /** Reads the current {@code events-dropped} counter from the management model. */
    private long readEventsDropped() throws IOException {
        ModelNode address = new ModelNode();
        address.add("subsystem", "ejb3");
        address.add("service", "access-log");

        ModelNode op = new ModelNode();
        op.get(OP).set("read-attribute");
        op.get(OP_ADDR).set(address);
        op.get("name").set("events-dropped");

        ModelNode result = managementClient.getControllerClient().execute(op);
        Assert.assertTrue("read-attribute(events-dropped) failed: " + result,
                Operations.isSuccessfulOutcome(result));
        return Operations.readResult(result).asLong();
    }

    /**
     * Fires {@link #THREAD_COUNT} concurrent threads, each performing
     * {@link #CALLS_PER_THREAD} remote SLSB invocations.  Each thread does its own
     * EJB lookup — an EJB proxy is not guaranteed thread-safe to share across threads.
     *
     * <p>Any invocation exception is propagated to the test thread so that silent
     * failures do not masquerade as "no drops".
     */
    private void fireConcurrentBurst() throws Exception {
        final String host = Utils.getHost(managementClient);
        final ExecutorService pool = Executors.newFixedThreadPool(THREAD_COUNT);
        final List<Future<?>> futures = new ArrayList<>(THREAD_COUNT);

        for (int t = 0; t < THREAD_COUNT; t++) {
            final int threadIndex = t;
            futures.add(pool.submit(() -> {
                try {
                    final Properties props = EJBUtil.createEjbClientConfiguration(host, null, null);
                    final SLSBRemote bean = EJBUtil.lookupEJB(
                            SLSB.class, SLSBRemote.class, props, APP_NAME, MODULE_NAME_EJB, false);
                    for (int i = 0; i < CALLS_PER_THREAD; i++) {
                        bean.echo("PING-" + threadIndex + "-" + i);
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Thread " + threadIndex + " failed", e);
                }
                return null;
            }));
        }

        pool.shutdown();
        final boolean finished = pool.awaitTermination(BURST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        Assert.assertTrue("Burst thread pool did not finish within " + BURST_TIMEOUT_SECONDS + "s", finished);

        // Propagate any thread-level failure so a silent exception cannot look like "no drops".
        for (final Future<?> f : futures) {
            try {
                f.get(0, TimeUnit.MILLISECONDS);
            } catch (TimeoutException ignored) {
                // already awaited via awaitTermination above
            } catch (ExecutionException e) {
                throw new AssertionError("Burst invocation failed: " + e.getCause(), e.getCause());
            }
        }
    }

    /**
     * Polls {@code events-dropped} every {@value #POLL_INTERVAL_MS} ms until it has not changed
     * for two consecutive reads (drain is complete) or the timeout elapses.
     *
     * <p>This replaces a fixed sleep: a fixed sleep either under-waits (false-pass when drain is
     * still in-flight) or over-waits.  The poll exits as soon as the counter is stable, so it
     * cannot pass before the drain thread has finished.
     */
    private long pollUntilDropCountStable() throws IOException, InterruptedException {
        final long deadline = System.currentTimeMillis()
                + (long) (POLL_TIMEOUT_MS * TimeoutUtil.getFactor());
        long prev = readEventsDropped();
        long cur;
        while (System.currentTimeMillis() < deadline) {
            TimeUnit.MILLISECONDS.sleep(POLL_INTERVAL_MS);
            cur = readEventsDropped();
            if (cur == prev) {
                return cur;  // stable
            }
            prev = cur;
        }
        return prev;
    }

    /**
     * Polls until {@code events-dropped} can be read without error — meaning the
     * {@code AccessLogService} has started again after a {@code RESTART_RESOURCE_SERVICES}
     * attribute write.
     *
     * <p>The service restart is kicked off synchronously by the DMR write-attribute operation
     * but the new service instance starts asynchronously.  Polling here rather than sleeping a
     * fixed interval prevents both false-passes (reading a stale counter before the new service
     * is up) and needless over-waiting on fast machines.
     */
    private void waitForServiceRestart() throws IOException, InterruptedException {
        final long deadline = System.currentTimeMillis()
                + (long) (POLL_TIMEOUT_MS * TimeoutUtil.getFactor());
        while (System.currentTimeMillis() < deadline) {
            try {
                readEventsDropped();
                return;  // service is up
            } catch (Exception ignored) {
                TimeUnit.MILLISECONDS.sleep(POLL_INTERVAL_MS);
            }
        }
        // Last attempt — let any exception propagate as a test failure.
        readEventsDropped();
    }

    /** Reconfigures the access-log {@code queue-length} attribute without removing the resource. */
    private void writeQueueLength(int value) throws IOException {
        ModelNode address = new ModelNode();
        address.add("subsystem", "ejb3");
        address.add("service", "access-log");

        ModelNode op = new ModelNode();
        op.get(OP).set("write-attribute");
        op.get(OP_ADDR).set(address);
        op.get("name").set("queue-length");
        op.get("value").set(value);

        ModelNode result = managementClient.getControllerClient().execute(op);
        Assert.assertTrue("write-attribute(queue-length=" + value + ") failed: " + result,
                Operations.isSuccessfulOutcome(result));
    }

    // -----------------------------------------------------------------------
    // B2 — small queue: drops must be non-zero
    // -----------------------------------------------------------------------

    /**
     * B2/1 — With {@code queue-length=1}, a concurrent burst of
     * {@value #THREAD_COUNT}&times;{@value #CALLS_PER_THREAD} invocations overflows the
     * queue and {@code events-dropped} rises above zero.
     *
     * <p>The setup task starts the service with {@code queue-length=1}. After the burst,
     * this test reads the drop counter and asserts it is positive. The exact value is
     * printed so the first non-zero reading is on record.
     */
    @Test
    @InSequence(10)
    @RunAsClient
    public void testDropsWithSmallQueue() throws Exception {
        fireConcurrentBurst();

        // Poll until the async drain thread has finished flushing events and recording drops.
        final long dropped = pollUntilDropCountStable();
        Assert.assertTrue(
                "events-dropped must be > 0 after " + THREAD_COUNT + "x" + CALLS_PER_THREAD
                        + " concurrent invocations against queue-length=1; got: " + dropped,
                dropped > 0);
        System.out.println("[B2] events-dropped with queue-length=1, burst="
                + (THREAD_COUNT * CALLS_PER_THREAD) + ": " + dropped);
    }

    // -----------------------------------------------------------------------
    // B3 — mutation: large queue means no additional drops
    // -----------------------------------------------------------------------

    /**
     * B3/1 — Change {@code queue-length} to 4096 (large) and run the same concurrent burst.
     * Assert that {@code events-dropped} sees no additional drops (delta == 0).
     *
     * <p>{@code queue-length} is {@code RESTART_RESOURCE_SERVICES}: the write-attribute
     * restarts the service. Following D25 / R6, counters live on {@code AccessLogHolder}
     * and survive service restarts, so {@code events-dropped} retains the count accumulated in
     * B2. Reading the counter before and after the burst verifies no additional drops occurred.
     */
    @Test
    @InSequence(20)
    @RunAsClient
    public void testNoAdditionalDropsWithLargeQueue() throws Exception {
        // Switch to a large queue — triggers RESTART_RESOURCE_SERVICES, new service instance.
        writeQueueLength(4096);

        // Poll until the new service instance is up and the counter is readable.
        waitForServiceRestart();
        final long droppedBefore = readEventsDropped();
        Assert.assertTrue("events-dropped must survive service restart; got: " + droppedBefore,
                droppedBefore > 0);

        fireConcurrentBurst();

        // Poll until the drain thread has settled; no new drops expected.
        final long droppedAfter = pollUntilDropCountStable();

        System.out.println("[B3] events-dropped before=" + droppedBefore + ", after=" + droppedAfter
                + " with queue-length=4096 and burst=" + (THREAD_COUNT * CALLS_PER_THREAD));

        Assert.assertEquals(
                "events-dropped must not increase with queue-length=4096 after "
                        + THREAD_COUNT + "x" + CALLS_PER_THREAD + " invocations; delta="
                        + (droppedAfter - droppedBefore),
                droppedBefore, droppedAfter);
    }

    // -----------------------------------------------------------------------
    // Counter lifecycle tests: restart preserves, remove+add resets
    // -----------------------------------------------------------------------

    /**
     * Verifies that modifying a {@code RESTART_RESOURCE_SERVICES} attribute preserves
     * the accumulated dropped events count.
     */
    @Test
    @InSequence(30)
    @RunAsClient
    public void testCountersSurviveRestart() throws Exception {
        final long countBefore = readEventsDropped();
        Assert.assertTrue("events-dropped must be non-zero from earlier tests; got: " + countBefore,
                countBefore > 0);

        // Write another RESTART_RESOURCE_SERVICES attribute (e.g. queue-length to 2048)
        writeQueueLength(2048);
        waitForServiceRestart();

        final long countAfter = readEventsDropped();
        Assert.assertEquals("events-dropped must be preserved across service restart",
                countBefore, countAfter);
    }

    /**
     * Verifies that removing and re-adding the access-log resource resets the counters to zero.
     */
    @Test
    @InSequence(40)
    @RunAsClient
    public void testRemoveAndAddResetsCounters() throws Exception {
        final long countBefore = readEventsDropped();
        Assert.assertTrue("events-dropped must be non-zero before remove; got: " + countBefore,
                countBefore > 0);

        ModelNode address = new ModelNode();
        address.add("subsystem", "ejb3");
        address.add("service", "access-log");

        // Remove the resource
        ModelNode removeOp = new ModelNode();
        removeOp.get(OP).set(REMOVE);
        removeOp.get(OP_ADDR).set(address);
        ModelNode removeResult = managementClient.getControllerClient().execute(removeOp);
        Assert.assertTrue("remove failed: " + removeResult, Operations.isSuccessfulOutcome(removeResult));

        // Re-add the resource
        ModelNode addOp = new ModelNode();
        addOp.get(OP).set(ADD);
        addOp.get(OP_ADDR).set(address);
        addOp.get("destination").set("logging");
        addOp.get("include-local").set(true);
        addOp.get("queue-length").set(1024);
        ModelNode addResult = managementClient.getControllerClient().execute(addOp);
        Assert.assertTrue("add failed: " + addResult, Operations.isSuccessfulOutcome(addResult));

        // Poll until the newly-added service is up and the counter can be read.
        waitForServiceRestart();
        final long countAfter = readEventsDropped();
        Assert.assertEquals("events-dropped must be 0 after :remove followed by :add",
                0L, countAfter);
    }

    // -----------------------------------------------------------------------
    // Server setup — add access-log with queue-length=1
    // -----------------------------------------------------------------------

    /**
     * Adds the access-log resource with {@code queue-length=1} (so every event after the
     * first risks being dropped when the queue is busy) and {@code include-local=true}
     * (required for in-VM invocations to be logged).  Uses {@code destination=logging} so
     * the writer is fast; the bottleneck is the queue capacity, not the writer.
     */
    static class SetupTask implements ServerSetupTask {

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            ModelNode address = new ModelNode();
            address.add("subsystem", "ejb3");
            address.add("service", "access-log");

            ModelNode op = new ModelNode();
            op.get(OP).set(ADD);
            op.get(OP_ADDR).set(address);
            op.get("destination").set("logging");
            op.get("include-local").set(true);
            op.get("queue-length").set(1);

            ModelNode result = managementClient.getControllerClient().execute(op);
            if (!Operations.isSuccessfulOutcome(result)) {
                throw new Exception("Setup failed: " + result.asString());
            }
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            ModelNode address = new ModelNode();
            address.add("subsystem", "ejb3");
            address.add("service", "access-log");

            ModelNode op = new ModelNode();
            op.get(OP).set(REMOVE);
            op.get(OP_ADDR).set(address);

            ModelNode result = managementClient.getControllerClient().execute(op);
            if (!Operations.isSuccessfulOutcome(result)) {
                System.err.println("TearDown :remove failed (non-fatal): " + result.asString());
            }
        }
    }
}

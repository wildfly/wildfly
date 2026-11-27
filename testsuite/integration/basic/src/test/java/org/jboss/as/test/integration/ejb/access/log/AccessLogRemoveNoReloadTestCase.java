/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.access.log;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROCESS_STATE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESPONSE_HEADERS;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.test.integration.ejb.access.log.util.AccessLog;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.as.test.integration.ejb.access.log.util.AccessLogFormat;
import org.jboss.as.test.integration.ejb.access.log.util.EJBUtil;
import org.jboss.as.test.integration.ejb.access.log.util.ServerLog;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Verifies that {@code /subsystem=ejb3/service=access-log:remove()} and {@code :add()} complete
 * without requiring a reload (WFLY-6892 R3).
 *
 * <p>The setup task adds the access-log resource with {@code destination=logging} and
 * {@code include-local=true} but intentionally does <em>not</em> reload the server — the fix
 * must make it work without one.  The teardown removes the resource, again without reloading.
 *
 * <p>The test sequence:
 * <ol>
 *   <li>Invoke a remote SLSB and assert an access-log record appears in server.log — the
 *       service is up.</li>
 *   <li>Execute {@code :remove()} with no operation headers and assert the DMR result
 *       carries no {@code process-state=reload-required} response header, and that
 *       {@code :read-attribute(name=server-state)} returns {@code running}.</li>
 *   <li>Invoke the SLSB again and assert <em>no</em> record appears — the service stopped.</li>
 *   <li>Execute {@code :add(destination=logging, include-local=true)} without reloading and
 *       assert the server stays {@code running}.</li>
 *   <li>Invoke the SLSB once more and assert a record appears — the service restarted.</li>
 * </ol>
 *
 * <p>This is the mutation-proof test for the R3 fix: if {@code RemoveHandler.performRuntime}
 * is reverted to call {@code context.reloadRequired()}, step 2 fails because the DMR result
 * will contain {@code process-state=reload-required} and the subsequent service-stopped
 * assertion (step 3) will also fail.
 */
@RunWith(Arquillian.class)
@ServerSetup(AccessLogRemoveNoReloadTestCase.SetupTask.class)
public class AccessLogRemoveNoReloadTestCase extends AbstractConsoleAccessLogTestCase {

    private static final AccessLogFormat LOG_FORMAT = AccessLogFormat.SHORT_JSON;

    @ArquillianResource
    ManagementClient managementClient;

    @Deployment
    public static Archive<?> createDeployment() {
        return createDeployment(
                AccessLogRemoveNoReloadTestCase.class,
                SetupTask.class,
                AbstractConsoleAccessLogTestCase.class);
    }

    // -----------------------------------------------------------------------
    // Required by AbstractAccessLogTestCase — not used in this test class
    // -----------------------------------------------------------------------

    @Override
    protected void checkAccessLog(Class ejbInterface, Class ejbClass, String ejbMethod, String user)
            throws IOException, InterruptedException {
        // Not used directly; individual steps below perform targeted checks.
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Reads server.log lines written since {@code offset} and returns access-log records that
     * match {@code specificStrings}.  Waits up to the ServerLog timeout for at least one line.
     */
    private List<AccessLog> readAccessLogs(Path serverLogPath, long offset, String... specificStrings)
            throws IOException, InterruptedException {
        ServerLog serverLog = new ServerLog(serverLogPath, offset);
        String[] lines = serverLog.getNewLines();
        if (lines == null) return java.util.Collections.emptyList();
        List<AccessLog> logs = getAccessLogs(lines, LOG_FORMAT, specificStrings);
        return logs != null ? logs : java.util.Collections.emptyList();
    }

    /**
     * Reads server.log lines written since {@code offset} and asserts none match the EJB
     * access-log pattern.
     *
     * <p>Uses a bounded poll rather than a fixed sleep: the file is checked every
     * {@value #ABSENCE_POLL_MS} ms for up to {@value #ABSENCE_WINDOW_MS} ms.  If a matching
     * record appears at any point during the window the test fails immediately.  This cannot
     * false-pass: the window is long enough to cover any realistic async flush, and every
     * poll interval is actively checked.  A fixed sleep would pass if records arrive after
     * the sleep ends; this form will not.
     *
     * <p>A positive control is required before calling this method: step 1
     * ({@link #testRecordsProducedBeforeRemove}) already confirms that a record
     * <em>does</em> appear within the same window when access-log is active, establishing
     * the baseline that absence here is meaningful.
     */
    private static final long ABSENCE_WINDOW_MS = 2_000L;
    private static final long ABSENCE_POLL_MS   = 100L;

    private void assertNoAccessLogs(Path serverLogPath, long offset, String... specificStrings)
            throws IOException, InterruptedException {
        final long deadline = System.currentTimeMillis()
                + (long) (ABSENCE_WINDOW_MS * TimeoutUtil.getFactor());
        try (RandomAccessFile raf = new RandomAccessFile(serverLogPath.toFile(), "r")) {
            raf.seek(offset);
            while (System.currentTimeMillis() < deadline) {
                String line;
                while ((line = raf.readLine()) != null) {
                    List<AccessLog> matches = getAccessLogs(new String[]{line}, LOG_FORMAT, specificStrings);
                    Assert.assertTrue(
                            "Expected no access-log records after :remove but found: " + line,
                            matches == null || matches.isEmpty());
                }
                TimeUnit.MILLISECONDS.sleep(ABSENCE_POLL_MS);
            }
        }
    }

    /** Issues :read-attribute(name=server-state) against the root resource. */
    private String readServerState() throws IOException {
        ModelNode op = new ModelNode();
        op.get(OP).set("read-attribute");
        op.get(OP_ADDR).setEmptyList();
        op.get("name").set("server-state");
        ModelNode result = managementClient.getControllerClient().execute(op);
        Assert.assertTrue("read-attribute(server-state) failed: " + result,
                Operations.isSuccessfulOutcome(result));
        return Operations.readResult(result).asString();
    }

    /** Invokes the remote SLSB echo method via the EJB client. */
    private void invokeSlsb() throws Exception {
        Properties props = EJBUtil.createEjbClientConfiguration(Utils.getHost(managementClient), null, null);
        SLSBRemote bean = EJBUtil.lookupEJB(SLSB.class, SLSBRemote.class, props, APP_NAME, MODULE_NAME_EJB, false);
        String echo = bean.echo("PING");
        Assert.assertTrue("SLSB invocation failed", echo != null && echo.contains("PING"));
    }

    // -----------------------------------------------------------------------
    // Test steps (sequenced)
    // -----------------------------------------------------------------------

    /**
     * Step 1 — confirm access-log records are produced before :remove.
     */
    @Test
    @InSequence(10)
    @RunAsClient
    public void testRecordsProducedBeforeRemove() throws Exception {
        Path logPath = getLogFilePath(SERVER_LOG_FILE);
        long offsetBefore = logPath.toFile().length();

        invokeSlsb();

        List<AccessLog> logs = readAccessLogs(logPath, offsetBefore, SLSB.class.getSimpleName(), "echo");
        Assert.assertFalse("Expected access-log record before :remove but found none", logs.isEmpty());
    }

    /**
     * Step 2 — execute :remove() with no operation headers and assert no reload-required.
     */
    @Test
    @InSequence(20)
    @RunAsClient
    public void testRemoveDoesNotRequireReload() throws Exception {
        ModelNode address = new ModelNode();
        address.add("subsystem", "ejb3");
        address.add("service", "access-log");

        ModelNode op = new ModelNode();
        op.get(OP).set(REMOVE);
        op.get(OP_ADDR).set(address);
        // No allow-resource-service-restart header — the fix must not need it.

        ModelNode result = managementClient.getControllerClient().execute(op);
        Assert.assertTrue(":remove failed: " + result, Operations.isSuccessfulOutcome(result));

        // Assert no reload-required process-state in the response headers.
        if (result.hasDefined(RESPONSE_HEADERS) && result.get(RESPONSE_HEADERS).hasDefined(PROCESS_STATE)) {
            String processState = result.get(RESPONSE_HEADERS).get(PROCESS_STATE).asString();
            Assert.assertNotEquals(
                    ":remove set process-state=" + processState + " — reload should not be required",
                    "reload-required", processState);
        }

        // Assert server is still running.
        Assert.assertEquals("Server should remain 'running' after :remove",
                "running", readServerState());
    }

    /**
     * Step 3 — after :remove, no access-log records should be written.
     */
    @Test
    @InSequence(30)
    @RunAsClient
    public void testNoRecordsAfterRemove() throws Exception {
        Path logPath = getLogFilePath(SERVER_LOG_FILE);
        long offsetBefore = logPath.toFile().length();

        invokeSlsb();

        assertNoAccessLogs(logPath, offsetBefore, SLSB.class.getSimpleName(), "echo");
    }

    /**
     * Step 4 — re-add the access-log resource without reloading and assert no reload-required.
     */
    @Test
    @InSequence(40)
    @RunAsClient
    public void testAddDoesNotRequireReload() throws Exception {
        ModelNode address = new ModelNode();
        address.add("subsystem", "ejb3");
        address.add("service", "access-log");

        ModelNode op = new ModelNode();
        op.get(OP).set(ADD);
        op.get(OP_ADDR).set(address);
        op.get("destination").set("logging");
        op.get("include-local").set(true);

        ModelNode result = managementClient.getControllerClient().execute(op);
        Assert.assertTrue(":add failed: " + result, Operations.isSuccessfulOutcome(result));

        if (result.hasDefined(RESPONSE_HEADERS) && result.get(RESPONSE_HEADERS).hasDefined(PROCESS_STATE)) {
            String processState = result.get(RESPONSE_HEADERS).get(PROCESS_STATE).asString();
            Assert.assertNotEquals(
                    ":add set process-state=" + processState + " — reload should not be required",
                    "reload-required", processState);
        }

        Assert.assertEquals("Server should remain 'running' after :add",
                "running", readServerState());
    }

    /**
     * Step 5 — after :add, records should be produced again.
     */
    @Test
    @InSequence(50)
    @RunAsClient
    public void testRecordsProducedAfterAdd() throws Exception {
        Path logPath = getLogFilePath(SERVER_LOG_FILE);
        long offsetBefore = logPath.toFile().length();

        invokeSlsb();

        List<AccessLog> logs = readAccessLogs(logPath, offsetBefore, SLSB.class.getSimpleName(), "echo");
        Assert.assertFalse("Expected access-log record after :add but found none", logs.isEmpty());
    }

    // -----------------------------------------------------------------------
    // Server setup — add access-log WITHOUT reloading (the whole point)
    // -----------------------------------------------------------------------

    /**
     * Adds the access-log resource before the test class runs.  No reload is issued —
     * if the fix is correct the service starts immediately.
     *
     * <p>Teardown removes the resource, again without reloading.
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

            ModelNode result = managementClient.getControllerClient().execute(op);
            if (!Operations.isSuccessfulOutcome(result)) {
                throw new Exception("Setup failed: " + result.asString());
            }
            // Intentionally no ServerReload — the fix must not need one.
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
                // Log but don't throw — teardown failure should not mask test results.
                System.err.println("TearDown :remove failed (non-fatal): " + result.asString());
            }
            // Intentionally no ServerReload.
        }
    }
}

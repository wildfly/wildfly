/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.manualmode.transaction;

import static org.jboss.as.controller.client.helpers.Operations.isSuccessfulOutcome;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

import javax.naming.Context;
import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.byteman.agent.submit.Submit;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for the {@code graceful-shutdown-timeout} attribute introduced by WFLY-17742.
 * Covers management attribute operations (read, write, undefine, validation),
 * shutdown behavior with Byteman-simulated delays on
 * {@code TransactionReaper.waitForAllTxnsToTerminate()} and
 * {@code TwoPhaseCoordinator.end()}, and a WFLY-22035 regression test verifying that
 * {@code @PreDestroy} can still begin transactions during server shutdown.
 */
@RunWith(Arquillian.class)
@RunAsClient
public class GracefulShutdownTimeoutTestCase {

    private static final String CONTAINER = "default-full-jbossas-byteman";
    private static final String ATTR_NAME = "graceful-shutdown-timeout";

    private static final ModelNode SUBSYSTEM_ADDRESS =
            Operations.createAddress("subsystem", "transactions");

    private static final Path SERVER_LOG = Paths.get(
            System.getenv("JBOSS_HOME"), "standalone", "log", "server.log");

    private static final String BYTEMAN_ADDRESS =
            System.getProperty("byteman.server.ipaddress", Submit.DEFAULT_ADDRESS);
    private static final int BYTEMAN_PORT =
            Integer.getInteger("byteman.server.port", Submit.DEFAULT_PORT);

    private static final String EJB_DEPLOYMENT = "slow-txn-test.jar";
    private static final String PREDESTROY_DEPLOYMENT = "predestroy-txn-test.jar";

    private static final Path TXN_FLAG = Paths.get(
            System.getenv("JBOSS_HOME"), "standalone", "data", "txn-in-commit.flag");

    private static final Path DRAIN_FLAG = Paths.get(
            System.getenv("JBOSS_HOME"), "standalone", "data", "drain-was-called.flag");

    private final Submit bytemanSubmit = new Submit(BYTEMAN_ADDRESS, BYTEMAN_PORT);

    @ArquillianResource
    private ContainerController container;

    @ArquillianResource
    private Deployer deployer;

    @Deployment(name = EJB_DEPLOYMENT, managed = false, testable = false)
    @TargetsContainer(CONTAINER)
    public static Archive<?> createSlowTxnDeployment() {
        return ShrinkWrap.create(JavaArchive.class, EJB_DEPLOYMENT)
                .addClasses(SlowTransactionBean.class, SlowTransactionRemote.class);
    }

    @Deployment(name = PREDESTROY_DEPLOYMENT, managed = false, testable = false)
    @TargetsContainer(CONTAINER)
    public static Archive<?> createPreDestroyDeployment() {
        return ShrinkWrap.create(JavaArchive.class, PREDESTROY_DEPLOYMENT)
                .addClass(TransactionalPreDestroySingletonBean.class);
    }

    private ManagementClient managementClient;
    private long logLineCheckpoint;

    @Before
    public void before() throws Exception {
        if (!container.isStarted(CONTAINER)) {
            container.start(CONTAINER);
        }
        managementClient = createManagementClient();
        logLineCheckpoint = Files.exists(SERVER_LOG) ? countLines(SERVER_LOG) : 0;
    }

    @After
    public void after() throws Exception {
        try {
            Files.deleteIfExists(TXN_FLAG);
            Files.deleteIfExists(DRAIN_FLAG);
            removeRules();
            if (!container.isStarted(CONTAINER)) {
                container.start(CONTAINER);
                if (managementClient != null) {
                    managementClient.close();
                }
                managementClient = createManagementClient();
            }
            safeUndeploy(EJB_DEPLOYMENT);
            safeUndeploy(PREDESTROY_DEPLOYMENT);
            writeAttribute(ATTR_NAME, new ModelNode(-1));
        } finally {
            if (managementClient != null) {
                managementClient.close();
            }
            if (container.isStarted(CONTAINER)) {
                container.stop(CONTAINER);
            }
        }
    }

    /**
     * Verifies that the {@code graceful-shutdown-timeout} attribute defaults to {@code -1}
     * (skip graceful shutdown entirely) when read from a live server.
     */
    @Test
    public void testReadDefaultValue() throws Exception {
        ModelNode result = readAttribute(ATTR_NAME);
        assertEquals(-1, result.asInt());
    }

    /**
     * Verifies that valid values ({@code -1}, {@code 0}, positive integers) can be written
     * at runtime without requiring a server reload ({@code RESTART_NONE} flag).
     */
    @Test
    public void testWriteValidValues() throws Exception {
        writeAttribute(ATTR_NAME, new ModelNode(0));
        assertEquals(0, readAttribute(ATTR_NAME).asInt());

        writeAttribute(ATTR_NAME, new ModelNode(60));
        assertEquals(60, readAttribute(ATTR_NAME).asInt());

        writeAttribute(ATTR_NAME, new ModelNode(-1));
        assertEquals(-1, readAttribute(ATTR_NAME).asInt());
    }

    /**
     * Verifies that the {@code IntRangeValidator(-1)} rejects values below {@code -1}
     * and non-integer types.
     */
    @Test
    public void testWriteInvalidValue() throws Exception {
        ModelNode result = executeOperation(
                Operations.createWriteAttributeOperation(SUBSYSTEM_ADDRESS, ATTR_NAME, new ModelNode(-2)));
        assertFalse("write-attribute(-2) should have failed", isSuccessfulOutcome(result));

        result = executeOperation(
                Operations.createWriteAttributeOperation(SUBSYSTEM_ADDRESS, ATTR_NAME, new ModelNode("abc")));
        assertFalse("write-attribute(\"abc\") should have failed", isSuccessfulOutcome(result));
    }

    /**
     * Verifies that undefining the attribute restores the default value of {@code -1}.
     */
    @Test
    public void testUndefineResetsToDefault() throws Exception {
        writeAttribute(ATTR_NAME, new ModelNode(60));
        assertEquals(60, readAttribute(ATTR_NAME).asInt());

        ModelNode undefineOp = Operations.createUndefineAttributeOperation(SUBSYSTEM_ADDRESS, ATTR_NAME);
        ModelNode result = executeOperation(undefineOp);
        assertTrue("undefine-attribute failed: " + result, isSuccessfulOutcome(result));

        assertEquals(-1, readAttribute(ATTR_NAME).asInt());
    }

    /**
     * Verifies that {@code graceful-shutdown-timeout=-1} skips graceful shutdown entirely.
     * A Byteman rule delays {@code TransactionReaper.waitForAllTxnsToTerminate()} by 10s,
     * but with {@code -1} the method should never be called. A flag file created by the
     * Byteman rule is used to detect invocation.
     */
    @Test
    public void testShutdownWithTimeoutMinusOne() throws Exception {
        writeAttribute(ATTR_NAME, new ModelNode(-1));
        deployDrainRule();

        container.stop(CONTAINER);

        assertFalse("waitForAllTxnsToTerminate should not be called with timeout=-1",
                Files.exists(DRAIN_FLAG));
    }

    /**
     * Verifies that {@code graceful-shutdown-timeout=0} (wait indefinitely) causes the
     * shutdown to invoke {@code TransactionReaper.waitForAllTxnsToTerminate()}.
     * A Byteman rule creates a flag file when the method is entered.
     */
    @Test
    public void testShutdownWithTimeoutZero() throws Exception {
        writeAttribute(ATTR_NAME, new ModelNode(0));
        deployDrainRule();

        container.stop(CONTAINER);

        assertTrue("waitForAllTxnsToTerminate should have been called with timeout=0",
                Files.exists(DRAIN_FLAG));
    }

    /**
     * Verifies that a positive {@code graceful-shutdown-timeout} fires
     * {@code CompletableFuture.orTimeout()} when the drain exceeds the limit.
     * A Byteman rule delays the drain by 10s while the timeout is set to 3s,
     * so the timeout fires first. Asserts that WFLYTX0050 ("graceful shutdown timed out")
     * is logged.
     */
    @Test
    public void testShutdownWithPositiveTimeout() throws Exception {
        writeAttribute(ATTR_NAME, new ModelNode(3));
        deployDrainRule();

        container.stop(CONTAINER);

        assertTrue("waitForAllTxnsToTerminate should have been called with timeout=3",
                Files.exists(DRAIN_FLAG));
        assertTrue("Expected WFLYTX0050 (graceful shutdown timed out) in log",
                hasLogMessageInFile("WFLYTX0050"));
    }

    /**
     * Verifies the full graceful shutdown sequence with a real in-doubt transaction.
     * A Byteman rule delays {@code TwoPhaseCoordinator.end()} by 8s to simulate a slow
     * commit, while {@code graceful-shutdown-timeout=0} makes the server wait indefinitely.
     * Asserts that the shutdown log messages appear in the correct order:
     * WFLYTX0048 (waiting for drain) -> WFLYTX0049 (drain complete) ->
     * WFLYTX0046 (recovery suspension started) -> WFLYTX0047 (recovery suspension complete).
     */
    @Test
    public void testGracefulShutdownWaitsForTransactionAndSuspends() throws Exception {
        writeAttribute(ATTR_NAME, new ModelNode(0));
        deployTxnEndRule();
        deployer.deploy(EJB_DEPLOYMENT);

        try {
            SlowTransactionRemote bean = lookupSlowTransactionBean();

            Thread asyncCall = new Thread(() -> {
                try {
                    bean.doWork();
                } catch (Exception ignored) {
                }
            });
            asyncCall.start();
            waitForTxnInCommit();

            container.stop(CONTAINER);

            assertLogMessagesInOrder("WFLYTX0048", "WFLYTX0049", "WFLYTX0046", "WFLYTX0047");

            asyncCall.join(5000);
        } finally {
            if (container.isStarted(CONTAINER)) {
                deployer.undeploy(EJB_DEPLOYMENT);
            }
        }
    }

    /**
     * Verifies that a positive timeout interrupts the wait for an in-doubt transaction.
     * A Byteman rule delays {@code TwoPhaseCoordinator.end()} by 8s while the timeout is
     * set to 3s, so the timeout fires before the transaction completes. Asserts that
     * WFLYTX0050 ("graceful shutdown timed out") is logged.
     */
    @Test
    public void testGracefulShutdownTimeoutInterruptsWait() throws Exception {
        writeAttribute(ATTR_NAME, new ModelNode(3));
        deployTxnEndRule();
        deployer.deploy(EJB_DEPLOYMENT);

        try {
            SlowTransactionRemote bean = lookupSlowTransactionBean();

            Thread asyncCall = new Thread(() -> {
                try {
                    bean.doWork();
                } catch (Exception ignored) {
                }
            });
            asyncCall.start();
            waitForTxnInCommit();

            container.stop(CONTAINER);

            assertTrue("Expected WFLYTX0050 (Transactions subsystem: graceful shutdown timed out) in log",
                    hasLogMessageInFile("WFLYTX0050"));

            asyncCall.join(5000);
        } finally {
            if (container.isStarted(CONTAINER)) {
                deployer.undeploy(EJB_DEPLOYMENT);
            }
        }
    }

    /**
     * Regression test for WFLY-22035. Verifies that {@code UserTransaction} is still
     * accessible during {@code @PreDestroy} of a {@code @Singleton @Startup} bean, proving
     * that {@code TxControl.disable()} is not called too early during MSC service shutdown.
     * The bean begins and commits a transaction inside {@code @PreDestroy} and writes the
     * result to a marker file. The test asserts the transaction was STATUS_ACTIVE.
     */
    @Test
    public void testPreDestroyCanBeginTransactionDuringShutdown() throws Exception {
        deployer.deploy(PREDESTROY_DEPLOYMENT);

        container.stop(CONTAINER);

        Path resultPath = Paths.get(System.getenv("JBOSS_HOME"),
                "standalone", "data", TransactionalPreDestroySingletonBean.RESULT_FILE_NAME);
        try {
            assertTrue("Result marker file not created by @PreDestroy",
                    Files.exists(resultPath));
            List<String> lines = Files.readAllLines(resultPath);
            assertFalse("Result file is empty", lines.isEmpty());
            if ("EXCEPTION".equals(lines.get(0))) {
                String detail = lines.size() > 1 ? lines.get(1) : "(no detail)";
                fail("@PreDestroy threw an exception — TxControl.disable() " +
                        "may have been called too early (WFLY-22035): " + detail);
            }
            assertEquals("Expected SUCCESS from @PreDestroy transactional work",
                    "SUCCESS", lines.get(0));
            assertTrue("Expected transaction status in result", lines.size() > 1);
            assertEquals("Transaction should have been STATUS_ACTIVE (0) during @PreDestroy",
                    "status=0", lines.get(1));
        } finally {
            container.start(CONTAINER);
            if (managementClient != null) {
                managementClient.close();
            }
            managementClient = createManagementClient();
            deployer.undeploy(PREDESTROY_DEPLOYMENT);
            Files.deleteIfExists(resultPath);
        }
    }

    private void removeRules() {
        try {
            bytemanSubmit.deleteAllRules();
        } catch (Exception ignored) {
        }
    }

    private void safeUndeploy(String deploymentName) {
        try {
            deployer.undeploy(deploymentName);
        } catch (Exception ignored) {
        }
    }

    private void deployDrainRule() throws Exception {
        bytemanSubmit.addRulesFromResources(Collections.singletonList(
                GracefulShutdownTimeoutTestCase.class.getClassLoader()
                        .getResourceAsStream("byteman/GracefulShutdownTimeoutTestCase.btm")));
    }

    private void deployTxnEndRule() throws Exception {
        bytemanSubmit.addRulesFromResources(Collections.singletonList(
                GracefulShutdownTimeoutTestCase.class.getClassLoader()
                        .getResourceAsStream("byteman/GracefulShutdownTimeoutTxnEndTestCase.btm")));
    }

    private SlowTransactionRemote lookupSlowTransactionBean() throws Exception {
        Properties props = new Properties();
        props.put(Context.INITIAL_CONTEXT_FACTORY, "org.wildfly.naming.client.WildFlyInitialContextFactory");
        props.put(Context.PROVIDER_URL, "remote+http://" +
                TestSuiteEnvironment.formatPossibleIpv6Address(
                        TestSuiteEnvironment.getServerAddress()) + ":" +
                TestSuiteEnvironment.getHttpPort());
        props.put("jboss.naming.client.connect.options.org.xnio.Options.SASL_POLICY_NOANONYMOUS", "false");
        InitialContext ctx = new InitialContext(props);
        try {
            return (SlowTransactionRemote) ctx.lookup(
                    "ejb:/" + EJB_DEPLOYMENT.replace(".jar", "") +
                            "/" + SlowTransactionBean.class.getSimpleName() + "!" +
                            SlowTransactionRemote.class.getName());
        } finally {
            ctx.close();
        }
    }

    private void waitForTxnInCommit() throws Exception {
        long deadline = System.currentTimeMillis() + 10_000;
        while (!Files.exists(TXN_FLAG)) {
            if (System.currentTimeMillis() > deadline) {
                fail("Timed out waiting for transaction to enter commit phase");
            }
            Thread.sleep(200);
        }
        Files.delete(TXN_FLAG);
    }

    private ModelNode readAttribute(String name) throws IOException {
        ModelNode op = Operations.createReadAttributeOperation(SUBSYSTEM_ADDRESS, name);
        ModelNode result = executeOperation(op);
        assertTrue("read-attribute failed: " + result, isSuccessfulOutcome(result));
        return Operations.readResult(result);
    }

    private void writeAttribute(String name, ModelNode value) throws IOException {
        ModelNode op = Operations.createWriteAttributeOperation(SUBSYSTEM_ADDRESS, name, value);
        ModelNode result = executeOperation(op);
        assertTrue("write-attribute failed: " + result, isSuccessfulOutcome(result));
    }

    private ModelNode executeOperation(ModelNode op) throws IOException {
        return managementClient.getControllerClient().execute(op);
    }

    private static ManagementClient createManagementClient() throws UnknownHostException {
        return new ManagementClient(
                TestSuiteEnvironment.getModelControllerClient(),
                TestSuiteEnvironment.formatPossibleIpv6Address(
                        TestSuiteEnvironment.getServerAddress()),
                TestSuiteEnvironment.getServerPort(),
                "remote+http");
    }

    private boolean hasLogMessageInFile(String messageId) throws Exception {
        try (Stream<String> lines = Files.lines(SERVER_LOG)) {
            return lines.skip(logLineCheckpoint)
                    .anyMatch(line -> line.contains(messageId));
        }
    }

    private void assertLogMessagesInOrder(String... messageIds) throws Exception {
        List<String> logLines = Files.readAllLines(SERVER_LOG);
        List<String> relevantLines = logLines.subList(
                (int) Math.min(logLineCheckpoint, logLines.size()), logLines.size());
        int lastIndex = -1;
        for (String msgId : messageIds) {
            int foundIndex = -1;
            for (int i = lastIndex + 1; i < relevantLines.size(); i++) {
                if (relevantLines.get(i).contains(msgId)) {
                    foundIndex = i;
                    break;
                }
            }
            assertTrue("Expected " + msgId + " in log after checkpoint but not found",
                    foundIndex >= 0);
            lastIndex = foundIndex;
        }
    }

    private static long countLines(Path path) throws IOException {
        try (Stream<String> lines = Files.lines(path)) {
            return lines.count();
        }
    }
}

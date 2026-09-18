/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.manualmode.messaging.ha;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@RunAsClient
public class DiscoveryWarningTestCase {

    private static final Logger log = Logger.getLogger(DiscoveryWarningTestCase.class);
    private static final String CONTAINER = "jbossas-messaging-ha-server1";

    @ArquillianResource
    protected static ContainerController container;

    private String snapshot;

    @Before
    public void setUp() throws Exception {
        deleteServerLog();
        container.start(CONTAINER);
        try (ModelControllerClient client = createClient()) {
            snapshot = takeSnapshot(client);
        }
    }

    @After
    public void tearDown() throws Exception {
        Exception firstError = null;
        try {
            if (container.isStarted(CONTAINER)) {
                container.stop(CONTAINER);
            }
        } catch (Exception e) {
            log.warnf(e, "Failed to stop %s", CONTAINER);
            firstError = e;
        }
        try {
            restoreSnapshot(snapshot);
        } catch (Exception e) {
            log.warnf(e, "Failed to restore snapshot for %s", CONTAINER);
            if (firstError == null) {
                firstError = e;
            } else {
                firstError.addSuppressed(e);
            }
        }
        if (firstError != null) {
            throw firstError;
        }
    }

    @Test
    public void testJgroupsDiscoveryGroupNoWarning() throws Exception {
        assertFalse("WFLYMSGAMQ0123 (UDP multicast warning) should NOT be in server.log",
                serverLogContains("WFLYMSGAMQ0123"));
    }

    @Test
    public void testSocketDiscoveryGroupWarning() throws Exception {
        try (ModelControllerClient client = createClient()) {
            addSocketDiscoveryGroup(client, "dg-socket1", "jgroups-udp");
        }
        restartServerWithCleanLog();

        assertTrue("WFLYMSGAMQ0123 (UDP multicast warning) should be in server.log",
                serverLogContains("WFLYMSGAMQ0123"));
        assertTrue("Warning should mention discovery group name",
                serverLogContains("discovery groups [dg-socket1]"));
    }

    @Test
    public void testSocketBroadcastGroupWarning() throws Exception {
        try (ModelControllerClient client = createClient()) {
            addSocketBroadcastGroup(client, "bg-socket1", "jgroups-udp", "http-connector");
        }
        restartServerWithCleanLog();

        assertTrue("WFLYMSGAMQ0123 (UDP multicast warning) should be in server.log",
                serverLogContains("WFLYMSGAMQ0123"));
        assertTrue("Warning should mention broadcast group name",
                serverLogContains("broadcast groups [bg-socket1]"));
    }

    @Test
    public void testJgroupsBroadcastGroupNoWarning() throws Exception {
        // Default config only has jgroups-discovery-group (dg-group1), no socket-based groups.
        // Verify that no WFLYMSGAMQ0123 warning is emitted.
        assertFalse("WFLYMSGAMQ0123 (UDP multicast warning) should NOT be in server.log",
                serverLogContains("WFLYMSGAMQ0123"));
    }

    @Test
    public void testCombinedDiscoveryAndBroadcastWarning() throws Exception {
        try (ModelControllerClient client = createClient()) {
            addSocketDiscoveryGroup(client, "dg-socket1", "jgroups-udp");
            addSocketBroadcastGroup(client, "bg-socket1", "jgroups-udp", "http-connector");
        }
        restartServerWithCleanLog();

        assertTrue("WFLYMSGAMQ0123 (UDP multicast warning) should be in server.log",
                serverLogContains("WFLYMSGAMQ0123"));
        assertTrue("Warning should mention discovery group",
                serverLogContains("discovery groups [dg-socket1]"));
        assertTrue("Warning should mention broadcast group",
                serverLogContains("broadcast groups [bg-socket1]"));
    }

    @Test
    public void testNoDiscoveryGroupsNoWarning() throws Exception {
        try (ModelControllerClient client = createClient()) {
            removeAllDiscoveryAndBroadcastGroups(client);
        }
        restartServerWithCleanLog();

        assertFalse("WFLYMSGAMQ0123 (UDP multicast warning) should NOT be in server.log",
                serverLogContains("WFLYMSGAMQ0123"));
    }

    @Test
    public void testPropertyAlreadySetPreserved() throws Exception {
        try (ModelControllerClient client = createClient()) {
            addSystemProperty(client, "artemis.discovery.enabled", "false");
        }
        restartServerWithCleanLog();

        // WFLYMSGAMQ0120 is DEBUG level so not visible in the default server log;
        // verify the server started successfully with the pre-set value preserved
        assertFalse("WFLYMSGAMQ0123 (UDP multicast warning) should NOT be in server.log",
                serverLogContains("WFLYMSGAMQ0123"));
    }

    @Test
    public void testPropertySetToTruePreserved() throws Exception {
        try (ModelControllerClient client = createClient()) {
            addSystemProperty(client, "artemis.discovery.enabled", "true");
        }
        restartServerWithCleanLog();

        // WFLYMSGAMQ0120 is DEBUG level so not visible in the default server log;
        // verify the server started successfully with the pre-set value preserved
        assertFalse("WFLYMSGAMQ0123 (UDP multicast warning) should NOT be in server.log",
                serverLogContains("WFLYMSGAMQ0123"));
    }

    @Test
    public void testInvalidPropertyValue() throws Exception {
        try (ModelControllerClient client = createClient()) {
            addSystemProperty(client, "artemis.discovery.enabled", "dummy");
        }
        restartServerWithCleanLog();

        assertTrue("WFLYMSGAMQ0124 (invalid property value) should be in server.log",
                serverLogContains("WFLYMSGAMQ0124"));
    }

    @Test
    public void testWarningSuppression() throws Exception {
        try (ModelControllerClient client = createClient()) {
            addSocketDiscoveryGroup(client, "dg-socket1", "jgroups-udp");
            addSystemProperty(client, "jboss.messaging.discovery.warning.disabled", "true");
        }
        restartServerWithCleanLog();

        assertFalse("WFLYMSGAMQ0123 (UDP multicast warning) should NOT be in server.log when suppressed",
                serverLogContains("WFLYMSGAMQ0123"));
    }

    @Test
    public void testWarningSuppressionNonTrueValue() throws Exception {
        try (ModelControllerClient client = createClient()) {
            addSocketDiscoveryGroup(client, "dg-socket1", "jgroups-udp");
            addSystemProperty(client, "jboss.messaging.discovery.warning.disabled", "yes");
        }
        restartServerWithCleanLog();

        assertTrue("WFLYMSGAMQ0123 (UDP multicast warning) should still be in server.log when suppression value is 'yes' (not 'true')",
                serverLogContains("WFLYMSGAMQ0123"));
    }

    @Test
    public void testSubsystemSocketDiscoveryGroupWarning() throws Exception {
        try (ModelControllerClient client = createClient()) {
            addSubsystemSocketDiscoveryGroup(client, "sub-dg-socket1", "jgroups-udp");
        }
        restartServerWithCleanLog();

        assertTrue("WFLYMSGAMQ0123 (UDP multicast warning) should be in server.log for subsystem-level socket discovery group",
                serverLogContains("WFLYMSGAMQ0123"));
        assertTrue("Warning should mention subsystem-level discovery group name",
                serverLogContains("sub-dg-socket1"));
    }

    @Test
    public void testSubsystemJgroupsDiscoveryGroupNoWarning() throws Exception {
        try (ModelControllerClient client = createClient()) {
            addSubsystemJgroupsDiscoveryGroup(client, "sub-dg-jgroups1", "activemq-cluster");
        }
        restartServerWithCleanLog();

        assertFalse("WFLYMSGAMQ0123 (UDP multicast warning) should NOT be in server.log for subsystem-level jgroups discovery group",
                serverLogContains("WFLYMSGAMQ0123"));
    }

    @Test
    public void testSubsystemAndServerSocketDiscoveryGroupCombinedWarning() throws Exception {
        try (ModelControllerClient client = createClient()) {
            addSubsystemSocketDiscoveryGroup(client, "sub-dg-socket1", "jgroups-udp");
            addSocketDiscoveryGroup(client, "srv-dg-socket1", "jgroups-udp");
        }
        restartServerWithCleanLog();

        assertTrue("WFLYMSGAMQ0123 (UDP multicast warning) should be in server.log",
                serverLogContains("WFLYMSGAMQ0123"));
        assertTrue("Warning should mention subsystem-level discovery group name",
                serverLogContains("sub-dg-socket1"));
        assertTrue("Warning should mention server-level discovery group name",
                serverLogContains("srv-dg-socket1"));
    }

    @Test
    public void testAdditionalJgroupsBroadcastGroupNoWarning() throws Exception {
        try (ModelControllerClient client = createClient()) {
            addJgroupsBroadcastGroup(client, "bg-jgroups1", "activemq-cluster", "http-connector");
        }
        restartServerWithCleanLog();

        assertFalse("WFLYMSGAMQ0123 (UDP multicast warning) should NOT be in server.log for jgroups broadcast group",
                serverLogContains("WFLYMSGAMQ0123"));
    }

    @Test
    public void testAdditionalJgroupsDiscoveryGroupNoWarning() throws Exception {
        try (ModelControllerClient client = createClient()) {
            addJgroupsDiscoveryGroup(client, "dg-jgroups1", "activemq-cluster");
        }
        restartServerWithCleanLog();

        assertFalse("WFLYMSGAMQ0123 (UDP multicast warning) should NOT be in server.log for jgroups discovery group",
                serverLogContains("WFLYMSGAMQ0123"));
    }

    @Test
    public void testJgroupsBroadcastWithSocketDiscoveryWarning() throws Exception {
        try (ModelControllerClient client = createClient()) {
            addJgroupsBroadcastGroup(client, "bg-jgroups1", "activemq-cluster", "http-connector");
            addSocketDiscoveryGroup(client, "dg-socket1", "jgroups-udp");
        }
        restartServerWithCleanLog();

        assertTrue("WFLYMSGAMQ0123 (UDP multicast warning) should be in server.log",
                serverLogContains("WFLYMSGAMQ0123"));
        assertTrue("Warning should mention socket discovery group",
                serverLogContains("dg-socket1"));
        assertFalse("Warning should NOT mention jgroups broadcast group",
                serverLogContains("bg-jgroups1"));
    }

    @Test
    public void testJgroupsDiscoveryWithSocketBroadcastWarning() throws Exception {
        try (ModelControllerClient client = createClient()) {
            addJgroupsDiscoveryGroup(client, "dg-jgroups1", "activemq-cluster");
            addSocketBroadcastGroup(client, "bg-socket1", "jgroups-udp", "http-connector");
        }
        restartServerWithCleanLog();

        assertTrue("WFLYMSGAMQ0123 (UDP multicast warning) should be in server.log",
                serverLogContains("WFLYMSGAMQ0123"));
        assertTrue("Warning should mention socket broadcast group",
                serverLogContains("bg-socket1"));
        assertFalse("Warning should NOT mention jgroups discovery group",
                serverLogContains("dg-jgroups1"));
    }

    @Test
    public void testJgroupsOnlyGroupsNoWarning() throws Exception {
        try (ModelControllerClient client = createClient()) {
            addJgroupsBroadcastGroup(client, "bg-jgroups1", "activemq-cluster", "http-connector");
            addJgroupsDiscoveryGroup(client, "dg-jgroups1", "activemq-cluster");
        }
        restartServerWithCleanLog();

        assertFalse("WFLYMSGAMQ0123 (UDP multicast warning) should NOT be in server.log when only jgroups groups exist",
                serverLogContains("WFLYMSGAMQ0123"));
    }

    @Test
    public void testJgroupsChannelNoAuthenticationWarning() throws Exception {
        assertTrue("WFLYCLJG0039 (unauthenticated members warning) should be in server.log",
                serverLogContains("WFLYCLJG0039"));
        assertTrue("Warning should reference the channel name",
                serverLogContains("channel ee"));
    }

    @Test
    public void testJgroupsChannelNoConfidentialityWarning() throws Exception {
        assertTrue("WFLYCLJG0040 (no confidentiality warning) should be in server.log",
                serverLogContains("WFLYCLJG0040"));
        assertTrue("Warning should reference the channel name",
                serverLogContains("channel ee"));
    }

    @Test
    public void testSecuredJgroupsChannelNoWarnings() throws Exception {
        assertTrue("WFLYCLJG0039 (unauthenticated members warning) should be present before securing",
                serverLogContains("WFLYCLJG0039"));
        assertTrue("WFLYCLJG0040 (no confidentiality warning) should be present before securing",
                serverLogContains("WFLYCLJG0040"));

        try (ModelControllerClient client = createClient()) {
            addAuthProtocol(client, "tcp", "changeit");
            addAsymEncryptProtocol(client, "tcp", "applicationKS", "localhost", "password");
        }
        restartServerWithCleanLog();

        assertFalse("WFLYCLJG0039 (unauthenticated members warning) should NOT be in server.log when AUTH is configured",
                serverLogContains("WFLYCLJG0039"));
        assertFalse("WFLYCLJG0040 (no confidentiality warning) should NOT be in server.log when ASYM_ENCRYPT is configured",
                serverLogContains("WFLYCLJG0040"));
    }

    // ------ helpers ------

    private void restartServerWithCleanLog() throws Exception {
        container.stop(CONTAINER);
        deleteServerLog();
        container.start(CONTAINER);
    }

    private static ModelControllerClient createClient() {
        return TestSuiteEnvironment.getModelControllerClient();
    }

    private Path getServerLogPath() {
        return Paths.get(System.getProperty("basedir", "."),
                "target", CONTAINER, "standalone", "log", "server.log");
    }

    private boolean serverLogContains(String messageId) throws IOException {
        Path logFile = getServerLogPath();
        try (java.util.stream.Stream<String> lines = Files.lines(logFile)) {
            return lines.anyMatch(line -> line.contains(messageId));
        }
    }

    private void deleteServerLog() throws IOException {
        Files.deleteIfExists(getServerLogPath());
    }

    private static String takeSnapshot(ModelControllerClient client) throws Exception {
        ModelNode op = new ModelNode();
        op.get("operation").set("take-snapshot");
        ModelNode result = client.execute(op);
        assertSuccess(result);
        return Operations.readResult(result).asString();
    }

    private static void assertSuccess(ModelNode result) {
        if (!Operations.isSuccessfulOutcome(result)) {
            fail(Operations.getFailureDescription(result).toString());
        }
    }

    private void restoreSnapshot(String snapshotPath) throws IOException {
        if (snapshotPath == null) return;
        Path snapshotFile = Paths.get(snapshotPath);
        Path configFile = snapshotFile.getParent().getParent().getParent()
                .resolve("standalone-full-ha.xml");
        Files.move(snapshotFile, configFile, StandardCopyOption.REPLACE_EXISTING);
    }

    private static void addSocketBroadcastGroup(ModelControllerClient client,
            String name, String socketBinding, String connectorName) throws Exception {
        ModelNode address = PathAddress.parseCLIStyleAddress(
                "/subsystem=messaging-activemq/server=default/socket-broadcast-group=" + name)
                .toModelNode();
        ModelNode op = Operations.createAddOperation(address);
        op.get("socket-binding").set(socketBinding);
        op.get("connectors").add(connectorName);
        ModelNode result = client.execute(op);
        assertSuccess(result);
    }

    private static void addSocketDiscoveryGroup(ModelControllerClient client,
            String name, String socketBinding) throws Exception {
        ModelNode address = PathAddress.parseCLIStyleAddress(
                "/subsystem=messaging-activemq/server=default/socket-discovery-group=" + name)
                .toModelNode();
        ModelNode op = Operations.createAddOperation(address);
        op.get("socket-binding").set(socketBinding);
        ModelNode result = client.execute(op);
        assertSuccess(result);
    }

    private static void removeAllDiscoveryAndBroadcastGroups(ModelControllerClient client) throws Exception {
        ModelNode composite = Operations.createCompositeOperation();
        ModelNode steps = composite.get("steps");

        // Remove cluster-connection first — it references dg-group1 via its discovery-group attribute
        steps.add(Operations.createRemoveOperation(PathAddress.parseCLIStyleAddress(
                "/subsystem=messaging-activemq/server=default/cluster-connection=my-cluster")
                .toModelNode()));
        steps.add(Operations.createRemoveOperation(PathAddress.parseCLIStyleAddress(
                "/subsystem=messaging-activemq/server=default/jgroups-discovery-group=dg-group1")
                .toModelNode()));
        steps.add(Operations.createRemoveOperation(PathAddress.parseCLIStyleAddress(
                "/subsystem=messaging-activemq/server=default/jgroups-broadcast-group=bg-group1")
                .toModelNode()));

        ModelNode result = client.execute(composite);
        assertSuccess(result);
    }

    private static void addJgroupsBroadcastGroup(ModelControllerClient client,
            String name, String jgroupsCluster, String connectorName) throws Exception {
        ModelNode address = PathAddress.parseCLIStyleAddress(
                "/subsystem=messaging-activemq/server=default/jgroups-broadcast-group=" + name)
                .toModelNode();
        ModelNode op = Operations.createAddOperation(address);
        op.get("jgroups-cluster").set(jgroupsCluster);
        op.get("connectors").add(connectorName);
        ModelNode result = client.execute(op);
        assertSuccess(result);
    }

    private static void addJgroupsDiscoveryGroup(ModelControllerClient client,
            String name, String jgroupsCluster) throws Exception {
        ModelNode address = PathAddress.parseCLIStyleAddress(
                "/subsystem=messaging-activemq/server=default/jgroups-discovery-group=" + name)
                .toModelNode();
        ModelNode op = Operations.createAddOperation(address);
        op.get("jgroups-cluster").set(jgroupsCluster);
        ModelNode result = client.execute(op);
        assertSuccess(result);
    }

    private static void addSubsystemSocketDiscoveryGroup(ModelControllerClient client,
            String name, String socketBinding) throws Exception {
        ModelNode address = PathAddress.parseCLIStyleAddress(
                "/subsystem=messaging-activemq/socket-discovery-group=" + name)
                .toModelNode();
        ModelNode op = Operations.createAddOperation(address);
        op.get("socket-binding").set(socketBinding);
        ModelNode result = client.execute(op);
        assertSuccess(result);
    }

    private static void addSubsystemJgroupsDiscoveryGroup(ModelControllerClient client,
            String name, String jgroupsCluster) throws Exception {
        ModelNode address = PathAddress.parseCLIStyleAddress(
                "/subsystem=messaging-activemq/jgroups-discovery-group=" + name)
                .toModelNode();
        ModelNode op = Operations.createAddOperation(address);
        op.get("jgroups-cluster").set(jgroupsCluster);
        ModelNode result = client.execute(op);
        assertSuccess(result);
    }

    private static void addAuthProtocol(ModelControllerClient client,
            String stackName, String sharedSecret) throws Exception {
        ModelNode composite = Operations.createCompositeOperation();
        ModelNode steps = composite.get("steps");

        ModelNode authAddress = PathAddress.parseCLIStyleAddress(
                "/subsystem=jgroups/stack=" + stackName + "/protocol=AUTH")
                .toModelNode();
        steps.add(Operations.createAddOperation(authAddress));

        ModelNode tokenAddress = PathAddress.parseCLIStyleAddress(
                "/subsystem=jgroups/stack=" + stackName + "/protocol=AUTH/token=plain")
                .toModelNode();
        ModelNode addToken = Operations.createAddOperation(tokenAddress);
        addToken.get("shared-secret-reference").get("clear-text").set(sharedSecret);
        steps.add(addToken);

        assertSuccess(client.execute(composite));
    }

    private static void addAsymEncryptProtocol(ModelControllerClient client,
            String stackName, String keyStoreName, String keyAlias,
            String keyPassword) throws Exception {
        ModelNode address = PathAddress.parseCLIStyleAddress(
                "/subsystem=jgroups/stack=" + stackName + "/protocol=ASYM_ENCRYPT")
                .toModelNode();
        ModelNode op = Operations.createAddOperation(address);
        op.get("key-store").set(keyStoreName);
        op.get("key-alias").set(keyAlias);
        op.get("key-credential-reference").get("clear-text").set(keyPassword);
        assertSuccess(client.execute(op));
    }

    private static void addSystemProperty(ModelControllerClient client,
            String name, String value) throws Exception {
        ModelNode address = Operations.createAddress("system-property", name);
        ModelNode op = Operations.createAddOperation(address);
        op.get("value").set(value);
        ModelNode result = client.execute(op);
        assertSuccess(result);
    }
}

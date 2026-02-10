/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.messaging.mgmt;

import org.jboss.as.test.integration.messaging.jms.deployment.MultipleConnectionsQueueMDB;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INCLUDE_DEFAULTS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INCLUDE_RUNTIME;
import static org.jboss.shrinkwrap.api.ShrinkWrap.create;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.common.jms.JMSOperations;
import org.jboss.as.test.integration.common.jms.JMSOperationsProvider;
import org.jboss.as.test.integration.messaging.jms.deployment.SharedQueueMDB;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.shaded.org.awaitility.Awaitility;

/**
 * Tests the management API for messaging server operations with detailed response validation.
 *
 * @author <a href="mailto:ehugonne@redhat.com">Emmanuel Hugonnet</a>
 */
@RunAsClient()
@RunWith(Arquillian.class)
@ServerSetup(ServerManagementOperationsTestCase.MessagingResourcesSetupTask.class)
public class ServerManagementOperationsTestCase {

    @ContainerResource
    private ManagementClient managementClient;

    @ArquillianResource
    Deployer deployer;

    public static final String QUEUE_LOOKUP = "java:/jms/ServerManagementOperationsTestCase/myQueue";
    private static final String QUEUE_NAME = "myQueue";

    static class MessagingResourcesSetupTask implements ServerSetupTask {

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            JMSOperations jmsOperations = JMSOperationsProvider.getInstance(managementClient.getControllerClient());
            jmsOperations.createJmsQueue(QUEUE_NAME, QUEUE_LOOKUP);
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            JMSOperations jmsOperations = JMSOperationsProvider.getInstance(managementClient.getControllerClient());
            jmsOperations.removeJmsQueue(QUEUE_NAME);
        }
    }

    @Deployment(name = "MULTIPLE", testable = false, managed = false)
    public static WebArchive createArchive() {
        return create(WebArchive.class, "ServerManagementOperationsTestCase.war")
                .addClasses(MultipleConnectionsQueueMDB.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Deployment(name = "SHARED", testable = false, managed = false)
    public static WebArchive createSharedArchive() {
        return create(WebArchive.class, "SharedServerManagementOperationsTestCase.war")
                .addClasses(SharedQueueMDB.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Test
    public void testReadServerResourceDetailed() throws Exception {
        ModelNode result = execute(getServerOperation("read-resource"), true);
        assertTrue(result.isDefined());

        // Verify critical configuration attributes
        assertTrue("persistence-enabled should be defined", result.hasDefined("persistence-enabled"));
        assertTrue("journal-type should be defined", result.hasDefined("journal-type"));
        assertTrue("journal-file-size should be defined", result.hasDefined("journal-file-size"));
        assertTrue("journal-min-files should be defined", result.hasDefined("journal-min-files"));

        // Verify types
        assertEquals(ModelType.BOOLEAN, result.get("persistence-enabled").getType());
        assertEquals(ModelType.STRING, result.get("journal-type").getType());
        assertEquals(ModelType.LONG, result.get("journal-file-size").getType());
        assertEquals(ModelType.INT, result.get("journal-min-files").getType());

        // Verify exact default values from WildFly configuration
        assertTrue("persistence-enabled should be true by default",
                result.get("persistence-enabled").asBoolean());

        assertEquals("journal-type should default to ASYNCIO",
                "ASYNCIO", result.get("journal-type").asString());

        assertEquals("journal-file-size should default to 10MB (10485760 bytes)",
                10485760L, result.get("journal-file-size").asLong());

        assertEquals("journal-min-files should default to 2",
                2, result.get("journal-min-files").asInt());
    }

    @Test
    public void testReadServerResourceWithRuntimeDetailed() throws Exception {
        final ModelNode readOp = getServerOperation("read-resource");
        readOp.get(INCLUDE_RUNTIME).set(true);
        ModelNode result = execute(readOp, true);
        assertTrue(result.isDefined());

        // Verify runtime attributes
        assertTrue("active should be defined", result.hasDefined("active"));
        assertTrue("started should be defined", result.hasDefined("started"));

        // Verify runtime attribute types
        assertEquals(ModelType.BOOLEAN, result.get("active").getType());
        assertEquals(ModelType.BOOLEAN, result.get("started").getType());

        // Verify the server is started
        boolean started = result.get("started").asBoolean();
        assertTrue("Server should be started", started);
    }

    @Test
    public void testReadResourceWithDefaults() throws Exception {
        final ModelNode readOp = getServerOperation("read-resource");
        readOp.get(INCLUDE_DEFAULTS).set(true);
        ModelNode result = execute(readOp, true);

        assertTrue(result.isDefined());

        // Verify default values are included
        assertTrue("thread-pool-max-size should have default", result.hasDefined("thread-pool-max-size"));
        assertTrue("scheduled-thread-pool-max-size should have default", result.hasDefined("scheduled-thread-pool-max-size"));
        assertTrue("security-enabled should have default", result.hasDefined("security-enabled"));

        // Verify exact default values from WildFly configuration
        assertEquals("thread-pool-max-size should default to 30",
                30, result.get("thread-pool-max-size").asInt());

        assertEquals("scheduled-thread-pool-max-size should default to 5",
                5, result.get("scheduled-thread-pool-max-size").asInt());

        assertTrue("security-enabled should default to true",
                result.get("security-enabled").asBoolean());

        // Verify journal defaults are consistent
        assertEquals("journal-file-size should be 10485760 bytes",
                10485760L, result.get("journal-file-size").asLong());

        assertEquals("journal-min-files should be 2",
                2, result.get("journal-min-files").asInt());

        assertEquals("journal-type should be ASYNCIO",
                "ASYNCIO", result.get("journal-type").asString());
    }

    @Test
    public void testReadMultipleAttributes() throws Exception {
        // Test persistence-enabled - should be true by default
        ModelNode readAttr = getServerOperation("read-attribute");
        readAttr.get("name").set("persistence-enabled");
        ModelNode result = execute(readAttr, true);
        assertTrue(result.isDefined());
        assertEquals(ModelType.BOOLEAN, result.getType());
        assertTrue("persistence-enabled should be true", result.asBoolean());

        // Test journal-type - should be ASYNCIO by default
        readAttr = getServerOperation("read-attribute");
        readAttr.get("name").set("journal-type");
        result = execute(readAttr, true);
        assertTrue(result.isDefined());
        assertEquals(ModelType.STRING, result.getType());
        assertEquals("journal-type should be ASYNCIO", "ASYNCIO", result.asString());

        // Test journal-file-size - should be 10MB (10485760 bytes) by default
        readAttr = getServerOperation("read-attribute");
        readAttr.get("name").set("journal-file-size");
        result = execute(readAttr, true);
        assertTrue(result.isDefined());
        assertEquals(ModelType.LONG, result.getType());
        assertEquals("journal-file-size should be 10485760 bytes", 10485760L, result.asLong());

        // Test journal-min-files - should be 2 by default
        readAttr = getServerOperation("read-attribute");
        readAttr.get("name").set("journal-min-files");
        result = execute(readAttr, true);
        assertTrue(result.isDefined());
        assertEquals(ModelType.INT, result.getType());
        assertEquals("journal-min-files should be 2", 2, result.asInt());

        // Test thread-pool-max-size - should be 30 by default
        readAttr = getServerOperation("read-attribute");
        readAttr.get("name").set("thread-pool-max-size");
        result = execute(readAttr, true);
        assertTrue(result.isDefined());
        assertEquals(ModelType.INT, result.getType());
        assertEquals("thread-pool-max-size should be 30", 30, result.asInt());
    }

    @Test
    public void testListConnectionsAsJsonDetailed() throws Exception {
        deployer.deploy("MULTIPLE");
        try {
            // Wait for the MDB pool to initialize
            // MDB instances are created on-demand as messages arrive
            // The pool creates instances up to the configured size (default 15)
            Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> {
                ModelNode result = execute(getServerOperation("list-connections-as-json"), true);
                return Json.createReader(new StringReader(result.asString())).readArray().size() >= 15;
            });
            ModelNode result = execute(getServerOperation("list-connections-as-json"), true);

            String jsonString = result.asString();
            JsonArray connections = Json.createReader(new StringReader(jsonString)).readArray();

            // Count connections by counting "connectionID" occurrences
            int connectionCount = connections.size();

            // MultipleConnectionsQueueMDB is configured with maxSession=15
            // This creates 15 concurrent sessions, each with its own connection
            // Additionally, 1 connection may be created during MDB initialization
            // Validate we have at least 15 remoting connections (typically 15 or 16)
            assertTrue("MDB with maxSession=15 should create at least 15 connections, found: " + connectionCount,
                    connectionCount >= 15);
            // Count connections with active sessions (sessionCount > 0)
            int activeSessionConnections = connections.stream().mapToInt(connection -> connection.asJsonObject().getInt("sessionCount")).sum();
            assertEquals("Should have exactly 15 connections with active sessions",
                    15, activeSessionConnections);
            List<JsonObject> remoteConnections = connections.stream()
                    .map(connection -> connection.asJsonObject())
                    .filter(connection -> "RemotingConnectionImpl".equals(connection.getString("implementation")) && connection.getInt("sessionCount") > 0)
                    .collect(Collectors.toList());
            assertEquals("Each connection should have sessionCount field",
                    15, remoteConnections.size());
        } finally {
            deployer.undeploy("MULTIPLE");
        }
    }

    @Test
    public void testSingleListConnectionsAsJsonDetailed() throws Exception {
        deployer.deploy("SHARED");
        try {
            Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> {
                ModelNode result = execute(getServerOperation("list-connections-as-json"), true);
                return Json.createReader(new StringReader(result.asString())).readArray().size() >= 1;
            });
            // Wait a bit for the MDB to initialize and create connections
            ModelNode result = execute(getServerOperation("list-connections-as-json"), true);
            assertTrue(result.isDefined());
            assertEquals(ModelType.STRING, result.getType());

            String jsonString = result.asString();
            assertNotNull("JSON should not be null", jsonString);
            JsonArray connections = Json.createReader(new StringReader(jsonString)).readArray();

            // Count connections by counting "connectionID" occurrences
            int connectionCount = connections.size();
            // SharedQueueMDB uses singleConnection=true, so it should create exactly 1 connection
            assertEquals("SharedQueueMDB with singleConnection=true should create exactly 1 connection",
                    1, connectionCount);
            JsonObject connection = connections.get(0).asJsonObject();

            // Validate the single connection has expected fields
            assertTrue("Connection should have connectionID", connection.containsKey("connectionID"));
            assertTrue("Connection should have clientAddress", connection.containsKey("clientAddress"));
            assertTrue("Connection should have creationTime", connection.containsKey("creationTime"));

            // Validate the connection's session state
            // For SharedQueueMDB with singleConnection=true:
            // - sessionCount:0 = idle connection (no messages being processed) - this is normal
            // - sessionCount:1+ = active sessions processing messages
            assertTrue("Connection should have sessionCount field", connection.containsKey("sessionCount"));

            int activeSessionConnections = connection.getInt("sessionCount");
            // The single connection is idle
            assertEquals("Should have exactly 1 connection (either active or idle)", 0, activeSessionConnections);
        } finally {
            deployer.undeploy("SHARED");
        }
    }

    @Test
    public void testServerConfigurationDefaults() throws Exception {
        final ModelNode readOp = getServerOperation("read-resource");
        readOp.get(INCLUDE_DEFAULTS).set(true);
        ModelNode result = execute(readOp, true);

        assertTrue(result.isDefined());

        // Journal Configuration Defaults
        assertEquals("journal-type default", "ASYNCIO", result.get("journal-type").asString());
        assertEquals("journal-file-size default", 10485760L, result.get("journal-file-size").asLong());
        assertEquals("journal-min-files default", 2, result.get("journal-min-files").asInt());
        assertTrue("persistence-enabled default", result.get("persistence-enabled").asBoolean());

        // Thread Pool Defaults
        assertEquals("thread-pool-max-size default", 30, result.get("thread-pool-max-size").asInt());
        assertEquals("scheduled-thread-pool-max-size default", 5, result.get("scheduled-thread-pool-max-size").asInt());

        // Security Default
        assertTrue("security-enabled default", result.get("security-enabled").asBoolean());

        // Verify additional important defaults exist
        assertTrue("journal-compact-min-files should be defined",
                result.hasDefined("journal-compact-min-files"));
        assertTrue("journal-compact-percentage should be defined",
                result.hasDefined("journal-compact-percentage"));

        // Verify journal compact percentage is an integer between 0-100
        int compactPercentage = result.get("journal-compact-percentage").asInt();
        assertTrue("journal-compact-percentage should be between 0-100",
                compactPercentage >= 0 && compactPercentage <= 100);
    }

    @Test
    public void testRuntimeStateConsistency() throws Exception {
        // Read resource with runtime to get current state
        final ModelNode readOp = getServerOperation("read-resource");
        readOp.get(INCLUDE_RUNTIME).set(true);
        ModelNode result = execute(readOp, true);

        // If server is started, it should also be active
        boolean started = result.get("started").asBoolean();
        boolean active = result.get("active").asBoolean();

        if (started) {
            assertTrue("Active server should be started", active);
        }

        // Verify version is defined and not empty
        if (result.hasDefined("version")) {
            String version = result.get("version").asString();
            assertNotNull("Version should not be null", version);
            assertFalse("Version should not be empty", version.isEmpty());
            assertTrue("Version should contain digits", version.matches(".*\\d.*"));
        }
    }

    private ModelNode getServerOperation(String operationName) {
        final ModelNode address = new ModelNode();
        address.add("subsystem", "messaging-activemq");
        address.add("server", "default");
        return org.jboss.as.controller.operations.common.Util.getEmptyOperation(operationName, address);
    }

    private ModelNode execute(final ModelNode op, final boolean expectSuccess) throws IOException {
        ModelNode response = managementClient.getControllerClient().execute(op);
        final String outcome = response.get("outcome").asString();
        if (expectSuccess) {
            Assert.assertEquals("Operation failed: " + response, "success", outcome);
            return response.get("result");
        } else {
            Assert.assertEquals("failed", outcome);
            return response.get("failure-description");
        }
    }

    /**
     * Counts the number of occurrences of a substring in a string.
     */
    private int countOccurrences(String str, String substring) {
        if (str == null || substring == null || substring.isEmpty()) {
            return 0;
        }
        int count = 0;
        int index = 0;
        while ((index = str.indexOf(substring, index)) != -1) {
            count++;
            index += substring.length();
        }
        return count;
    }
}

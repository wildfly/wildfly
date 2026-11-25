/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.manualmode.messaging;

import static org.jboss.as.controller.client.helpers.Operations.isSuccessfulOutcome;

import java.io.IOException;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.cli.Util;
import org.jboss.as.controller.client.helpers.Operations;

import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;

import org.apache.activemq.artemis.core.server.NetworkHealthCheck;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.common.jms.JMSOperations;
import org.jboss.as.test.integration.common.jms.JMSOperationsProvider;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.as.test.shared.TestLogHandlerSetupTask;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.as.test.shared.logging.LoggingUtil;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 *
 * @author Emmanuel Hugonnet (c) 2020 Red Hat, Inc.
 */
@RunAsClient()
@RunWith(Arquillian.class)
public class NetworkHealthTestCase {

    private static Logger log = Logger.getLogger(NetworkHealthTestCase.class.getName());

    private static final String DEFAULT_FULL_JBOSSAS = "default-full-jbossas";
    private static final String IP_ADDRESS = "192.0.2.0";

    @ArquillianResource
    protected static ContainerController container;
    private LoggerSetup loggerSetup;
    private ManagementClient managementClient;

    @Before
    public void setup() throws Exception {
        if (!container.isStarted(DEFAULT_FULL_JBOSSAS)) {
            container.start(DEFAULT_FULL_JBOSSAS);
        }
        loggerSetup = new LoggerSetup();
        managementClient = createManagementClient();
        loggerSetup.setup(managementClient, DEFAULT_FULL_JBOSSAS);
    }

    @After
    public void tearDown() throws Exception {
        try {
            JMSOperations jmsOperations = JMSOperationsProvider.getInstance(managementClient.getControllerClient());
            revertTestChanges(jmsOperations.getServerAddress());
        } catch (Exception e) {
            // don't fail, let's try to do the rest of cleanup
            log.warn("Failed to revert test changes", e);
        }
        loggerSetup.tearDown(managementClient, DEFAULT_FULL_JBOSSAS);
        managementClient.close();
        container.stop(DEFAULT_FULL_JBOSSAS);
    }

    /**
     * Defines a test IP to ping so that it fails and ensure that the logs contains the error message.
     * Re-defines the IP to be pinged to the current server address and checks that no such error message occurs.
     * @throws Exception.
     */
    @Test
    public void testNetworkUnHealthyNetwork() throws Exception {
        JMSOperations jmsOperations = JMSOperationsProvider.getInstance(managementClient.getControllerClient());

        ModelNode op;
        if(TestSuiteEnvironment.isWindows()) {
            // The default ping commands in the AttributeDefinitions are meant for *nix based OSs and don't work,
            // on Windows, so on Windows use the default *Artemis* values (from their NetworkHealthCheck class).
            // The values of the Artemis defaults vary at runtime based on the OS, so we'll get the ones they think are best for Windows.
            // Doing this also serves to test their current values (vs us hard coding something here).
            op = Operations.createWriteAttributeOperation(jmsOperations.getServerAddress(), "network-check-ping-command", NetworkHealthCheck.IPV4_DEFAULT_COMMAND);
            executeOperationForSuccess(managementClient, op);
            op = Operations.createWriteAttributeOperation(jmsOperations.getServerAddress(), "network-check-ping6-command", NetworkHealthCheck.IPV6_DEFAULT_COMMAND);
            executeOperationForSuccess(managementClient, op);
        } // else .... for non-Windows for the ping commands we leave the config as-is and test the default AD values

        op = Operations.createWriteAttributeOperation(jmsOperations.getServerAddress(), "network-check-list", IP_ADDRESS);
        executeOperationForSuccess(managementClient, op);
        op = Operations.createWriteAttributeOperation(jmsOperations.getServerAddress(), "network-check-period", 1000);
        executeOperationForSuccess(managementClient, op);
        op = Operations.createWriteAttributeOperation(jmsOperations.getServerAddress(), "network-check-timeout", 200);
        executeOperationForSuccess(managementClient, op);
        Path logFile = LoggingUtil.getLogPath(managementClient.getControllerClient(), "file-handler", "artemis-log");
        managementClient.close();
        container.stop(DEFAULT_FULL_JBOSSAS);
        container.start(DEFAULT_FULL_JBOSSAS);
        managementClient = createManagementClient();
        Thread.sleep(TimeoutUtil.adjust(2000));

        if(TestSuiteEnvironment.isWindows()) {
            // Until Artemis is upgraded to 2.22.0 which contains the fix.
            // @see ARTEMIS-3803 / ARTEMIS-3799
            Assert.assertTrue("Log should contains ActiveMQ ping error log message: [AMQ201001]", LoggingUtil.hasLogMessage(managementClient.getControllerClient(), "artemis-log", "AMQ201001", (line) -> line.contains("name=default")));
        } else {
            Assert.assertTrue("Log should contains ActiveMQ ping error log message: [AMQ202002]", LoggingUtil.hasLogMessage(managementClient.getControllerClient(), "artemis-log", "AMQ202002", (line) -> line.contains(IP_ADDRESS)));
        }
        Assert.assertFalse("Broker should be stopped", isBrokerActive(jmsOperations, managementClient));

        String ipAddress = TestSuiteEnvironment.getServerAddress();
        if(ipAddress.charAt(0) == '[') {
            ipAddress = ipAddress.substring(1, ipAddress.lastIndexOf(']'));
        }
        op = Operations.createWriteAttributeOperation(jmsOperations.getServerAddress(), "network-check-list", ipAddress);
        executeOperationForSuccess(managementClient, op);
        managementClient.close();
        container.stop(DEFAULT_FULL_JBOSSAS);

        long restartLine = LoggingUtil.countLines(logFile);

        container.start(DEFAULT_FULL_JBOSSAS);
        managementClient = createManagementClient();
        Thread.sleep(TimeoutUtil.adjust(2000));

        LoggingUtil.dumpTestLog(managementClient.getControllerClient(), "artemis-log");
        Assert.assertFalse("Log contains ActiveMQ ping error log message: [AMQ202002]",
                LoggingUtil.hasLogMessage(managementClient.getControllerClient(), "artemis-log", "AMQ202002", restartLine, (line) -> line.contains(IP_ADDRESS)));
        Assert.assertTrue("Broker should be running", isBrokerActive(jmsOperations, managementClient));
    }

    /**
     * Defines an unexisting ping command to check that this creates a proper error message.
     * @throws Exception.
     */
    @Test
    public void testPingCommandError() throws Exception {
        JMSOperations jmsOperations = JMSOperationsProvider.getInstance(managementClient.getControllerClient());
        ModelNode op = Operations.createWriteAttributeOperation(jmsOperations.getServerAddress(), "network-check-ping-command", "not_existing_command");
        executeOperationForSuccess(managementClient, op);
        op = Operations.createWriteAttributeOperation(jmsOperations.getServerAddress(), "network-check-ping6-command", "not_existing_command");
        executeOperationForSuccess(managementClient, op);
        op = Operations.createWriteAttributeOperation(jmsOperations.getServerAddress(), "network-check-list", IP_ADDRESS);
        executeOperationForSuccess(managementClient, op);
        op = Operations.createWriteAttributeOperation(jmsOperations.getServerAddress(), "network-check-period", 1000);
        executeOperationForSuccess(managementClient, op);
        op = Operations.createWriteAttributeOperation(jmsOperations.getServerAddress(), "network-check-timeout", 200);
        executeOperationForSuccess(managementClient, op);
        managementClient.close();
        container.stop(DEFAULT_FULL_JBOSSAS);
        container.start(DEFAULT_FULL_JBOSSAS);
        managementClient = createManagementClient();
        Thread.sleep(TimeoutUtil.adjust(2000));
        Assert.assertTrue("Log should contains ActiveMQ ping error log message: [AMQ202007]",
                LoggingUtil.hasLogMessage(managementClient.getControllerClient(), "artemis-log", "",
                        (line) -> (line.contains(" AMQ202007") && line.contains(IP_ADDRESS) && line.contains("java.io.IOException")) || line.contains("AMQ201001")));

        Assert.assertFalse("Broker should be stopped", isBrokerActive(jmsOperations, managementClient));
    }

    private void revertTestChanges(ModelNode serverAddress) throws IOException, MgmtOperationException {
        ModelNode op;
        op = Operations.createUndefineAttributeOperation(serverAddress, "network-check-list");
        executeOperationForSuccess(managementClient, op);
        op = Operations.createUndefineAttributeOperation(serverAddress, "network-check-period");
        executeOperationForSuccess(managementClient, op);
        op = Operations.createUndefineAttributeOperation(serverAddress, "network-check-timeout");
        executeOperationForSuccess(managementClient, op);
        op = Operations.createUndefineAttributeOperation(serverAddress, "network-check-ping-command");
        executeOperationForSuccess(managementClient, op);
        op = Operations.createUndefineAttributeOperation(serverAddress, "network-check-ping6-command");
        executeOperationForSuccess(managementClient, op);
    }

    private static ManagementClient createManagementClient() throws UnknownHostException {
        return new ManagementClient(
                TestSuiteEnvironment.getModelControllerClient(),
                TestSuiteEnvironment.formatPossibleIpv6Address(TestSuiteEnvironment.getServerAddress()),
                TestSuiteEnvironment.getServerPort(),
                "remote+http");
    }

    private Path getLogFile(ManagementClient managementClient) throws IOException {
        final ModelNode address = Operations.createAddress("subsystem", "logging", "periodic-rotating-file-handler", "FILE");
        final ModelNode op = Operations.createOperation("resolve-path", address);
        final ModelNode result = managementClient.getControllerClient().execute(op);
        if (!Operations.isSuccessfulOutcome(result)) {
            Assert.fail("Failed to locate the log file: " + Operations.getFailureDescription(result).asString());
        }
        return Paths.get(Operations.readResult(result).asString());
    }

    private void executeOperationForSuccess(ManagementClient managementClient, ModelNode operation) throws IOException, MgmtOperationException {
        ModelNode response = managementClient.getControllerClient().execute(operation);
        Assert.assertTrue(Util.getFailureDescription(response), isSuccessfulOutcome(response));
    }

    private boolean isBrokerActive(JMSOperations jmsOperations, ManagementClient managementClient) throws IOException, MgmtOperationException {
        ModelNode operation = Operations.createReadAttributeOperation(jmsOperations.getServerAddress(), "active");
        ModelNode response = managementClient.getControllerClient().execute(operation);
        Assert.assertTrue(Util.getFailureDescription(response), isSuccessfulOutcome(response));
        return response.get("result").asBoolean();
    }

    class LoggerSetup extends TestLogHandlerSetupTask {

        @Override
        public Collection<String> getCategories() {
            return Arrays.asList("org.apache.activemq.artemis");
        }

        @Override
        public String getLevel() {
            return "TRACE";
        }

        @Override
        public String getHandlerName() {
            return "artemis-log";
        }

        @Override
        public String getLogFileName() {
            return "artemis.log";
        }
    }
}

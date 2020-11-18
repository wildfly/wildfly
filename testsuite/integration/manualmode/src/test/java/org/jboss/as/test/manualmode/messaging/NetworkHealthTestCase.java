/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.manualmode.messaging;

import java.io.IOException;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.cli.Util;
import org.jboss.as.controller.client.helpers.Operations;

import static org.jboss.as.controller.client.helpers.Operations.isSuccessfulOutcome;

import java.io.BufferedReader;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.common.jms.JMSOperations;
import org.jboss.as.test.integration.common.jms.JMSOperationsProvider;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 *
 * @author Emmanuel Hugonnet (c) 2020 Red Hat, Inc.
 */
@RunAsClient()
@RunWith(Arquillian.class)
public class NetworkHealthTestCase {

    private static final String DEFAULT_FULL_JBOSSAS = "default-full-jbossas";
    private static final String IP_ADDRESS = "192.0.2.0";

    @ArquillianResource
    protected static ContainerController container;

    /**
     * Defines a test IP to ping so that it fails and ensure that the logs contains the error message.
     * Re-defines the IP to be pinged to the current server address and checks that no such error message occurs.
     * @throws Exception.
     */
    @Test
    public void testNetworkUnHealthyNetwork() throws Exception {
        if (!container.isStarted(DEFAULT_FULL_JBOSSAS)) {
            container.start(DEFAULT_FULL_JBOSSAS);
        }
        ManagementClient managementClient = createManagementClient();
        JMSOperations jmsOperations = JMSOperationsProvider.getInstance(managementClient.getControllerClient());
        ModelNode op = Operations.createWriteAttributeOperation(jmsOperations.getServerAddress(), "network-check-list", IP_ADDRESS);
        executeOperationForSuccess(managementClient, op);
        op = Operations.createWriteAttributeOperation(jmsOperations.getServerAddress(), "network-check-period", 1000);
        executeOperationForSuccess(managementClient, op);
        op = Operations.createWriteAttributeOperation(jmsOperations.getServerAddress(), "network-check-timeout", 200);
        executeOperationForSuccess(managementClient, op);
        Path logFile = getLogFile(managementClient);
        managementClient.close();
        container.stop(DEFAULT_FULL_JBOSSAS);
        container.start(DEFAULT_FULL_JBOSSAS);
        managementClient = createManagementClient();
        Thread.sleep(TimeoutUtil.adjust(2000));
        boolean found = false;
        try (BufferedReader reader = Files.newBufferedReader(logFile, StandardCharsets.UTF_8)) {
            String line = null;
            while (!found && (line = reader.readLine()) != null) {
                found = line.contains("AMQ202002") && line.contains(IP_ADDRESS);
            }
            Assert.assertTrue(String.format("Log contains ActiveMQ ping error log message: %n%s", line), found);
        }
        Assert.assertFalse("Beroker should be stopped", isBrokerActive(jmsOperations, managementClient));
        op = Operations.createWriteAttributeOperation(jmsOperations.getServerAddress(), "network-check-list", TestSuiteEnvironment.getServerAddress());
        executeOperationForSuccess(managementClient, op);
        managementClient.close();
        container.stop(DEFAULT_FULL_JBOSSAS);
        int failure = Files.readAllLines(logFile).size();
        container.start(DEFAULT_FULL_JBOSSAS);
        managementClient = createManagementClient();
        Thread.sleep(TimeoutUtil.adjust(2000));
        try (BufferedReader reader = Files.newBufferedReader(logFile, StandardCharsets.UTF_8)) {
            String line;
            int lineNumber = 0 ;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if(lineNumber > failure) {
                    Assert.assertFalse(String.format("Log contains ActiveMQ ping error log message: %n%s", line), line.contains("AMQ202002") || line.contains("AMQ201001"));
                }
            }
        }
        Assert.assertTrue("Broker should be running", isBrokerActive(jmsOperations, managementClient));
        op = Operations.createUndefineAttributeOperation(jmsOperations.getServerAddress(), "network-check-list");
        executeOperationForSuccess(managementClient, op);
        op = Operations.createUndefineAttributeOperation(jmsOperations.getServerAddress(), "network-check-period");
        executeOperationForSuccess(managementClient, op);
        op = Operations.createUndefineAttributeOperation(jmsOperations.getServerAddress(), "network-check-timeout");
        executeOperationForSuccess(managementClient, op);
        container.stop(DEFAULT_FULL_JBOSSAS);
    }

    /**
     * Defines an unexisting ping command to check that this creates a proper error message.
     * @throws Exception.
     */
    @Test
    public void testPingCommandError() throws Exception {
        if (!container.isStarted(DEFAULT_FULL_JBOSSAS)) {
            container.start(DEFAULT_FULL_JBOSSAS);
        }
        ManagementClient managementClient = createManagementClient();
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
        Path logFile = getLogFile(managementClient);
        managementClient.close();
        container.stop(DEFAULT_FULL_JBOSSAS);
        container.start(DEFAULT_FULL_JBOSSAS);
        managementClient = createManagementClient();
        Thread.sleep(TimeoutUtil.adjust(2000));
        boolean found = false;
        try (BufferedReader reader = Files.newBufferedReader(logFile, StandardCharsets.UTF_8)) {
            String line = null;
            while (!found && (line = reader.readLine()) != null) {
                found = (line.contains(" AMQ202007") && line.contains(IP_ADDRESS) && line.contains("java.io.IOException")) || line.contains("AMQ201001");
            }
            Assert.assertTrue(String.format("Log contains ActiveMQ ping error log message: %n%s", line), found);
        }
        Assert.assertFalse("Broker should be stopped", isBrokerActive(jmsOperations, managementClient));
        op = Operations.createUndefineAttributeOperation(jmsOperations.getServerAddress(), "network-check-list");
        executeOperationForSuccess(managementClient, op);
        op = Operations.createUndefineAttributeOperation(jmsOperations.getServerAddress(), "network-check-period");
        executeOperationForSuccess(managementClient, op);
        op = Operations.createUndefineAttributeOperation(jmsOperations.getServerAddress(), "network-check-timeout");
        executeOperationForSuccess(managementClient, op);
        op = Operations.createUndefineAttributeOperation(jmsOperations.getServerAddress(), "network-check-ping-command");
        executeOperationForSuccess(managementClient, op);
        op = Operations.createUndefineAttributeOperation(jmsOperations.getServerAddress(), "network-check-ping6-command");
        executeOperationForSuccess(managementClient, op);
        managementClient.close();
        container.stop(DEFAULT_FULL_JBOSSAS);
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
}

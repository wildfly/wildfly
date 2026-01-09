/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.manualmode.messaging;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.shrinkwrap.api.ShrinkWrap.create;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collection;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.test.integration.common.jms.JMSOperations;
import org.jboss.as.test.integration.common.jms.JMSOperationsProvider;
import org.jboss.as.test.manualmode.messaging.deployment.HelloWorldMDBServletClient;
import org.jboss.as.test.manualmode.messaging.deployment.HelloWorldQueueMDB;
import org.jboss.as.test.manualmode.messaging.deployment.HelloWorldTopicMDB;

import org.jboss.as.test.shared.TestLogHandlerSetupTask;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.as.test.shared.logging.LoggingUtil;

import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test creating queues on an external broker.
 * Covers WFLY-19418
 *
 * @author Emmanuel Hugonnet (c) 2019 Red Hat, Inc.
 */
@RunAsClient()
@RunWith(Arquillian.class)
public class AutocreationManagementTestCase {

    private static final String DEFAULT_FULL_JBOSSAS = "default-full-jbossas";

    @ArquillianResource
    protected static ContainerController container;
    @ArquillianResource
    private Deployer deployer;
    private LoggerSetup loggerSetup;

    private static final ModelNode SERVER_ADDRESS = PathAddress.parseCLIStyleAddress("/subsystem=messaging-activemq/server=default").toModelNode();

    @Before
    public void setup() throws Exception {
        loggerSetup = new LoggerSetup();
        if (container.isStarted(DEFAULT_FULL_JBOSSAS)) {
            container.stop(DEFAULT_FULL_JBOSSAS);
        }
        if (!container.isStarted(DEFAULT_FULL_JBOSSAS)) {
            container.start(DEFAULT_FULL_JBOSSAS);
        }
        try (ManagementClient managementClient = createManagementClient()) {
            loggerSetup.setup(managementClient, DEFAULT_FULL_JBOSSAS);
            JMSOperations jmsOperations = JMSOperationsProvider.getInstance(managementClient.getControllerClient());
            jmsOperations.createSocketBinding("broker-one", null, 61616);
            jmsOperations.createSocketBinding("broker-two", null, 61716);
            jmsOperations.createRemoteAcceptor("remote-artemis", "broker-one", null);
            jmsOperations.enableSecurity();
            ModelNode address = new ModelNode();
            address.add("subsystem", "messaging-activemq");
            address.add("server", "default");
            address.add("pooled-connection-factory", "activemq-ra");
            ModelNode op = Operations.createRemoveOperation(address);
            execute(managementClient, op, true);
            jmsOperations.addExternalRemoteConnector("remote-artemis-one", "broker-one");
            jmsOperations.addExternalRemoteConnector("remote-artemis-two", "broker-two");
            op = Operations.createAddOperation(jmsOperations.getServerAddress().add("security-setting", "#").add("role", "Role1"));
            op.get("send").set(ModelNode.TRUE);
            op.get("consume").set(ModelNode.TRUE);
            op.get("create-non-durable-queue").set(ModelNode.TRUE);
            op.get("delete-non-durable-queue").set(ModelNode.TRUE);
            op.get("manage").set(ModelNode.TRUE);
            execute(managementClient, op, true);
            op = Operations.createAddOperation(jmsOperations.getSubsystemAddress().add("pooled-connection-factory", "activemq-ra"));
            op.get("transaction").set("xa");
            op.get("user").set("user1");
            op.get("password").set("password1");
            op.get("enable-amq1-prefix").set("false");
            op.get("connectors").add("remote-artemis-one");
            op.get("rebalance-connections").set(true);
            op.get("entries").add("java:jboss/DefaultJMSConnectionFactory").add("java:/RemoteJmsXA");
            execute(managementClient, op, true);
            jmsOperations.close();
            if (container.isStarted(DEFAULT_FULL_JBOSSAS)) {
                container.stop(DEFAULT_FULL_JBOSSAS);
            }
        }
    }

    @After
    public void teardown() throws Exception {
        try (ManagementClient managementClient = createManagementClient()) {
            loggerSetup.tearDown(managementClient, DEFAULT_FULL_JBOSSAS);
            JMSOperations jmsOperations = JMSOperationsProvider.getInstance(managementClient.getControllerClient());
            ModelNode op = Operations.createRemoveOperation(jmsOperations.getSubsystemAddress().add("pooled-connection-factory", "activemq-ra"));
            execute(managementClient, op, true);
            jmsOperations.removeExternalRemoteConnector("remote-artemis-one");
            jmsOperations.removeExternalRemoteConnector("remote-artemis-two");
            jmsOperations.removeRemoteAcceptor("remote-artemis");
            op = Operations.createRemoveOperation(PathAddress.parseCLIStyleAddress("/socket-binding-group=standard-sockets/socket-binding=broker-one").toModelNode());
            execute(managementClient, op, true);
            op = Operations.createRemoveOperation(PathAddress.parseCLIStyleAddress("/socket-binding-group=standard-sockets/socket-binding=broker-two").toModelNode());
            execute(managementClient, op, true);
            ModelNode address = new ModelNode();
            address.add("subsystem", "messaging-activemq");
            address.add("server", "default");
            address.add("pooled-connection-factory", "activemq-ra");
            op = Operations.createAddOperation(address);
            op.get("entries").add("java:/JmsXA");
            op.get("entries").add("java:jboss/DefaultJMSConnectionFactory");
            op.get("connectors").add("in-vm");
            op.get("transaction").add("xa");
            execute(managementClient, op, true);
            if (container.isStarted(DEFAULT_FULL_JBOSSAS)) {
                container.stop(DEFAULT_FULL_JBOSSAS);
            }
            if (!container.isStarted(DEFAULT_FULL_JBOSSAS)) {
                container.start(DEFAULT_FULL_JBOSSAS);
            }
        }
    }

    private ModelNode execute(final org.jboss.as.arquillian.container.ManagementClient managementClient, final ModelNode op, final boolean expectSuccess) throws IOException {
        ModelNode response = managementClient.getControllerClient().execute(op);
        final boolean success = Operations.isSuccessfulOutcome(response);
        if (expectSuccess) {
            assertTrue(response.toString(), success);
            return Operations.readResult(response);
        } else {
            assertFalse(response.toString(), success);
            return Operations.getFailureDescription(response);
        }
    }

    @Test
    public void testExtenalBrokerQueueCreation() throws Exception {
        if (!container.isStarted(DEFAULT_FULL_JBOSSAS)) {
            container.start(DEFAULT_FULL_JBOSSAS);
        }
        deployer.deploy(DEPLOYMENT);
        deployer.undeploy(DEPLOYMENT);
        try (ManagementClient managementClient = createManagementClient()) {
            assertFalse(LoggingUtil.hasLogMessage(managementClient.getControllerClient(), "artemis-log", "",
                    (line) -> (line.contains("AMQ229031: Unable to validate user from"))));
            assertTrue(LoggingUtil.hasLogMessage(managementClient.getControllerClient(), "artemis-log", "",
                    (line) -> (line.contains("Creating topic jms.topic.HelloWorldMDBTopic on node UP"))));
            assertTrue(LoggingUtil.hasLogMessage(managementClient.getControllerClient(), "artemis-log", "",
                    (line) -> (line.contains("Creating queue jms.queue.HelloWorldMDBQueue on node UP"))));
         }
    }

    private static ManagementClient createManagementClient() throws UnknownHostException {
        return new ManagementClient(
                TestSuiteEnvironment.getModelControllerClient(),
                TestSuiteEnvironment.formatPossibleIpv6Address(TestSuiteEnvironment.getServerAddress()),
                TestSuiteEnvironment.getServerPort(),
                "remote+http");
    }

    @Deployment(name = DEPLOYMENT, managed = false, testable = false)
    @TargetsContainer(DEFAULT_FULL_JBOSSAS)
    public static WebArchive createArchive() {
        return create(WebArchive.class, DEPLOYMENT + ".war")
                .addClasses(HelloWorldQueueMDB.class, HelloWorldTopicMDB.class, HelloWorldMDBServletClient.class)
                .addAsWebInfResource(new StringAsset("<beans xmlns=\"https://jakarta.ee/xml/ns/jakartaee\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                        + "       xsi:schemaLocation=\"\n"
                        + "         https://jakarta.ee/xml/ns/jakartaee\n"
                        + "         https://jakarta.ee/xml/ns/jakartaee/beans_4_0.xsd\"\n"
                        + "       bean-discovery-mode=\"all\">\n"
                        + "</beans>"), "beans.xml");
    }

    class LoggerSetup extends TestLogHandlerSetupTask {

        @Override
        public Collection<String> getCategories() {
            return Arrays.asList("org.apache.activemq.artemis.client", "org.apache.activemq.artemis.utils", "org.wildfly.extension.messaging-activemq");
        }

        @Override
        public String getLevel() {
            return "INFO";
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

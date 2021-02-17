/*
 * Copyright 2021 JBoss by Red Hat.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.test.manualmode.messaging;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Properties;
import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.JMSProducer;
import javax.jms.Queue;
import javax.jms.TextMessage;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.apache.commons.lang3.RandomStringUtils;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.test.integration.common.jms.JMSOperations;
import org.jboss.as.test.integration.common.jms.JMSOperationsProvider;
import org.jboss.as.test.shared.TestLogHandlerSetupTask;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.as.test.shared.util.LoggingUtil;
import org.jboss.byteman.agent.submit.Submit;
import org.jboss.dmr.ModelNode;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 *
 * @author Emmanuel Hugonnet (c) 2020 Red Hat, Inc.
 */
@RunWith(Arquillian.class)
@RunAsClient
public class CriticalAnalyzerTestCase {

    private static final String DEFAULT_FULL_JBOSSAS = "default-full-jbossas-byteman";
    private static final String EXPORTED_PREFIX = "java:jboss/exported/";

    @ArquillianResource
    protected static ContainerController container;

    private LoggerSetup loggerSetup;
    private ManagementClient managementClient;

    private final Submit bytemanSubmit = new Submit(
            System.getProperty("byteman.server.ipaddress", Submit.DEFAULT_ADDRESS),
            Integer.getInteger("byteman.server.port", Submit.DEFAULT_PORT));

    private void deployRules() throws Exception {
        bytemanSubmit.addRulesFromResources(Collections.singletonList(
                CriticalAnalyzerTestCase.class.getClassLoader().getResourceAsStream("byteman/CriticalAnalyzerTestCase.btm")));
    }

    private void removeRules() {
        try {
            bytemanSubmit.deleteAllRules();
        } catch (Exception ex) {
        }
    }

    @Before
    public void setup() throws Exception {
        if (!container.isStarted(DEFAULT_FULL_JBOSSAS)) {
            container.start(DEFAULT_FULL_JBOSSAS);
        }
        loggerSetup = new LoggerSetup();
        managementClient = createManagementClient();
        loggerSetup.setup(managementClient, DEFAULT_FULL_JBOSSAS);
        JMSOperations jmsOperations = JMSOperationsProvider.getInstance(managementClient.getControllerClient());
        managementClient.getControllerClient().execute(Operations.createWriteAttributeOperation(jmsOperations.getServerAddress(), "critical-analyzer-policy", "SHUTDOWN"));
        managementClient.getControllerClient().execute(Operations.createWriteAttributeOperation(jmsOperations.getServerAddress(), "critical-analyzer-enabled", ModelNode.TRUE));
        managementClient.getControllerClient().execute(Operations.createWriteAttributeOperation(jmsOperations.getServerAddress(), "critical-analyzer-check-period", new ModelNode(100L)));
        managementClient.getControllerClient().execute(Operations.createWriteAttributeOperation(jmsOperations.getServerAddress(), "critical-analyzer-timeout", new ModelNode(1000L)));
        try {
            jmsOperations.removeJmsQueue("critical");
        } catch(RuntimeException ex) {
        }
        jmsOperations.createJmsQueue("critical", EXPORTED_PREFIX + "queue/critical");
        jmsOperations.close();
        managementClient.close();
        container.stop(DEFAULT_FULL_JBOSSAS);
        container.start(DEFAULT_FULL_JBOSSAS);
        managementClient = createManagementClient();
    }

    @After
    public void cleanAll() throws Exception {
        if (!container.isStarted(DEFAULT_FULL_JBOSSAS)) {
            container.start(DEFAULT_FULL_JBOSSAS);
        }
        removeRules();
        loggerSetup.tearDown(managementClient, DEFAULT_FULL_JBOSSAS);
        JMSOperations jmsOperations = JMSOperationsProvider.getInstance(managementClient.getControllerClient());
        managementClient.getControllerClient().execute(Operations.createUndefineAttributeOperation(jmsOperations.getServerAddress(), "critical-analyzer-policy"));
        managementClient.getControllerClient().execute(Operations.createUndefineAttributeOperation(jmsOperations.getServerAddress(), "critical-analyzer-enabled"));
        managementClient.getControllerClient().execute(Operations.createUndefineAttributeOperation(jmsOperations.getServerAddress(), "critical-analyzer-check-period"));
        managementClient.getControllerClient().execute(Operations.createUndefineAttributeOperation(jmsOperations.getServerAddress(), "critical-analyzer-timeout"));
        if (isBrokerRunning()) {
            jmsOperations.removeJmsQueue("critical");
        }
        jmsOperations.close();
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

    /**
     * Set the critical analyzer to SHUTDOWN strategy.
     * Use the byteman script to simulate a slow journal that would make the critical analyzer to activate.
     * Check that the critical analyzer was started and created the expected log traces.
     * Check that the broker has been stopped.
     * @throws Exception
     */
    @Test
    public void testCriticalAnalyzer() throws Exception {
        if (!container.isStarted(DEFAULT_FULL_JBOSSAS)) {
            container.start(DEFAULT_FULL_JBOSSAS);
        }
        InitialContext remoteContext = createJNDIContext();
        managementClient = createManagementClient();
        ConnectionFactory cf = (ConnectionFactory) remoteContext.lookup("jms/RemoteConnectionFactory");
        Queue queue = (Queue) remoteContext.lookup("queue/critical");
        deployRules();
        try (JMSContext context = cf.createContext("guest", "guest", JMSContext.AUTO_ACKNOWLEDGE)) {
            JMSProducer producer = context.createProducer();
            for (int i = 0; i < 20; i++) {
                TextMessage message = context.createTextMessage(RandomStringUtils.randomAlphabetic(10));
                producer.send(queue, message);
            }
            Assert.fail("Critical analyzer should have kicked in");
        } catch (javax.jms.JMSRuntimeException ex) {
            Assert.assertTrue("Log should contains ActiveMQ connection failure error log message: [AMQ219016]", ex.getMessage().contains("AMQ219016"));
            Assert.assertTrue("Log should contains ActiveMQ critical measure ", LoggingUtil.hasLogMessage(managementClient, "artemis-log", "",
                    (line) -> (line.contains("[org.apache.activemq.artemis.utils.critical.CriticalMeasure]"))));
            Assert.assertTrue("Log should contains ActiveMQ AMQ224080 : critical analyzer is stopping the broker", LoggingUtil.hasLogMessage(managementClient, "artemis-log", "",
                    (line) -> (line.contains("AMQ224080"))));
            Assert.assertTrue("Log should contains ActiveMQ AMQ222199 : Thread dump ", LoggingUtil.hasLogMessage(managementClient, "artemis-log", "",
                    (line) -> (line.contains("AMQ222199"))));
        }
        remoteContext.close();
        assertFalse(isBrokerRunning());
    }

    /**
     * Disable the critical analyzer.
     * Use the byteman script to simulate a slow journal that would make the critical analyzer to activate.
     * Check that there is nothing in the logs and that the broker is still running.
     * @throws Exception
     */
    @Test
    public void testCriticalAnalyzerDisabled() throws Exception {
        if (!container.isStarted(DEFAULT_FULL_JBOSSAS)) {
            container.start(DEFAULT_FULL_JBOSSAS);
        }
        JMSOperations jmsOperations = JMSOperationsProvider.getInstance(managementClient.getControllerClient());
        managementClient.getControllerClient().execute(Operations.createWriteAttributeOperation(jmsOperations.getServerAddress(), "critical-analyzer-enabled", ModelNode.FALSE));
        jmsOperations.close();
        managementClient.close();
        container.stop(DEFAULT_FULL_JBOSSAS);
        container.start(DEFAULT_FULL_JBOSSAS);
        InitialContext remoteContext = createJNDIContext();
        managementClient = createManagementClient();
        ConnectionFactory cf = (ConnectionFactory) remoteContext.lookup("jms/RemoteConnectionFactory");
        Queue queue = (Queue) remoteContext.lookup("queue/critical");
        deployRules();
        try (JMSContext context = cf.createContext("guest", "guest", JMSContext.AUTO_ACKNOWLEDGE)) {
            JMSProducer producer = context.createProducer();
            for (int i = 0; i < 20; i++) {
                TextMessage message = context.createTextMessage(RandomStringUtils.randomAlphabetic(10));
                producer.send(queue, message);
            }
        }
        remoteContext.close();
        assertTrue(isBrokerRunning());
    }

    private boolean isBrokerRunning() throws IOException {
        JMSOperations jmsOperations = JMSOperationsProvider.getInstance(managementClient.getControllerClient());
        ModelNode response =  managementClient.getControllerClient().execute(Operations.createReadAttributeOperation(jmsOperations.getServerAddress(), "started"));
        jmsOperations.close();
        assertTrue(response.toJSONString(true), Operations.isSuccessfulOutcome(response));
        return Operations.readResult(response).asBoolean();
    }

    protected static InitialContext createJNDIContext() throws NamingException {
        final Properties env = new Properties();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "org.jboss.naming.remote.client.InitialContextFactory");
        String ipAdddress = TestSuiteEnvironment.formatPossibleIpv6Address(TestSuiteEnvironment.getServerAddress());
        env.put(Context.PROVIDER_URL, System.getProperty(Context.PROVIDER_URL, "remote+http://" + ipAdddress + ":8080"));
        env.put(Context.SECURITY_PRINCIPAL, "guest");
        env.put(Context.SECURITY_CREDENTIALS, "guest");
        return new InitialContext(env);
    }

    class LoggerSetup extends TestLogHandlerSetupTask {

        @Override
        public Collection<String> getCategories() {
            return Arrays.asList("org.apache.activemq.artemis.core.server", "org.apache.activemq.artemis.utils");
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
